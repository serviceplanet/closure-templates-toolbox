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
package nl.serviceplanet.closuretemplates.toolbox.toggle;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.io.Resources;
import com.google.template.soy.base.SourceFilePath;
import com.google.template.soy.shared.ToggleRegistry;
import com.google.template.soy.shared.internal.ImmutableSetMultimapToggleRegistry;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The declared contents of a single Soy toggles file; The path templates import it by, together with the toggle
 * (feature flag) names it declares.
 * <br /><br />
 * A toggles file is a plain newline-delimited list of toggle names. Templates make an individual toggle available
 * with an import, after which it can be used as a {@code bool}:
 *
 * <pre>{@code
 * import {NEW_UI} from 'nl/serviceplanet/app/feature.toggles';
 *
 * {template greeting}
 *   {if NEW_UI}new UI{else}old UI{/if}
 * {/template}
 * }</pre>
 *
 * The Soy compiler matches the import statement against {@link #importPath()} as an exact string; there is no path
 * normalization or resolution. So the path passed to the compiler (via the {@code togglesFiles} Maven parameter, which
 * interprets it relative to {@code workdir}) must be spelled exactly as the templates import it.
 *
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
public record ToggleFile(String importPath, ImmutableSet<String> toggleNames) {

	/**
	 * Toggle names have to be usable as an imported symbol in a template, so they are restricted to Soy identifiers.
	 */
	private static final Pattern TOGGLE_NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");

	public ToggleFile {
		if (importPath == null || importPath.isBlank()) {
			throw new IllegalArgumentException("Toggles file import path must not be blank.");
		}
		toggleNames = ImmutableSet.copyOf(toggleNames);
		for (String toggleName : toggleNames) {
			if (!TOGGLE_NAME_PATTERN.matcher(toggleName).matches()) {
				throw new IllegalArgumentException(String.format(
						"Toggle name '%s' in toggles file '%s' is not a valid Soy identifier, so no template can "
								+ "import it.", toggleName, importPath));
			}
		}
	}

	public static ToggleFile of(String importPath, String... toggleNames) {
		return new ToggleFile(importPath, ImmutableSet.copyOf(toggleNames));
	}

	/**
	 * Reads a toggles file from the classpath, using the resource path as the path templates import it by.
	 * <br /><br />
	 * This is the common case when the Soy sources live in {@code src/main/resources} and the Maven goals are
	 * configured with {@code workdir} pointing at that directory: the classpath resource path and the import path are
	 * then the same string.
	 */
	public static ToggleFile fromClasspath(String resourcePath) {
		return read(Resources.getResource(resourcePath), resourcePath);
	}

	public static ToggleFile read(URL url, String importPath) {
		try {
			return parse(Resources.asCharSource(url, StandardCharsets.UTF_8).read(), importPath);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Unable to read toggles file '%s'.", url), e);
		}
	}

	public static ToggleFile read(Path file, String importPath) {
		try {
			return parse(Files.readString(file, StandardCharsets.UTF_8), importPath);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Unable to read toggles file '%s'.", file), e);
		}
	}

	/**
	 * Parses the contents of a toggles file, one toggle name per line.
	 * <br /><br />
	 * Blank lines are ignored. Every other line has to be a bare toggle name. The Soy compiler takes each line
	 * verbatim, so a line carrying anything else (surrounding whitespace, a comment, several names) would register a
	 * toggle no template can ever import. Rather than let that fail later as a confusing "unknown symbol" error on the
	 * import statement, it is rejected here.
	 */
	public static ToggleFile parse(String content, String importPath) {
		ImmutableSet.Builder<String> toggleNames = ImmutableSet.builder();

		List<String> lines = content.lines().toList();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}

			if (!TOGGLE_NAME_PATTERN.matcher(line).matches()) {
				throw new IllegalArgumentException(String.format(
						"Line %s of toggles file '%s' is not a bare toggle name: '%s'. A toggles file contains one "
								+ "toggle name per line and supports no other syntax (such as comments).",
						i + 1, importPath, line));
			}
			toggleNames.add(line);
		}

		return new ToggleFile(importPath, toggleNames.build());
	}

	/**
	 * Builds the registry the Soy compiler uses to resolve toggle imports. Needed when compiling templates
	 * programmatically (via {@code SoyFileSet.Builder.setToggleRegistry}); the Maven goals instead pass the toggles
	 * files to the compiler with the {@code togglesFiles} parameter.
	 */
	public static ToggleRegistry toggleRegistry(Iterable<ToggleFile> toggleFiles) {
		ImmutableSetMultimap.Builder<SourceFilePath, String> registry = ImmutableSetMultimap.builder();
		for (ToggleFile toggleFile : toggleFiles) {
			registry.putAll(
					SourceFilePath.create(toggleFile.importPath(), toggleFile.importPath()),
					toggleFile.toggleNames());
		}
		return ImmutableSetMultimapToggleRegistry.create(registry.build());
	}
}
