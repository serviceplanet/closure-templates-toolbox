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

import jnr.posix.POSIXFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles generating command line arguments for options defined in the {@code com.google.template.soy.AbstractSoyCompiler}
 * class which are shared between various Soy compilers.
 *
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
abstract class AbstractClosureTemplatesCompilerMojo extends AbstractMojo {

	/** Soy compiler CLI flag to specify which Soy source files to compile. */
	private static final String SRCS_FLAG = "--srcs";

	private static final String DIRECT_PROTO_DEPS_FLAG = "--directProtoDeps";

	private static final String INDIRECT_PROTO_DEPS_FLAG = "--indirectProtoDeps";

	private static final String DEP_HEADERS_FLAG = "--depHeaders";

	private static final String INDIRECT_DEP_HEADERS_FLAG = "--indirectDepHeaders";

	/**
	 * For various Soy compilers, such as the Soy header compiler, the working directory from which the compiler is
	 * invoked is significant. For example in case of the Soy header compiler the full path used in the Soy compiler
	 * {@code --srcs} flag ends-up in the name of the template in the Protobuf header file. In which case you probably
	 * want to set the {@code workdir} to something like {@code ${project.basedir}/src/main/resources} in your POM and use
	 * the {@code --srcs} flag with relative paths.
	 * <br /><br />
	 * If you don't explicitly need this (for example the Soy compiler in question does not rely on relative paths) you
	 * are discouraged from using this. The reason is this uses voodoo at the lower levels which might break in the
	 * future.
	 */
	@Parameter(property = "workdir")
	protected String workdir;

	/**
	 * List of Soy (Closure Templates) template files on which to work.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code --srcs} flag.
	 */
	@Parameter(property = "soySources")
	private String soySources;

	/**
	 * Finds all Soy (Closure Templates) files under the specified path and passes them as comma-separated relative
	 * paths to the Soy compiler.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code --srcs} flag.
	 */
	@Parameter(property = "soySourcesBasePath")
	private String soySourcesBasePath;

	/**
	 * Location of protocol buffer definitions in the form of a file descriptor set. These are the 'direct dependencies'
	 * of the compilation unit -- any protos imported by Soy files in this compilation unit must be listed here.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code directProtoDeps} flag.
	 */
	@Parameter(property = "directProtoDeps")
	protected String directProtoDeps;

	/**
	 * Location of protocol buffer definitions in the form of a file descriptor set. These are the 'indirect
	 * dependencies' of the compilation unit and must include all transitive dependencies of the files passed to
	 * {@code directProtoDeps}.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code indirectProtoDeps} flag.
	 */
	@Parameter(property = "indirectProtoDeps")
	protected String indirectProtoDeps;

	/**
	 * Comma seperated list of file paths with dependency Soy header files (if applicable). The compiler needs deps for
	 * analysis/checking.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code depHeaders} flag.
	 */
	@Parameter(property = "depHeaders")
	protected String depHeaders;

	/**
	 * Comma seperated list of file paths with dependency Soy header files (if applicable) required by deps, but which
	 * may not be used by srcs. Used by the compiler for typechecking and call analysis.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code indirectDepHeaders} flag.
	 */
	@Parameter(property = "indirectDepHeaders")
	protected String indirectDepHeaders;

	protected List<String> generateBaseCliFlags() {
		List<String> baseCliFlags = new ArrayList<>();

		String allSoySources = "";
		if (soySourcesBasePath != null && !soySourcesBasePath.isBlank()) {
			allSoySources += SoyFileDiscovery.findSoyFilesAsCSV(Path.of(soySourcesBasePath));
		}
		if (soySources != null && !soySources.isBlank()) {
			if (!allSoySources.isEmpty()) {
				allSoySources += ",";
			}
			allSoySources += soySources;
		}
		baseCliFlags.add(SRCS_FLAG);
		baseCliFlags.add(allSoySources);

		if (directProtoDeps != null && !directProtoDeps.isBlank()) {
			baseCliFlags.add(DIRECT_PROTO_DEPS_FLAG);
			baseCliFlags.add(directProtoDeps);
		}

		if (indirectProtoDeps != null && !indirectProtoDeps.isBlank()) {
			baseCliFlags.add(INDIRECT_PROTO_DEPS_FLAG);
			baseCliFlags.add(indirectProtoDeps);
		}

		if (depHeaders != null && !depHeaders.isBlank()) {
			baseCliFlags.add(DEP_HEADERS_FLAG);
			baseCliFlags.add(depHeaders);
		}

		if (indirectDepHeaders != null && !indirectDepHeaders.isBlank()) {
			baseCliFlags.add(INDIRECT_DEP_HEADERS_FLAG);
			baseCliFlags.add(indirectDepHeaders);
		}

		return baseCliFlags;
	}

	/**
	 * You should probably not override this method, override {@link #executeMojo()} instead. This method ensures
	 * various base settings get configured.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String originalWorkDir = System.getProperty("user.dir");

		try {
			if (workdir != null && !workdir.isBlank()) {
				String workdirStr = null;

				try {
					workdirStr = Path.of(workdir).toAbsolutePath().toString();
					getLog().debug(String.format("Using workdir '%s'.", workdirStr));
					POSIXFactory.getNativePOSIX().chdir(workdirStr);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Unable to change work dir to '%s'", workdirStr), e);
				}
			}

			executeMojo();
		} finally {
			try {
				if (workdir != null) {
					POSIXFactory.getNativePOSIX().chdir(originalWorkDir);
				}
			} catch (Exception e) {
				getLog().debug(String.format("Unable to revert workdir to original '%s'.", originalWorkDir), e);
			}
		}
	}

	public abstract void executeMojo() throws MojoExecutionException, MojoFailureException;
}
