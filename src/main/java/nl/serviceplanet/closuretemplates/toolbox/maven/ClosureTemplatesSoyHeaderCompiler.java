package nl.serviceplanet.closuretemplates.toolbox.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

@Mojo(name = "soy-header-compiler", defaultPhase = LifecyclePhase.VERIFY)
public final class ClosureTemplatesSoyHeaderCompiler extends AbstractClosureTemplatesCompilerMojo {

	private static final String OUTPUT_FLAG = "--output";
	
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "outputFile")
	private String outputFile;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().debug("Invoking Soy header compiler.");
		
		String[] compilerCliArgs = generateCliFlags().toArray(new String[0]);

		int exitCode = runSoyHeaderCompilerInstance(compilerCliArgs, System.err);
		if (exitCode != 0) {
			throw new MojoFailureException("SoyHeaderCompiler returned exit-code: " + exitCode);
		}
	}

	/**
	 * Generates the "command line" flags and arguments we are going to pass to the Soy compiler.
	 */
	private List<String> generateCliFlags() {
		List<String> compilerCliArgs = generateBaseCliFlags();
		
		compilerCliArgs.add(OUTPUT_FLAG);
		compilerCliArgs.add(outputFile);

		return compilerCliArgs;
	}

	/**
	 * Instantiates and runs the 'run' method of the 'SoyHeaderCompiler' class.  Because the 'SoyHeaderCompiler' class 
	 * is package-private (which differs from the other compiler classes such as 'SoyToJbcSrcCompiler') we need to use 
	 * more reflection voodoo.
	 */
	private int runSoyHeaderCompilerInstance(String[] args, PrintStream err) throws MojoExecutionException {		
		try {
			// Create instance.
			Class<?> clazz = Class.forName("com.google.template.soy.SoyHeaderCompiler");
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			Object instance = constructor.newInstance();

			// Invoke 'run' method.
			Method method = clazz.getMethod(
					"run",
					String[].class,
					PrintStream.class
			);
			method.setAccessible(true);

			return (int)method.invoke(instance, args, err);
		} catch (Exception e) {
			throw new MojoExecutionException("Unable to create instance of 'SoyHeaderCompiler' class and invoke 'run' method.", e);
		}
	}
}
