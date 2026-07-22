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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which Soy toggles (feature flags) are switched on for rendering, checked against the toggles the templates actually
 * declare.
 * <br /><br />
 * Soy evaluates a toggle by asking the renderer's "active mod selector" whether the toggle name is active, so enabling
 * a toggle is a matter of passing a {@link Predicate} which accepts its name:
 *
 * <pre>{@code
 * ToggleFile featureToggles = ToggleFile.fromClasspath("nl/serviceplanet/app/feature.toggles");
 *
 * SoyToggles toggles = SoyToggles.builder()
 *         .declareFrom(featureToggles)
 *         .enable("NEW_UI")
 *         .build();
 *
 * sauce.renderTemplate(templateName)
 *         .setData(data)
 *         .setActiveModSelector(toggles.activeModSelector())
 *         .renderHtml();
 * }</pre>
 *
 * The reason to go through this class rather than pass a bare {@code Set::contains} is that Soy silently renders an
 * unrecognised toggle name as {@code false}. A typo in the name of a toggle you meant to switch on therefore produces
 * no error at all, just the wrong output. Enabling a toggle which is not declared in any of the toggles files handed
 * to {@link Builder#declareFrom} fails here instead.
 * <br /><br />
 * Note that Soy uses this same selector to decide which <em>mods</em> are active, so toggle names and mod names share
 * one namespace. If the templates use both, combine the two predicates:
 * {@code toggles.activeModSelector().or(activeMods::contains)}.
 *
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
public final class SoyToggles {

	private final ImmutableSet<String> declaredToggles;

	private final ImmutableSet<String> enabledToggles;

	private SoyToggles(ImmutableSet<String> declaredToggles, ImmutableSet<String> enabledToggles) {
		this.declaredToggles = declaredToggles;
		this.enabledToggles = enabledToggles;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** All toggles the templates declare, whether switched on or not. */
	public ImmutableSet<String> declaredToggles() {
		return declaredToggles;
	}

	/** The subset of {@link #declaredToggles()} which is switched on. */
	public ImmutableSet<String> enabledToggles() {
		return enabledToggles;
	}

	/**
	 * Whether the given toggle is switched on. Useful to branch on the same toggle outside of a template.
	 *
	 * @throws IllegalArgumentException if the toggle is not declared in any of the toggles files.
	 */
	public boolean isEnabled(String toggleName) {
		if (!declaredToggles.contains(toggleName)) {
			throw new IllegalArgumentException(String.format(
					"Unknown toggle '%s'. Declared toggles are: %s.", toggleName, declaredToggles));
		}
		return enabledToggles.contains(toggleName);
	}

	/**
	 * The predicate to hand to {@code setActiveModSelector} on a SoySauce or Tofu renderer.
	 */
	public Predicate<String> activeModSelector() {
		return enabledToggles::contains;
	}

	public static final class Builder {

		private final Set<String> declaredToggles = new LinkedHashSet<>();

		private final Set<String> enabledToggles = new LinkedHashSet<>();

		private Builder() {
		}

		/**
		 * Declares the toggles of the given toggles files, i.e. the complete set of toggles which may be enabled.
		 */
		public Builder declareFrom(ToggleFile... toggleFiles) {
			return declareFrom(Arrays.asList(toggleFiles));
		}

		public Builder declareFrom(Iterable<ToggleFile> toggleFiles) {
			for (ToggleFile toggleFile : toggleFiles) {
				declaredToggles.addAll(toggleFile.toggleNames());
			}
			return this;
		}

		/**
		 * Switches the given toggles on. Every name has to be declared by one of the toggles files passed to
		 * {@link #declareFrom}, which {@link #build()} verifies. If the names come from a configuration source which
		 * also holds flags unrelated to Soy, intersect them with {@link ToggleFile#toggleNames()} before passing them
		 * here.
		 */
		public Builder enable(String... toggleNames) {
			return enable(Arrays.asList(toggleNames));
		}

		public Builder enable(Iterable<String> toggleNames) {
			toggleNames.forEach(enabledToggles::add);
			return this;
		}

		/**
		 * @throws IllegalArgumentException if a toggle was enabled which no toggles file declares.
		 */
		public SoyToggles build() {
			Set<String> undeclared = new LinkedHashSet<>(enabledToggles);
			undeclared.removeAll(declaredToggles);
			if (!undeclared.isEmpty()) {
				throw new IllegalArgumentException(String.format(
						"Cannot enable toggles %s because no toggles file declares them. Declared toggles are: %s.",
						undeclared, declaredToggles));
			}

			return new SoyToggles(ImmutableSet.copyOf(declaredToggles), ImmutableSet.copyOf(enabledToggles));
		}
	}
}
