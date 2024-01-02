package nl.serviceplanet.maven.closureteemplatecompiler;

import com.google.common.io.ByteSink;
import com.google.common.io.Files;
import com.google.template.soy.AbstractSoyCompiler;
import com.google.template.soy.SoyFileSet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Allows us to call the Soy compiler from our own Maven code. 
 * 
 * It uses a reflection hack to do so. We can't simply call it's main method because it in turn calls a method which 
 * calls {@code System.exit()}. We also can't only extend {@code AbstractSoyCompiler} because it needs to call 
 * private methods.
 * 
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
public final class EmbeddedSoyCompiler extends AbstractSoyCompiler {
	
	private final Path outputJar;
	
	public EmbeddedSoyCompiler(Path outputJar) {
		this.outputJar = outputJar;
	}
	
	@Override
	protected void compile(SoyFileSet.Builder sfsBuilder) throws IOException {
		try {
			SoyFileSet sfs = sfsBuilder.build();
			
			Class<?>[] arguments = new Class[]{
					ByteSink.class,
					Optional.class
			};
			Method method = SoyFileSet.class.getDeclaredMethod("compileToJar", arguments);
			method.setAccessible(true);
			method.invoke(sfs, Files.asByteSink(outputJar.toFile()), Optional.empty());
			
		} catch (Exception e) {
			throw new RuntimeException("Unable to invoke 'compileToJar' method on 'SoyFileSet' class.", e);
		}
	}
}
