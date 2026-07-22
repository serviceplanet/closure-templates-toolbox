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
package nl.serviceplanet.closuretemplates.toolbox;

import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.SoyUtil;
import nl.serviceplanet.closuretemplates.toolbox.toggle.SoyToggles;
import nl.serviceplanet.closuretemplates.toolbox.toggle.ToggleFile;
import nl.serviceplanet.closuretemplates.toolbox.util.SoyTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public final class TogglesTest {
	private static final String SOY_FILE = "toggles.soy";
	private static final String TOGGLES_FILE = "feature.toggles";

	private static ToggleFile toggleFile;
	private static SoyUtil.SoyCompilation soy;

	@BeforeAll
	public static void setup() throws Exception {
		toggleFile = ToggleFile.fromClasspath(SoyTestUtil.getPathToResource(TOGGLES_FILE));
		soy = SoyUtil.compileSoyFile(SoyTestUtil.getPathToResource(SOY_FILE), toggleFile);
	}

	private static String render(String... enabledToggles) {
		SoyToggles toggles = SoyToggles.builder()
				.declareFrom(toggleFile)
				.enable(enabledToggles)
				.build();

		return SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), Map.of(), null,
				toggles.activeModSelector());
	}

	@Test
	public void testTogglesFileDeclaresItsToggles() {
		Assertions.assertEquals(Set.of("NEW_CHECKOUT", "BETA_BANNER"), toggleFile.toggleNames());
		Assertions.assertEquals("nl/serviceplanet/closuretemplates/toolbox/feature.toggles", toggleFile.importPath());
	}

	@Test
	public void testAllTogglesDisabled() {
		Assertions.assertEquals("Old checkout", render());
	}

	@Test
	public void testSingleToggleEnabled() {
		Assertions.assertEquals("New checkout", render("NEW_CHECKOUT"));
	}

	@Test
	public void testAllTogglesEnabled() {
		Assertions.assertEquals("[beta]New checkout", render("NEW_CHECKOUT", "BETA_BANNER"));
	}

	/**
	 * Soy renders a toggle name it does not know as {@code false}, so a typo would silently produce the wrong output
	 * rather than an error. {@link SoyToggles} rejects it instead.
	 */
	@Test
	public void testEnablingUndeclaredToggleFails() {
		IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
				() -> SoyToggles.builder().declareFrom(toggleFile).enable("NEW_CHEKOUT").build());

		Assertions.assertTrue(e.getMessage().contains("NEW_CHEKOUT"), e.getMessage());
	}

	@Test
	public void testIsEnabled() {
		SoyToggles toggles = SoyToggles.builder()
				.declareFrom(toggleFile)
				.enable("NEW_CHECKOUT")
				.build();

		Assertions.assertTrue(toggles.isEnabled("NEW_CHECKOUT"));
		Assertions.assertFalse(toggles.isEnabled("BETA_BANNER"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> toggles.isEnabled("NO_SUCH_TOGGLE"));
	}

	/**
	 * The Soy compiler takes every line of a toggles file verbatim, so a line which is not a bare toggle name declares
	 * a toggle no template can import. That is rejected while parsing rather than left to fail as an "unknown symbol"
	 * error on the import statement.
	 */
	@Test
	public void testMalformedTogglesFileIsRejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ToggleFile.parse("# a comment\nNEW_UI\n", "some/path/my.toggles"));

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ToggleFile.parse("  NEW_UI\n", "some/path/my.toggles"));
	}

	@Test
	public void testBlankLinesInTogglesFileAreIgnored() {
		ToggleFile parsed = ToggleFile.parse("\nNEW_UI\n\nBETA\n\n", "some/path/my.toggles");

		Assertions.assertEquals(Set.of("NEW_UI", "BETA"), parsed.toggleNames());
	}
}
