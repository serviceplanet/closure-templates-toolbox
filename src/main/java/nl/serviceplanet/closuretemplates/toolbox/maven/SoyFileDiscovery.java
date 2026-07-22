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

import com.google.common.collect.ImmutableSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class SoyFileDiscovery {

	private static final String SOY_EXTENSION = ".soy";

	private static final String TOGGLES_EXTENSION = ".toggles";

	public static ImmutableSet<Path> findSoyFiles(Path basePath) {
		return findFiles(basePath, SOY_EXTENSION);
	}

	public static ImmutableSet<Path> findTogglesFiles(Path basePath) {
		return findFiles(basePath, TOGGLES_EXTENSION);
	}

	private static ImmutableSet<Path> findFiles(Path basePath, String extension) {
		if (!Files.exists(basePath)) {
			throw new IllegalArgumentException("base-path not found: " + basePath + ", absolute-path: " + basePath.toAbsolutePath());
		}

		if (Files.isRegularFile(basePath)) {
			return ImmutableSet.of(basePath.normalize());
		}

		try (Stream<Path> stream = Files.walk(basePath, 200)) {
			return stream
					.filter(path -> path.getFileName().toString().endsWith(extension))
					// Normalized, so no '.' or '..' segments leak into the paths handed to the Soy compiler or into
					// the workdir-relativization which determines the logical path of each file.
					.map(Path::normalize)
					.sorted(Comparator.comparing(x -> x.toString().contains("/target/")).reversed().thenComparing(Object::toString))
					.collect(ImmutableSet.toImmutableSet());
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Failed searching for '%s' files in: '%s'.", extension, basePath), e);
		}
	}
}
