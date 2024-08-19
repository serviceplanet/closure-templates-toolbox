package nl.serviceplanet.closuretemplates.toolbox.maven;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SoyFileDiscovery {
	public static String findSoyFilesAsCSV(Path basePath) {
		return findSoyFiles(basePath).stream()
				.map(p -> p.toFile().getAbsolutePath())
				.collect(Collectors.joining(","));
	}

	public static ImmutableSet<Path> findSoyFiles(Path basePath) {
		try (Stream<Path> stream = Files.walk(basePath, 200)) {
			return stream
					.filter(path -> path.getFileName().toString().endsWith(".soy"))
					.collect(ImmutableSet.toImmutableSet());
		} catch (IOException e) {
			throw new IllegalStateException("Failed searching for soy-files in: " + basePath, e);
		}
	}
}
