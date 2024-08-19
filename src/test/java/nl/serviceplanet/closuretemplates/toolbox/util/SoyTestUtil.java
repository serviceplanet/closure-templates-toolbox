package nl.serviceplanet.closuretemplates.toolbox.util;

import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.PropertiesIcuGenerator;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.SoyUtil;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.TextUtil;

import java.net.URL;
import java.util.Map;

public final class SoyTestUtil {
	/**
	 * Should be executed (manually) when generating message.properties files for the unit-tests.
	 */
	public static void dumpIcuPropertiesFile(String filename) {
		SoyMsgBundleHandler.OutputFileOptions options = new SoyMsgBundleHandler.OutputFileOptions();
		options.setSourceLocaleString("en");
		options.setTargetLocaleString("nl");

		String propertiesFileContent = PropertiesIcuGenerator.extractSoyMsgBundleFromFileSet(
				compileSoyFile(filename).soyFileSet(), options
		).propertiesFileContent();

		// intentionally not using logger
		System.out.println("############################### " + filename);
		System.out.println(propertiesFileContent);
		System.out.println("###############################");
	}

	public static SoyUtil.SoyCompilation compileSoyFile(String filename) {
		URL url = Resources.getResource(getDirectoryOfUnitTestResources() + "/" + filename);
		SoyFileSet fileSet = SoyFileSet.builder().add(url).build();
		return SoyUtil.compileSoyFileSet(fileSet);
	}

	public static SoyMsgBundle loadIcuMsgBundle(String filename) {
		URL url = Resources.getResource(getDirectoryOfUnitTestResources() + "/" + filename);
		return SoyUtil.loadIcuMsgBundle(url);
	}

	public static String render(SoySauce sauce, String templateName, Map<String, ?> data, SoyMsgBundle bundle) {
		return sauce
				.renderTemplate(templateName)
				.setData(data)
				.setMsgBundle(bundle == null ? SoyMsgBundle.EMPTY : bundle)
				.renderHtml()
				.get()
				.toSafeHtml()
				.getSafeHtmlString();
	}

	private static String getDirectoryOfUnitTestResources() {
		return getPackageNameOfUnitTestClasses().replace(".","/");
	}

	private static String getPackageNameOfUnitTestClasses() {
		// parent package of current util-package
		return TextUtil.beforeLast(SoyTestUtil.class.getPackageName(), ".");
	}
}
