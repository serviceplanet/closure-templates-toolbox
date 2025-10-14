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
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.PropertiesIcuGenerator;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.SoyUtil;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.TextUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class SoyTestUtil {
	public static void main(String[] args) throws Exception {
		dumpIcuPropertiesFile("nl/serviceplanet/closuretemplates/toolbox/multi-variable.soy");
	}

	/**
	 * Should be executed (manually) when generating message.properties files for the unit-tests.
	 */
	public static void dumpIcuPropertiesFile(String filename) throws Exception {
		SoyMsgBundleHandler.OutputFileOptions options = new SoyMsgBundleHandler.OutputFileOptions();
		options.setSourceLocaleString("en");
		options.setTargetLocaleString("nl");

		String propertiesFileContent = PropertiesIcuGenerator.extractSoyMsgBundleFromFileSet(
				SoyUtil.compileSoyFile(
						filename
				).soyFileSet(), options
		).propertiesFileContent();

		// intentionally not using logger
		System.out.println("############################### " + filename);
		System.out.println(propertiesFileContent);
		System.out.println("###############################");
	}

	public static void generateMessagePropertiesFromSoyFileInClasspath(SoyUtil.SoyCompilation soy, String propFile) throws IOException {
		SoyMsgBundleHandler.OutputFileOptions options = new SoyMsgBundleHandler.OutputFileOptions();
		options.setSourceLocaleString("en");
		options.setTargetLocaleString("nl");

		Path path = Path.of(SoyTestUtil.getPathToResource(SoyTestUtil.Source.TARGET_TEST_RESOURCES, propFile));
		Files.writeString(path, PropertiesIcuGenerator.extractSoyMsgBundleFromFileSet(soy.soyFileSet(), options).propertiesFileContent());
	}

	public enum Source {
		CLASSPATH, TARGET_TEST_RESOURCES
	}

	public static String getPathToResource(String filename) {
		return getPathToResource(Source.CLASSPATH, filename);
	}

	public static String getPathToResource(Source source, String filename) {
		String path = getDirectoryOfUnitTestResources() + "/" + filename;
		if (source == Source.TARGET_TEST_RESOURCES) {
			path = "target/test-classes/" + path;
		}
		return path;
	}

	public static SoyMsgBundle loadIcuMsgBundle(String filename, String locale) {
		URL url = Resources.getResource(getPathToResource(filename));
		return SoyUtil.loadIcuMsgBundle(url, locale, "unit-test", true);
	}

	public static String render(SoySauce sauce, String templateName, Map<String, ?> data, SoyMsgBundle bundle) {
		return sauce
				.renderTemplate(templateName)
				.setData(data)
				.setMsgBundle(Optional.ofNullable(bundle).orElse(SoyMsgBundle.EMPTY))
				.renderHtml()
				.get()
				.toSafeHtml()
				.getSafeHtmlString();
	}

	public static String getDirectoryOfUnitTestResources() {
		return getPackageNameOfUnitTestClasses().replace(".", "/");
	}

	private static String getPackageNameOfUnitTestClasses() {
		// parent package of current util-package
		return TextUtil.beforeLast(SoyTestUtil.class.getPackageName(), ".");
	}
}
