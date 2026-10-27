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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles generating command line arguments for options defined in the {@code com.google.template.soy.AbstractSoyCompiler}
 * class which are shared between various Soy compilers.
 * <br /><br />
 * The Soy compiler knows each source and toggles file by the literal path string it is handed. That string becomes the
 * file's logical path, which is what ends up as the template's file path and what toggle import statements are matched
 * against (as an exact string). To keep those logical paths stable regardless of where the Maven JVM happens to run,
 * this plugin resolves all paths against {@link #workdir} to absolute paths and passes the Soy compiler a
 * {@code --generated_files} mapping from each absolute path back to its workdir-relative logical path.
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

	private static final String JAVA_EXTERN_DEFINITIONS_FLAG = "--java_extern_defn_jars";

	/** Soy compiler CLI flag to specify which Soy toggles files declare the available toggles. */
	private static final String TOGGLES_FILES_FLAG = "--togglesFiles";

	/**
	 * Soy compiler CLI flag holding a map of {@code real path=logical path} entries. The compiler reads each file
	 * through its real path but knows it by its logical path. This is what lets this plugin pass absolute paths (which
	 * resolve independently of the working directory) while keeping template names and toggle import paths
	 * workdir-relative.
	 */
	private static final String GENERATED_FILES_FLAG = "--generated_files";

	/**
	 * The base directory against which the relative paths of the other parameters are resolved. It also determines the
	 * logical name of each source and toggles file. A file under the workdir is known to the Soy compiler by its
	 * workdir-relative path, which is what ends up as the template's file path and as the path templates must import a
	 * toggles file by. Typically something like {@code ${project.basedir}/src/main/resources}.
	 * <br /><br />
	 * When unset, paths resolve against the directory the Maven JVM was launched from.
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

	/**
	 * Comma separated list of file paths with JAR files containing classes referenced by Soy externs.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code java_extern_defn_jars} flag.
	 */
	@Parameter(property = "javaExternDefinitionJars")
	protected String javaExternDefinitionJars;

	/**
	 * Comma separated list of file paths of Soy toggles files. A toggles file is a plain newline-delimited list of
	 * toggle (feature flag) names, which templates make available with an import statement, after which the toggle can
	 * be used as a {@code bool}.
	 * <br /><br />
	 * A relative path is resolved against {@link #workdir} and doubles as the path templates must import the toggles
	 * file by.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code togglesFiles} flag.
	 */
	@Parameter(property = "togglesFiles")
	protected String togglesFiles;

	/**
	 * Finds all Soy toggles files under the specified path and passes them to the Soy compiler.
	 * <br /><br />
	 * Toggles files under {@link #workdir} are known to the compiler by their workdir-relative path, which is the path
	 * templates must import them by. A toggles file <em>outside</em> the workdir gets an absolute logical path, which
	 * no template will spell in an import statement. Keep toggles files under the workdir.
	 * <br /><br />
	 * Contents are passed to the Closure templates / Soy compiler as arguments of the {@code togglesFiles} flag.
	 */
	@Parameter(property = "togglesFilesBasePath")
	protected String togglesFilesBasePath;

	protected List<String> generateBaseCliFlags() {
		List<String> baseCliFlags = new ArrayList<>();

		Path workdirPath = workdirPath();
		// Maps the absolute path this plugin hands the Soy compiler for each source and toggles file to the
		// workdir-relative logical path the compiler should know the file by. Passed via the --generated_files flag.
		Map<String, String> logicalPaths = new LinkedHashMap<>();

		baseCliFlags.add(SRCS_FLAG);
		baseCliFlags.add(combine(
				discoverFiles(soySourcesBasePath, SoyFileDiscovery::findSoyFiles, workdirPath, logicalPaths),
				resolvePaths(soySources, workdirPath, logicalPaths)));

		if (!isBlank(directProtoDeps)) {
			baseCliFlags.add(DIRECT_PROTO_DEPS_FLAG);
			baseCliFlags.add(resolvePaths(directProtoDeps, workdirPath, null));
		}

		if (!isBlank(indirectProtoDeps)) {
			baseCliFlags.add(INDIRECT_PROTO_DEPS_FLAG);
			baseCliFlags.add(resolvePaths(indirectProtoDeps, workdirPath, null));
		}

		if (!isBlank(depHeaders)) {
			baseCliFlags.add(DEP_HEADERS_FLAG);
			baseCliFlags.add(resolvePaths(depHeaders, workdirPath, null));
		}

		if (!isBlank(indirectDepHeaders)) {
			baseCliFlags.add(INDIRECT_DEP_HEADERS_FLAG);
			baseCliFlags.add(resolvePaths(indirectDepHeaders, workdirPath, null));
		}

		if (!isBlank(javaExternDefinitionJars)) {
			baseCliFlags.add(JAVA_EXTERN_DEFINITIONS_FLAG);
			baseCliFlags.add(resolvePaths(javaExternDefinitionJars, workdirPath, null));
		}

		String allTogglesFiles = combine(
				discoverFiles(togglesFilesBasePath, SoyFileDiscovery::findTogglesFiles, workdirPath, logicalPaths),
				resolvePaths(togglesFiles, workdirPath, logicalPaths));
		if (!allTogglesFiles.isEmpty()) {
			baseCliFlags.add(TOGGLES_FILES_FLAG);
			baseCliFlags.add(allTogglesFiles);
		}

		if (!logicalPaths.isEmpty()) {
			baseCliFlags.add(GENERATED_FILES_FLAG);
			baseCliFlags.add(logicalPaths.entrySet().stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue())
					.collect(Collectors.joining(",")));
		}

		return baseCliFlags;
	}

	/**
	 * The absolute base directory against which all relative paths are resolved.
	 */
	private Path workdirPath() {
		return (isBlank(workdir) ? Path.of("") : Path.of(workdir)).toAbsolutePath().normalize();
	}

	/**
	 * Resolves a single path (such as an output file) against {@link #workdir} when it is relative. For use by
	 * subclasses on their own path parameters.
	 */
	protected Path resolveAgainstWorkdir(String path) {
		return workdirPath().resolve(path).normalize();
	}

	/**
	 * Resolves each relative path in the comma-separated list against {@link #workdir}. For use by subclasses on
	 * their own path parameters.
	 */
	protected String resolvePathsAgainstWorkdir(String csvPaths) {
		return resolvePaths(csvPaths, workdirPath(), null);
	}

	/**
	 * Finds all files with the discovery function under the base path (resolved against {@link #workdir}) and returns
	 * them as a comma-separated list of absolute paths. For use by subclasses on their own base path parameters, for
	 * files which are not known to the Soy compiler by a logical path (such as conformance configs).
	 */
	protected String discoverFilesAgainstWorkdir(String basePath, Function<Path, ImmutableSet<Path>> discovery) {
		return discoverFiles(basePath, discovery, workdirPath(), null);
	}

	/**
	 * Finds all files with the discovery function under the base path (resolved against the workdir) and returns them
	 * as a comma-separated list of absolute paths. When {@code logicalPaths} is given, the workdir-relative logical
	 * path of each file found under the workdir is registered in it.
	 */
	private static String discoverFiles(String basePath, Function<Path, ImmutableSet<Path>> discovery,
			Path workdirPath, Map<String, String> logicalPaths) {
		if (isBlank(basePath)) {
			return "";
		}

		Path base = workdirPath.resolve(basePath).normalize();
		return discovery.apply(base).stream()
				.map(file -> {
					String absolute = file.toString();
					if (logicalPaths != null && file.startsWith(workdirPath)) {
						logicalPaths.put(absolute, toLogicalPath(workdirPath.relativize(file)));
					}
					return absolute;
				})
				.collect(Collectors.joining(","));
	}

	/**
	 * Resolves each relative path in the comma-separated list against the workdir, returning a comma-separated list of
	 * absolute paths. When {@code logicalPaths} is given, each resolved relative path is registered with its original
	 * relative spelling as the logical path. Absolute paths are passed through untouched (and thus keep an absolute
	 * logical path).
	 */
	private static String resolvePaths(String csvPaths, Path workdirPath, Map<String, String> logicalPaths) {
		if (isBlank(csvPaths)) {
			return "";
		}

		return Arrays.stream(csvPaths.split(","))
				.filter(path -> !isBlank(path))
				.map(pathStr -> {
					Path path = Path.of(pathStr);
					if (path.isAbsolute()) {
						return pathStr;
					}

					Path relative = path.normalize();
					String absolute = workdirPath.resolve(relative).toString();
					if (logicalPaths != null) {
						logicalPaths.put(absolute, toLogicalPath(relative));
					}
					return absolute;
				})
				.collect(Collectors.joining(","));
	}

	/**
	 * Logical paths (template file paths, toggle import paths) always use forward slashes, also on Windows.
	 */
	private static String toLogicalPath(Path relativePath) {
		return relativePath.toString().replace(File.separatorChar, '/');
	}

	/**
	 * Whether the value is unset, i.e. null, empty or whitespace only. Guava offers {@code Strings.isNullOrEmpty},
	 * but no variant which also treats a whitespace-only value as unset.
	 */
	private static boolean isBlank(String value) {
		return Strings.nullToEmpty(value).isBlank();
	}

	/**
	 * Joins the non-blank arguments into a single comma-separated list.
	 */
	protected static String combine(String... values) {
		return Arrays.stream(values)
				.filter(value -> !isBlank(value))
				.collect(Collectors.joining(","));
	}
}
