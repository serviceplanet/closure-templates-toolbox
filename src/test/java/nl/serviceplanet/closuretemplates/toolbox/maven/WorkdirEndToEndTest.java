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

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the real {@code soy-to-bytecode} goal against the real Soy compiler, with {@code workdir} pointing away from
 * the working directory of the test JVM. 
 */
public final class WorkdirEndToEndTest {

	/** Toggle import path used inside the template, relative to the workdir. */
	private static final String TOGGLES_IMPORT_PATH = "nl/serviceplanet/demo/feature.toggles";

	private static ClosureTemplatesSoyToJbcMojo createMojo(Path workdir, Path outputDir) throws Exception {
		ClosureTemplatesSoyToJbcMojo mojo = new ClosureTemplatesSoyToJbcMojo();

		setField(mojo, AbstractClosureTemplatesCompilerMojo.class, "workdir", workdir.toString());
		setField(mojo, AbstractClosureTemplatesCompilerMojo.class, "soySourcesBasePath", ".");
		setField(mojo, AbstractClosureTemplatesCompilerMojo.class, "togglesFilesBasePath", ".");

		MavenProject project = new MavenProject();
		project.getBuild().setOutputDirectory(outputDir.toString());
		setField(mojo, ClosureTemplatesSoyToJbcMojo.class, "project", project);

		return mojo;
	}

	private static void setField(Object target, Class<?> declaringClass, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * Writes a workdir holding a toggles file and a template importing a toggle from it by its workdir-relative path.
	 */
	private static void writeSoyModule(Path workdir, String namespace) throws Exception {
		Path templateDir = workdir.resolve("nl/serviceplanet/demo");
		Files.createDirectories(templateDir);
		Files.writeString(templateDir.resolve("feature.toggles"), "NEW_UI\n");
		Files.writeString(templateDir.resolve("greeting.soy"), """
				{namespace %s}

				import {NEW_UI} from '%s';

				{template greeting}
				  {if NEW_UI}new UI{else}old UI{/if}
				{/template}
				""".formatted(namespace, TOGGLES_IMPORT_PATH));
	}

	@Test
	public void testCompileWithWorkdirAwayFromProcessWorkingDirectory(@TempDir Path tempDir) throws Exception {
		Path workdir = Files.createDirectory(tempDir.resolve("module"));
		Path outputDir = Files.createDirectory(tempDir.resolve("classes"));
		writeSoyModule(workdir, "nl.serviceplanet.demo");

		// The workdir points at a temp directory elsewhere.
		Assertions.assertTrue(Files.exists(Path.of("pom.xml")), "test precondition: running from the project directory");

		createMojo(workdir, outputDir).execute();

		Assertions.assertTrue(Files.exists(Path.of("pom.xml")),
				"the process working directory changed, the goal must not touch process state");
		try (var classFiles = Files.walk(outputDir)) {
			Assertions.assertTrue(classFiles.anyMatch(f -> f.toString().endsWith(".class")),
					"no compiled template class file was produced");
		}
	}

	@Test
	public void testConcurrentCompilesWithDifferentWorkdirs(@TempDir Path tempDir) throws Exception {
		int concurrentExecutions = 4;

		List<ClosureTemplatesSoyToJbcMojo> mojos = new ArrayList<>();
		List<Path> outputDirs = new ArrayList<>();
		for (int i = 0; i < concurrentExecutions; i++) {
			Path workdir = Files.createDirectory(tempDir.resolve("module-" + i));
			Path outputDir = Files.createDirectory(tempDir.resolve("classes-" + i));
			writeSoyModule(workdir, "nl.serviceplanet.demo" + i);
			mojos.add(createMojo(workdir, outputDir));
			outputDirs.add(outputDir);
		}

		ExecutorService pool = Executors.newFixedThreadPool(concurrentExecutions);
		try {
			// Release all executions at once, like a parallel Maven build running these goals concurrently.
			CountDownLatch startSignal = new CountDownLatch(1);

			List<Future<?>> results = new ArrayList<>();
			for (ClosureTemplatesSoyToJbcMojo mojo : mojos) {
				results.add(pool.submit(() -> {
					startSignal.await();
					mojo.execute();
					return null;
				}));
			}

			startSignal.countDown();
			for (Future<?> result : results) {
				result.get(120, TimeUnit.SECONDS); // Throws when the goal it ran failed.
			}
		} finally {
			pool.shutdownNow();
		}

		for (Path outputDir : outputDirs) {
			try (var classFiles = Files.walk(outputDir)) {
				Assertions.assertTrue(classFiles.anyMatch(f -> f.toString().endsWith(".class")),
						"execution with output " + outputDir + " produced no compiled template class file");
			}
		}
	}
}
