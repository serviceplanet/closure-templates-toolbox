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

import com.google.template.soy.SoyConformanceChecker;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Provides a Maven goal which checks Soy (Closure Templates) templates against conformance rules by invoking the
 * {@code SoyConformanceChecker}. Conformance rules are project-specific requirements on template source (such as
 * "never call this function" or "never use inline event handlers"), configured as {@code soy.ConformanceConfig}
 * messages in protocol buffer text format. Each violation is reported as a compile error with the configured message
 * and fails the build.
 * <br /><br />
 * The conformance checker is the only Soy compiler which accepts conformance configs (the code generating compilers
 * do not), so checking conformance is a goal of its own which runs next to the code generating goals.
 *
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
@Mojo(name = "soy-conformance-checker", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public final class ClosureTemplatesSoyConformanceCheckerMojo extends AbstractClosureTemplatesCompilerMojo {

	/** Soy conformance checker CLI flag to specify the conformance config files. */
	private static final String CONFORMANCE_CONFIG_FLAG = "--conformanceConfig";

	/**
	 * Comma separated list of file paths of conformance config files. A conformance config file is a
	 * {@code soy.ConformanceConfig} message in protocol buffer text format. The requirements of all config files are
	 * concatenated, so rules can be split over several files. For example {@code conformance/security.textproto} and
	 * {@code conformance/style.textproto}.
	 * <br /><br />
	 * A relative path is resolved against {@link #workdir}.
	 * <br /><br />
	 * Contents are passed to the {@code SoyConformanceChecker} as arguments of the {@code conformanceConfig} flag.
	 */
	@Parameter(property = "conformanceConfigs")
	private String conformanceConfigs;

	/**
	 * Finds all conformance config files ({@code *.textproto}) under the specified path and passes them to the
	 * {@code SoyConformanceChecker}. Can be combined with {@link #conformanceConfigs}.
	 * <br /><br />
	 * A relative path is resolved against {@link #workdir}.
	 * <br /><br />
	 * Contents are passed to the {@code SoyConformanceChecker} as arguments of the {@code conformanceConfig} flag.
	 */
	@Parameter(property = "conformanceConfigsBasePath")
	private String conformanceConfigsBasePath;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<String> checkerCliArgs = generateCliFlags();
		getLog().debug(String.format("About to run Soy conformance checker with the following CLI arguments: '%s'.",
				checkerCliArgs));

		SoyConformanceChecker checker = createSoyConformanceCheckerInstance();
		int exitCode = checker.run(checkerCliArgs.toArray(new String[0]), System.err);

		if (exitCode != 0) {
			throw new MojoFailureException("SoyConformanceChecker returned exit-code: " + exitCode);
		}
	}

	/**
	 * Generates the "command line" flags and arguments we are going to pass to the Soy conformance checker.
	 */
	List<String> generateCliFlags() throws MojoExecutionException {
		List<String> args = generateBaseCliFlags();

		String allConformanceConfigs = combine(
				discoverFilesAgainstWorkdir(conformanceConfigsBasePath, SoyFileDiscovery::findConformanceConfigFiles),
				resolvePathsAgainstWorkdir(conformanceConfigs));
		if (allConformanceConfigs.isEmpty()) {
			// The checker itself also refuses to run without a config, but with a stack trace on stderr.
			throw new MojoExecutionException(
					"No conformance config files given. Set 'conformanceConfigs' and / or 'conformanceConfigsBasePath'.");
		}

		args.add(CONFORMANCE_CONFIG_FLAG);
		args.add(allConformanceConfigs);

		return args;
	}

	private SoyConformanceChecker createSoyConformanceCheckerInstance() {
		try {
			Constructor<SoyConformanceChecker> constr = SoyConformanceChecker.class.getDeclaredConstructor();
			constr.setAccessible(true);
			return constr.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to create instance of 'SoyConformanceChecker' class.", e);
		}
	}
}
