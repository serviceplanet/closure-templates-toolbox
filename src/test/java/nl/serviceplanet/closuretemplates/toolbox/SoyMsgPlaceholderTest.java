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

import com.google.template.soy.msgs.SoyMsgBundle;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.SoyUtil;
import nl.serviceplanet.closuretemplates.toolbox.util.SoyTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public final class SoyMsgPlaceholderTest {
	private static final String SOY_FILE = "soy-msg-placeholder.soy";
	private static final String GENERATED_PROP_FILE = "soy-msg-placeholder.properties";
	private static final String TRANSLATED_PROP_FILE = "soy-msg-placeholder_en_US.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle generatedBundle;
	private static SoyMsgBundle translatedBundle;

	@BeforeAll
	public static void setup() throws Exception {
		soy = SoyUtil.compileSoyFile(SoyTestUtil.getPathToResource(SOY_FILE));

		// write both to the source-root (for editor/git) and the classpath (for ICU/Soy that loads from the classpath)
		String inputPath = "src/test/resources/";
		List<String> outputPaths = List.of("src/test/resources/", "target/test-classes/");

		String soyBasePath = inputPath + SoyTestUtil.getDirectoryOfUnitTestResources() + "/" + SOY_FILE;

		generatedBundle = SoyTestUtil.loadIcuMsgBundle(GENERATED_PROP_FILE, "nl-NL");
		translatedBundle = SoyTestUtil.loadIcuMsgBundle(TRANSLATED_PROP_FILE, "en-US");

		Assertions.assertEquals("nl-NL", generatedBundle.getLocale().toLanguageTag());
		Assertions.assertEquals("en-US", translatedBundle.getLocale().toLanguageTag());
	}

	@Test
	public void testTrickyMsgBlocks() {
		Map<String, ?> data = createTemplateData();

		Assertions.assertEquals(
				"Vendor: 4444--4444a/b5555-5555c/dXX=XX--Quantity: 2222--Model: 5555--Name: 8888/YY--2222 assets--Quantity: 2222 of 8888--Quantity2: 2222 of 8888",
				SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null)
		);
	}

	@Test
	public void testGeneratedMsgBundleMatchesOriginalTemplate() {
		Map<String, ?> data = createTemplateData();

		Assertions.assertEquals(
				SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null),
				SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, generatedBundle)
		);
	}

	@Test
	public void testTranslatedMsgBundleMatchesOriginalTemplate() {
		Map<String, ?> data = createTemplateData();

		Assertions.assertEquals(
				SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null),
				SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, translatedBundle)
		);
	}


	private Map<String, ?> createTemplateData() {
		return Map.of(
				"assetVendor", "4444",
				"assetObject", Map.of(
						"model", "5555", // yields "{MODEL_1}"
						"name", "8888",
						"quantity", 2222
				),
				"otherObject", Map.of(
						"model", "XX", // yields "{MODEL_2}"
						"name", "YY"
				)
		);
	}
}
