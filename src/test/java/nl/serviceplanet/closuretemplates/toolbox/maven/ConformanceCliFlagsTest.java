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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies the {@code --conformanceConfig} flag the {@code soy-conformance-checker} goal hands to the Soy conformance
 * checker. The contract is: Every config file is passed as an absolute path (resolved against the workdir), and
 * multiple config files are combined into a single comma-separated flag value, which is what the checker expects.
 */
public final class ConformanceCliFlagsTest {

	private static final Path CWD = Path.of("").toAbsolutePath();

	private static final String CONFORMANCE_CONFIG_FLAG = "--conformanceConfig";

	private static ClosureTemplatesSoyConformanceCheckerMojo mojoFor(String workdir, String conformanceConfigs,
			String conformanceConfigsBasePath) throws Exception {
		ClosureTemplatesSoyConformanceCheckerMojo mojo = new ClosureTemplatesSoyConformanceCheckerMojo();
		mojo.workdir = workdir;
		setField(mojo, "conformanceConfigs", conformanceConfigs);
		setField(mojo, "conformanceConfigsBasePath", conformanceConfigsBasePath);
		return mojo;
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = ClosureTemplatesSoyConformanceCheckerMojo.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static String flagValue(List<String> flags, String flag) {
		int flagIndex = flags.indexOf(flag);
		Assertions.assertTrue(flagIndex >= 0, () -> "no " + flag + " flag in " + flags);
		return flags.get(flagIndex + 1);
	}

	@Test
	public void testMultipleConfigsAreResolvedAgainstTheWorkdirAndCombinedIntoOneFlag() throws Exception {
		List<String> flags = mojoFor(null, "conformance/security.textproto,conformance/style.textproto", null)
				.generateCliFlags();

		Assertions.assertEquals(
				CWD.resolve("conformance/security.textproto") + "," + CWD.resolve("conformance/style.textproto"),
				flagValue(flags, CONFORMANCE_CONFIG_FLAG));
		// The flag must be given exactly once, with all files in it.
		Assertions.assertEquals(1, flags.stream().filter(CONFORMANCE_CONFIG_FLAG::equals).count());
	}

	@Test
	public void testConfigsAreResolvedAgainstAnExplicitWorkdir(@TempDir Path workdir) throws Exception {
		List<String> flags = mojoFor(workdir.toString(), "conformance/security.textproto", null).generateCliFlags();

		Assertions.assertEquals(
				workdir.toRealPath().resolve("conformance/security.textproto").toString(),
				flagValue(flags, CONFORMANCE_CONFIG_FLAG));
	}

	/**
	 * Config files are not Soy sources: the compiler does not know them by a logical path, so they must not get a
	 * {@code --generated_files} entry.
	 */
	@Test
	public void testConfigsGetNoLogicalPathMapping() throws Exception {
		List<String> flags = mojoFor(null, "conformance/security.textproto", null).generateCliFlags();

		Assertions.assertFalse(flags.contains("--generated_files"), () -> "unexpected --generated_files in " + flags);
	}

	@Test
	public void testBasePathIsScannedRecursivelyForTextprotoFiles(@TempDir Path workdir) throws Exception {
		Path conformanceDir = Files.createDirectories(workdir.resolve("conformance"));
		Files.writeString(conformanceDir.resolve("style.textproto"), "");
		Files.writeString(conformanceDir.resolve("security.textproto"), "");
		Files.writeString(conformanceDir.resolve("README.md"), "not a config");
		Path nested = Files.createDirectories(conformanceDir.resolve("team"));
		Files.writeString(nested.resolve("extra.textproto"), "");

		List<String> flags = mojoFor(workdir.toString(), null, "conformance").generateCliFlags();

		Path realWorkdir = workdir.toRealPath();
		Assertions.assertEquals(
				realWorkdir.resolve("conformance/security.textproto") + ","
						+ realWorkdir.resolve("conformance/style.textproto") + ","
						+ realWorkdir.resolve("conformance/team/extra.textproto"),
				flagValue(flags, CONFORMANCE_CONFIG_FLAG));
	}

	@Test
	public void testBasePathAndExplicitConfigsAreCombined(@TempDir Path workdir) throws Exception {
		Path conformanceDir = Files.createDirectories(workdir.resolve("conformance"));
		Files.writeString(conformanceDir.resolve("security.textproto"), "");

		List<String> flags = mojoFor(workdir.toString(), "shared/style.textproto", "conformance").generateCliFlags();

		Path realWorkdir = workdir.toRealPath();
		Assertions.assertEquals(
				realWorkdir.resolve("conformance/security.textproto") + "," + realWorkdir.resolve("shared/style.textproto"),
				flagValue(flags, CONFORMANCE_CONFIG_FLAG));
	}

	@Test
	public void testAbsoluteConfigIsPassedThrough() throws Exception {
		String absolute = CWD.resolve("somewhere/security.textproto").toString();

		List<String> flags = mojoFor(null, absolute, null).generateCliFlags();

		Assertions.assertEquals(absolute, flagValue(flags, CONFORMANCE_CONFIG_FLAG));
	}

	/**
	 * Without a config there is nothing to check. The goal must say so itself instead of letting the checker fail
	 * with a stack trace.
	 */
	@Test
	public void testNoConfigsIsRejected() throws Exception {
		ClosureTemplatesSoyConformanceCheckerMojo mojo = mojoFor(null, " ", null);

		MojoExecutionException e = Assertions.assertThrows(MojoExecutionException.class, mojo::execute);
		Assertions.assertTrue(e.getMessage().contains("conformanceConfigs"), e.getMessage());
	}
}
