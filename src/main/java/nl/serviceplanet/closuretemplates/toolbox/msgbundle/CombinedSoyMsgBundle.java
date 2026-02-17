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

import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.restricted.SoyMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class CombinedSoyMsgBundle extends SoyMsgBundle {
	
	private static final Logger log = LoggerFactory.getLogger(CombinedSoyMsgBundle.class);
	private final String localeString;
	private final Map<Long, SoyMsg> id2msg;

	public CombinedSoyMsgBundle(List<SoyMsgBundle> bundles) {
		checkNotNull(bundles, "bundles must not be null");
		checkArgument(!bundles.isEmpty(), "bundles must not be empty");

		Set<String> distinctLocales = bundles.stream().map(SoyMsgBundle::getLocaleString).collect(Collectors.toSet());
		if (distinctLocales.size() != 1) {
			throw new IllegalStateException("All bundles must have the same locale, found: " + distinctLocales);
		}
		this.localeString = distinctLocales.iterator().next();

		// N.B.: Ignore the `SoyMsgBundle` instances, we do not delegate calls to them.
		// We merely extract all messages from the provided bundles, and provide access to it.
		id2msg = new HashMap<>();
		bundles.forEach(bundle -> bundle.forEach(msg -> {
			if (id2msg.put(msg.getId(), msg) != null) {
				log.debug("Found msg.id '{}' in multiple bundles.", msg.getId());
			}
		}));
	}

	@Override
	public String getLocaleString() {
		return localeString;
	}

	@Override
	public SoyMsg getMsg(long id) {
		return id2msg.get(id);
	}

	@Override
	public int getNumMsgs() {
		return id2msg.size();
	}

	@Override
	public Iterator<SoyMsg> iterator() {
		return id2msg.values().iterator();
	}

	public String toString() {
		return this.getClass().getName() + "[locale=" + this.getLocaleString() + "]";
	}
}
