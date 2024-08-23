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
