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
package nl.serviceplanet.closuretemplates.toolbox.msgbundle;

import com.google.common.io.CharSource;
import com.google.errorprone.annotations.Immutable;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import com.google.template.soy.msgs.SoyMsgPlugin;

import java.io.IOException;

@Immutable
public final class PropertiesIcuMsgPlugin implements SoyMsgPlugin {

	@Override
	public CharSequence generateExtractedMsgsFile(SoyMsgBundle msgBundle,
												  SoyMsgBundleHandler.OutputFileOptions options,
												  ErrorReporter errorReporter) {
		return PropertiesIcuGenerator.generateProperties(msgBundle);
	}

	@Override
	public SoyMsgBundle parseTranslatedMsgsFile(CharSource charSource) throws IOException {
		return PropertiesIcuParser.parseIcuMsgsPropertiesFile(charSource.read());
	}
}
