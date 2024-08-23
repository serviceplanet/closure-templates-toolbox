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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class TwoPersonGenderTest {
	private static final Logger log = LoggerFactory.getLogger(TwoPersonGenderTest.class);
	private static final String SOY_FILE = "two-person-gender.soy";
	private static final String ICU_NL_PROP_FILE = "two-person-gender-NL.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle bundleNL;

	@BeforeAll
	public static void setup() {
		soy = SoyTestUtil.compileSoyFile(SOY_FILE);
		bundleNL = SoyTestUtil.loadIcuMsgBundle(ICU_NL_PROP_FILE);
	}

	@Test
	public void testPersonGenderMaleMale() {
		Map<String, ?> data = Map.of(
				"sourceGender", "male",
				"targetGender", "male"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("He shared a document with him.", renderedEN);
		Assertions.assertEquals("Hij deelde een document met hem.", renderedNL);
	}

	@Test
	public void testPersonGenderMaleFemale() {
		Map<String, ?> data = Map.of(
				"sourceGender", "male",
				"targetGender", "female"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("He shared a document with her.", renderedEN);
		Assertions.assertEquals("Hij deelde een document met haar.", renderedNL);
	}

	@Test
	public void testPersonGenderFemaleMale() {
		Map<String, ?> data = Map.of(
				"sourceGender", "female",
				"targetGender", "male"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("She shared a document with him.", renderedEN);
		Assertions.assertEquals("Ze deelde een document met hem.", renderedNL);
	}

	@Test
	public void testPersonGenderFemaleFemale() {
		Map<String, ?> data = Map.of(
				"sourceGender", "female",
				"targetGender", "female"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("She shared a document with her.", renderedEN);
		Assertions.assertEquals("Ze deelde een document met haar.", renderedNL);
	}

	@Test
	public void testPersonGenderMaleUnknown() {
		Map<String, ?> data = Map.of(
				"sourceGender", "male",
				"targetGender", "unknown"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("He shared a document with them.", renderedEN);
		Assertions.assertEquals("Hij deelde een document met hen.", renderedNL);
	}

	@Test
	public void testPersonGenderUnknownUnknown() {
		Map<String, ?> data = Map.of(
				"sourceGender", "unknown",
				"targetGender", "unknown"
		);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("They shared a document with them.", renderedEN);
		Assertions.assertEquals("Zij deelden een document met hen.", renderedNL);
	}
}
