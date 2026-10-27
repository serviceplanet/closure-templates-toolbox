/*
 * Copyright © 2024 Service Planet Rotterdam B.V. (it@ask.serviceplanet.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.serviceplanet.closuretemplates.toolbox.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the real {@code soy-conformance-checker} goal against the real Soy conformance checker, with the rules split
 * over two config files ({@code conformance/security.textproto} and {@code conformance/style.textproto}) to verify
 * that every config file given is enforced.
 */
public final class ConformanceEndToEndTest {

	private static final String SECURITY_MESSAGE = "SECURITY: Inline event handlers are not allowed.";

	private static final String STYLE_MESSAGE = "STYLE: Obsolete elements are not allowed.";

	/** Workdir-relative directory whose templates are exempt from the style rules. */
	private static final String LEGACY_DIR = "nl/serviceplanet/demo/legacy/";

	/**
	 * Directory name of the workdir. It is part of the absolute path of every template but not of the
	 * workdir-relative logical path, which lets the exemption test tell the two apart.
	 */
	private static final String WORKDIR_NAME = "absolute-path-only";

	/** Outcome of running the goal: the exception it threw (if any) and what the checker printed on stderr. */
	private record Outcome(Exception failure, String stderr) {
	}

	private static Path createWorkdir(Path tempDir) throws Exception {
		Path workdir = Files.createDirectory(tempDir.resolve(WORKDIR_NAME));
		Path conformanceDir = Files.createDirectories(workdir.resolve("conformance"));

		Files.writeString(conformanceDir.resolve("security.textproto"), """
				requirement {
				  custom {
				    java_class: "com.google.template.soy.conformance.BanInlineEventHandlers"
				  }
				  error_message: "%s"
				}
				""".formatted(SECURITY_MESSAGE));

		// Exempts the legacy directory by its workdir-relative path, and the workdir by its directory name. The
		// latter is a substring of every template's absolute path but of no template's logical path.
		Files.writeString(conformanceDir.resolve("style.textproto"), """
				requirement {
				  banned_html_tag {
				    tag: "marquee"
				  }
				  error_message: "%s"
				  exempt: "%s"
				  exempt: "%s"
				}
				""".formatted(STYLE_MESSAGE, LEGACY_DIR, WORKDIR_NAME));

		return workdir;
	}

	private static void writeTemplate(Path workdir, String relativePath, String namespace, String body)
			throws Exception {
		Path file = workdir.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, """
				{namespace %s}

				{template main}
				  %s
				{/template}
				""".formatted(namespace, body));
	}

	private static ClosureTemplatesSoyConformanceCheckerMojo createMojo(Path workdir, String conformanceConfigs,
			String conformanceConfigsBasePath) throws Exception {
		ClosureTemplatesSoyConformanceCheckerMojo mojo = new ClosureTemplatesSoyConformanceCheckerMojo();

		setField(mojo, AbstractClosureTemplatesCompilerMojo.class, "workdir", workdir.toString());
		setField(mojo, AbstractClosureTemplatesCompilerMojo.class, "soySourcesBasePath", ".");
		setField(mojo, ClosureTemplatesSoyConformanceCheckerMojo.class, "conformanceConfigs", conformanceConfigs);
		setField(mojo, ClosureTemplatesSoyConformanceCheckerMojo.class, "conformanceConfigsBasePath",
				conformanceConfigsBasePath);

		return mojo;
	}

	private static void setField(Object target, Class<?> declaringClass, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * Runs the goal while capturing stderr, which is where the goal lets the checker report violations.
	 */
	private static Outcome execute(ClosureTemplatesSoyConformanceCheckerMojo mojo) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		PrintStream originalErr = System.err;
		System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));

		Exception failure = null;
		try {
			mojo.execute();
		} catch (MojoExecutionException | MojoFailureException e) {
			failure = e;
		} finally {
			System.setErr(originalErr);
		}

		return new Outcome(failure, buffer.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testConformingTemplatePasses(@TempDir Path tempDir) throws Exception {
		Path workdir = createWorkdir(tempDir);
		writeTemplate(workdir, "nl/serviceplanet/demo/clean.soy", "nl.serviceplanet.demo.clean",
				"<button>Buy</button>");

		Outcome outcome = execute(createMojo(workdir,
				"conformance/security.textproto,conformance/style.textproto", null));

		Assertions.assertNull(outcome.failure(), () -> "goal failed: " + outcome.failure() + "\n" + outcome.stderr());
	}

	/**
	 * A template violating a rule from each config file: both violations must be reported, proving that every config
	 * file in the comma-separated list is enforced.
	 */
	@Test
	public void testViolationsOfEveryConfigFileAreReported(@TempDir Path tempDir) throws Exception {
		Path workdir = createWorkdir(tempDir);
		writeTemplate(workdir, "nl/serviceplanet/demo/violating.soy", "nl.serviceplanet.demo.violating",
				"<marquee><button onclick=\"buy()\">Buy</button></marquee>");

		Outcome outcome = execute(createMojo(workdir,
				"conformance/security.textproto,conformance/style.textproto", null));

		Assertions.assertInstanceOf(MojoFailureException.class, outcome.failure(), outcome.stderr());
		Assertions.assertTrue(outcome.stderr().contains(SECURITY_MESSAGE), outcome.stderr());
		Assertions.assertTrue(outcome.stderr().contains(STYLE_MESSAGE), outcome.stderr());
	}

	@Test
	public void testConfigFilesFoundUnderBasePathAreEnforced(@TempDir Path tempDir) throws Exception {
		Path workdir = createWorkdir(tempDir);
		writeTemplate(workdir, "nl/serviceplanet/demo/violating.soy", "nl.serviceplanet.demo.violating",
				"<marquee><button onclick=\"buy()\">Buy</button></marquee>");

		Outcome outcome = execute(createMojo(workdir, null, "conformance"));

		Assertions.assertInstanceOf(MojoFailureException.class, outcome.failure(), outcome.stderr());
		Assertions.assertTrue(outcome.stderr().contains(SECURITY_MESSAGE), outcome.stderr());
		Assertions.assertTrue(outcome.stderr().contains(STYLE_MESSAGE), outcome.stderr());
	}

	/**
	 * Exemptions are matched against the path the checker knows a template by. With this plugin that is the
	 * workdir-relative logical path: an exemption spelled relative to the workdir applies, while one matching only
	 * the absolute path of the workdir does not.
	 */
	@Test
	public void testExemptionsMatchTheWorkdirRelativePath(@TempDir Path tempDir) throws Exception {
		Path workdir = createWorkdir(tempDir);
		writeTemplate(workdir, LEGACY_DIR + "old.soy", "nl.serviceplanet.demo.legacy.old",
				"<marquee>exempt</marquee>");
		writeTemplate(workdir, "nl/serviceplanet/demo/current.soy", "nl.serviceplanet.demo.current",
				"<marquee>not exempt</marquee>");

		Outcome outcome = execute(createMojo(workdir, "conformance/style.textproto", null));

		Assertions.assertInstanceOf(MojoFailureException.class, outcome.failure(), outcome.stderr());
		Assertions.assertTrue(outcome.stderr().contains("current.soy"), outcome.stderr());
		Assertions.assertFalse(outcome.stderr().contains("old.soy"), outcome.stderr());
	}
}
