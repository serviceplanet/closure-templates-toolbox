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

public final class MultiVarTest {
	private static final Logger log = LoggerFactory.getLogger(MultiVarTest.class);
	private static final String SOY_FILE = "multi-variable.soy";
	private static final String ICU_NL_PROP_FILE = "multi-variable-NL.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle bundleNL;

	@BeforeAll
	public static void setup() throws Exception {
		soy = SoyUtil.compileSoyFile(SoyTestUtil.getPathToResource(SOY_FILE));
		SoyTestUtil.generateMessagePropertiesFromSoyFileInClasspath(soy, ICU_NL_PROP_FILE);
		bundleNL = SoyTestUtil.loadIcuMsgBundle(ICU_NL_PROP_FILE, "nl_NL");
	}

	@Test
	public void testOneVariable() {
		Map<String, ?> data = Map.of(
				"varOne", "A");

		String renderedEN = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl1", data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl1", data, bundleNL);

		Assertions.assertEquals("VarOne=A, VarOneAgain=A", renderedEN);
		Assertions.assertEquals("VarOne=A, VarOneAgain=A", renderedNL);
	}

	@Test
	public void testTwoVariables() {
		Map<String, ?> data = Map.of(
				"varOne", "A",
				"varTwo", "B");

		String renderedEN = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl2", data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl2", data, bundleNL);

		Assertions.assertEquals("VarOne=A, VarTwo=B, VarOneAgain=A", renderedEN);
		Assertions.assertEquals("VarOne=A, VarTwo=B, VarOneAgain=A", renderedNL);
	}

	@Test
	public void testTwoReferencedVariables() {
		Map<String, ?> data = Map.of(
				"objOne", Map.of("id", "X"),
				"objTwo", Map.of("id", "Y"));

		String renderedEN = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl3", data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl3", data, bundleNL);

		Assertions.assertEquals("ObjOne=X, ObjTwo=Y, ObjOneAgain=X", renderedEN);
		Assertions.assertEquals("ObjOne=X, ObjTwo=Y, ObjOneAgain=X", renderedNL);
	}

	@Test
	public void testTwoReferencedVariablesReordered() {
		// ObjTwo={ID_1}, ObjOne={ID_2}, ObjTwoAgain={ID_1}
		Map<String, ?> data = Map.of(
				"objOne", Map.of("id", "X"),
				"objTwo", Map.of("id", "Y"));

		String renderedEN = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl4", data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl4", data, bundleNL);

		Assertions.assertEquals("ObjTwo=Y, ObjOne=X, ObjTwoAgain=Y", renderedEN);
		Assertions.assertEquals("ObjTwo=Y, ObjOne=X, ObjTwoAgain=Y", renderedNL);
	}


	@Test
	public void testTwoReferencedVariablesAndTwoRegularVariables() {
		// ObjTwo={ID_1}, ObjOne={ID_2}, ObjTwoAgain={ID_1}, VarOne={VAR_ONE}, VarTwo={VAR_TWO}, VarOneAgain={VAR_ONE}
		Map<String, ?> data = Map.of(
				"varOne", "A",
				"varTwo", "B",
				"objOne", Map.of("id", "X"),
				"objTwo", Map.of("id", "Y"));

		String renderedEN = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl5", data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), "nl.serviceplanet.tmpl5", data, bundleNL);

		Assertions.assertEquals("ObjTwo=Y, ObjOne=X, ObjTwoAgain=Y, VarOne=A, VarTwo=B, VarOneAgain=A", renderedEN);
		Assertions.assertEquals("ObjTwo=Y, ObjOne=X, ObjTwoAgain=Y, VarOne=A, VarTwo=B, VarOneAgain=A", renderedNL);
	}
}
