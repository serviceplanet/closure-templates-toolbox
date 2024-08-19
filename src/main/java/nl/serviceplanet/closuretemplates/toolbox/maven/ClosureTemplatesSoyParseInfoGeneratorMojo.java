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

import com.google.template.soy.AbstractSoyCompiler;
import com.google.template.soy.SoyParseInfoGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "soy-parse-info-generator", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public final class ClosureTemplatesSoyParseInfoGeneratorMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "javaPackage")
	private String javaPackage;

	@Parameter(property = "soySourcesBasePath")
	private String soySourcesBasePath;

	@Parameter(property = "outputDirectory")
	private String outputDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		String[] args = generateCliFlags().toArray(new String[0]);


		SoyParseInfoGenerator generator = createSoyParseInfoGeneratorInstance();
		int exitcode = callRunMethod(generator, args, System.err);

		if (exitcode != 0) {
			throw new MojoFailureException("SoyMsgExtractor returned exit-code: " + exitcode);
		}

		// N.B.: add the generated java-source as a source-root
		project.addCompileSourceRoot(outputDirectory);
	}

	private List<String> generateCliFlags() {
		List<String> args = new ArrayList<>();

		args.add("--generateBuilders");

		args.add("--javaPackage");
		args.add(javaPackage);

		args.add("--javaClassNameSource");
		args.add("filename");

		args.add("--srcs");
		args.add(SoyFileDiscovery.findSoyFilesAsCSV(Path.of(soySourcesBasePath)));

		args.add("--outputDirectory");
		args.add(outputDirectory);

		return args;
	}

	private SoyParseInfoGenerator createSoyParseInfoGeneratorInstance() {
		try {
			Constructor<SoyParseInfoGenerator> constr = SoyParseInfoGenerator.class.getDeclaredConstructor();
			constr.setAccessible(true);
			return constr.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to create instance of 'SoyMsgExtractor' class.", e);
		}
	}

	private int callRunMethod(SoyParseInfoGenerator instance, String[] args, PrintStream stderr) {
		try {
			Method method = AbstractSoyCompiler.class.getDeclaredMethod(
					"run",
					String[].class,
					PrintStream.class
			);
			method.setAccessible(true);
			return (Integer) method.invoke(instance, args, stderr);
		} catch (Exception e) {
			throw new RuntimeException("Unable to execute 'run' method on 'AbstractSoyCompiler' class.", e);
		}
	}
}
