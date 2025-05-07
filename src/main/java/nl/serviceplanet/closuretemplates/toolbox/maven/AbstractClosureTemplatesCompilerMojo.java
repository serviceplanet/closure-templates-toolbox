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

import org.apache.maven.plugin.AbstractMojo;
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

	/**
	 * List of Soy (Closure Templates) template files on which to work.
	 * <br /><br />
	 * Contents are passed to the {@code SoyToJbcSrcCompiler} as arguments of the {@code --srcs} flag.
	 */
	@Parameter(property = "soySources")
	private String soySources;

	/**
	 * List of Soy (Closure Templates) template files on which to work.
	 * <br /><br />
	 * Contents are passed to the {@code SoyToJbcSrcCompiler} as arguments of the {@code --srcs} flag.
	 */
	@Parameter(property = "soySourcesBasePath")
	private String soySourcesBasePath;

	/**
	 * Location of protocol buffer definitions in the form of a file descriptor set. These are the 'direct dependencies'
	 * of the compilation unit -- any protos imported by Soy files in this compilation unit must be listed here.
	 * <br /><br />
	 * Contents are passed to the {@code SoyToJbcSrcCompiler} as arguments of the {@code directProtoDeps} flag.
	 */
	@Parameter(property = "directProtoDeps")
	protected String directProtoDeps;

	/**
	 * Location of protocol buffer definitions in the form of a file descriptor set. These are the 'indirect
	 * dependencies' of the compilation unit and must include all transitive dependencies of the files passed to
	 * {@code directProtoDeps}.
	 * <br /><br />
	 * Contents are passed to the {@code SoyToJbcSrcCompiler} as arguments of the {@code indirectProtoDeps} flag.
	 */
	@Parameter(property = "indirectProtoDeps")
	protected String indirectProtoDeps;

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

		return baseCliFlags;
	}
}
