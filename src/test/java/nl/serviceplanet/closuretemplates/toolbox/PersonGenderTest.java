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

public final class PersonGenderTest {
	private static final Logger log = LoggerFactory.getLogger(PersonGenderTest.class);
	private static final String SOY_FILE = "person-gender.soy";
	private static final String ICU_NL_PROP_FILE = "person-gender-NL.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle bundleNL;

	@BeforeAll
	public static void setup() {
		soy = SoyTestUtil.compileSoyFile(SOY_FILE);
		bundleNL = SoyTestUtil.loadIcuMsgBundle(ICU_NL_PROP_FILE);
	}

	@Test
	public void testPersonGenderMale() {
		Map<String, ?> data = Map.of("gender", "male");

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("His name is Chris.", renderedEN);
		Assertions.assertEquals("Zijn naam is Chris.", renderedNL);
	}

	@Test
	public void testPersonGenderFemale() {
		Map<String, ?> data = Map.of("gender", "female");

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("Her name is Chris.", renderedEN);
		Assertions.assertEquals("Haar naam is Chris.", renderedNL);
	}

	@Test
	public void testPersonGenderDefault() {
		Map<String, ?> data = Map.of("gender", "somewhere-within-or-outside-the-spectrum");

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("Their name is Chris.", renderedEN);
		Assertions.assertEquals("Hun naam is Chris.", renderedNL);
	}
}
