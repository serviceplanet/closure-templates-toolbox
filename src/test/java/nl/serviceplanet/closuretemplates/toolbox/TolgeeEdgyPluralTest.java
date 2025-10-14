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

import java.util.Map;

public final class TolgeeEdgyPluralTest {
	private static final String SOY_FILE = "soy-tolgee-edgy-plural.soy";
	private static final String PROP_FILE_LOCALE_CS_CZ = "soy-tolgee-edgy-plural_cs-CZ.properties";
	private static final String PROP_FILE_LOCALE_HR_HR = "soy-tolgee-edgy-plural_hr-HR.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle translatedBundleCsCz;
	private static SoyMsgBundle translatedBundleHrHr;

	@BeforeAll
	public static void setup() throws Exception {
		soy = SoyUtil.compileSoyFile(SoyTestUtil.getPathToResource(SOY_FILE));

		translatedBundleCsCz = SoyTestUtil.loadIcuMsgBundle(PROP_FILE_LOCALE_CS_CZ, "cs-CZ");
		translatedBundleHrHr = SoyTestUtil.loadIcuMsgBundle(PROP_FILE_LOCALE_HR_HR, "hr-HR");

		Assertions.assertEquals("cs-CZ", translatedBundleCsCz.getLocale().toLanguageTag());
		Assertions.assertEquals("hr-HR", translatedBundleHrHr.getLocale().toLanguageTag());
	}

	@Test
	public void testCzechPlural() {
		// https://www.unicode.org/cldr/charts/47/supplemental/language_plural_rules.html#cs

		/*
			one		1	1 den	i = 1 and  v = 0
			few		2~4	2 dny	i = 2..4 and  v = 0
			many	0.0~1.5, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, …	1,5 dne	v != 0
			other	0, 5~19, 100, 1000, 10000, 100000, 1000000, …	5 dní
		 */

		Map.of(
				0, "0 dní", // other
				1, "1 den", // one
				2, "2 dny", // few
				3, "3 dny", // few
				4, "4 dny", // few
				5, "5 dní" // other
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleCsCz)));

		Map.of(
				19, "19 dní", // other
				20, "20 dní", // other
				21, "21 dní", // other
				99, "99 dní", // other
				100, "100 dní", // other
				101, "101 dní" // other
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleCsCz)));

		Map.of(
				99.0, "99 dní", // other
				99.1, "99.1 dne", // many
				99.9, "99.9 dne", // many
				100.0, "100 dní", // other
				100.1, "100.1 dne", // many
				100.9, "100.9 dne", // many
				101.0, "101 dní", // other
				101.1, "101.1 dne", // many
				101.9, "101.9 dne" // many
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleCsCz)));

		Map.of(
				0.003, "0.003 dne", // many
				1.003, "1.003 dne", // many
				2.003, "2.003 dne", // many
				3.003, "3.003 dne", // many
				4.003, "4.003 dne", // many
				5.003, "5.003 dne" // many
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleCsCz)));
	}

	@Test
	public void testCroatianPlural() {
		// https://www.unicode.org/cldr/charts/47/supplemental/language_plural_rules.html#cs

		/*
			one		1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, …
					0.1, 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 10.1, 100.1, 1000.1, …	1 sat

			few		2~4, 22~24, 32~34, 42~44, 52~54, 62, 102, 1002, …
					0.2~0.4, 1.2~1.4, 2.2~2.4, 3.2~3.4, 4.2~4.4, 5.2, 10.2, 100.2, 1000.2, …	2 sata

			other	0, 5~19, 100, 1000, 10000, 100000, 1000000, …
					0.0, 0.5~1.0, 1.5~2.0, 2.5~2.7, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, …	5 sati
					0,5 sati
		 */

		Map.of(
				0, "0 sati", // other
				1, "1 sat", // one
				2, "2 sata", // few
				3, "3 sata", // few
				4, "4 sata", // few
				5, "5 sati" // other
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));

		Map.of(
				3, "3 sata", // few
				13, "13 sati", // other -- exception for [abs(n)%100] = [12..14]
				-13, "-13 sati", // other -- exception for [abs(n)%100] = [12..14]
				23, "23 sata", // few
				33, "33 sata", // few
				713, "713 sati", // other -- exception for [abs(n)%100] = [12..14]
				-713, "-713 sati", // other -- exception for [abs(n)%100] = [12..14]
				723, "723 sata", // few
				-723, "-723 sata" // few
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));

		Map.of(
				19, "19 sati", // other
				20, "20 sati", // other
				21, "21 sat", // one
				22, "22 sata", // few
				23, "23 sata", // few
				24, "24 sata", // few
				25, "25 sati", // other
				29, "29 sati", // other
				30, "30 sati", // other
				31, "31 sat" // one
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));

		Map.of(
				27.0, "27 sati", // other
				27.1, "27.1 sat", // one
				27.2, "27.2 sata", // few
				27.3, "27.3 sata", // few
				27.4, "27.4 sata", // few
				27.5, "27.5 sati", // other
				27.6, "27.6 sati", // other
				27.7, "27.7 sati", // other
				27.8, "27.8 sati", // other
				27.9, "27.9 sati" // other
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));

		Map.of(
				27.000, "27 sati", // other
				27.001, "27.001 sat", // one
				27.002, "27.002 sata", // few
				27.003, "27.003 sata", // few
				27.004, "27.004 sata", // few
				27.005, "27.005 sati", // other
				27.006, "27.006 sati", // other
				27.007, "27.007 sati", // other
				27.008, "27.008 sati", // other
				27.009, "27.009 sati" // other
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));

		Map.of(
				0.003, "0.003 sata", // few
				1.003, "1.003 sati", // other <-- why?! even google-translate disagrees - bug in Soy/ICU-impl
				2.003, "2.003 sata", // few
				3.003, "3.003 sata", // few
				4.003, "4.003 sata", // few
				5.003, "5.003 sata" // few
		).forEach((num, txt) -> Assertions.assertEquals(txt, renderTemplate(num, translatedBundleHrHr)));
	}

	private String renderTemplate(double someNumber, SoyMsgBundle soyMsgBundle) {
		return SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), createTemplateData(someNumber), soyMsgBundle);
	}

	private Map<String, ?> createTemplateData(double someNumber) {
		return Map.of("someNumber", someNumber);
	}
}
