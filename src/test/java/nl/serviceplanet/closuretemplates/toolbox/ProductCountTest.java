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

public final class ProductCountTest {
	private static final Logger log = LoggerFactory.getLogger(ProductCountTest.class);
	private static final String SOY_FILE = "product-count.soy";
	private static final String ICU_NL_PROP_FILE = "product-count-NL.properties";

	private static SoyUtil.SoyCompilation soy;
	private static SoyMsgBundle bundleNL;

	@BeforeAll
	public static void setup() {
		soy = SoyTestUtil.compileSoyFile(SOY_FILE);
		bundleNL = SoyTestUtil.loadIcuMsgBundle(ICU_NL_PROP_FILE);
	}

	@Test
	public void testProductCountWithoutProduct() {
		Map<String, ?> data = Map.of("productCount", 0);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("You will own nothing and be happy.", renderedEN);
		Assertions.assertEquals("Je zult niks bezitten en tevreden zijn.", renderedNL);
	}

	@Test
	public void testProductCountWithOneProduct() {
		Map<String, ?> data = Map.of("productCount", 1);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("You have one product.", renderedEN);
		Assertions.assertEquals("Je hebt een product.", renderedNL);
	}

	@Test
	public void testProductCountWithMultipleProducts() {
		Map<String, ?> data = Map.of("productCount", 13);

		String renderedEN = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, null);
		String renderedNL = SoyTestUtil.render(soy.soySauce(), soy.rootTemplateName(), data, bundleNL);

		Assertions.assertEquals("You have 13 products.", renderedEN);
		Assertions.assertEquals("Je hebt 13 producten.", renderedNL);
	}
}
