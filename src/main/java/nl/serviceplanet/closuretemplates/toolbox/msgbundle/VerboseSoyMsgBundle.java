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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class VerboseSoyMsgBundle extends SoyMsgBundle {
	private final Logger log = LoggerFactory.getLogger(VerboseSoyMsgBundle.class);

	private final String tag;
	private final String origin;
	private final SoyMsgBundle wrapped;
	private final boolean failOnError;
	private final Set<Long> failedMsgLookups;

	public VerboseSoyMsgBundle(SoyMsgBundle wrapped, String tag, @Nullable String origin, boolean failOnError) {
		checkNotNull(tag, "tag");
		checkNotNull(wrapped, "wrapped");

		this.tag = tag;
		this.origin = origin;
		this.wrapped = wrapped;
		this.failOnError = failOnError;
		this.failedMsgLookups = new HashSet<>();
	}

	public String getTag() {
		return tag;
	}

	public String getOrigin() {
		return origin;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	public Set<Long> getFailedMsgLookups() {
		return failedMsgLookups;
	}

	@Override
	public String getLocaleString() {
		return wrapped.getLocaleString();
	}

	@Override
	public SoyMsg getMsg(long id) {
		// How Soy uses the SoyMsgBundle:
		//
		// When the Soy template engine resolves the template, it uses the SoyMsgBundle. For every {msg} it resolves, it
		// calculates a unique ID based on the entire msg-element (attributes and body). This ID is passed to this
		// method of the SoyMsgBundle. The SoyMsgBundle could then provide a SoyMsg that replaces the original body of
		// the {msg} element in the template. This is the only place where we can intercept the call to the bundle,
		// where the lookup is attempted and the ID is revealed. The ID is typically found in an i18n mapping-file. The
		// logged {msg} ID can be used to ensure the mapping-files are correctly defined.

		SoyMsg got = wrapped.getMsg(id);
		if (got == null) {
			String msg = String.format("SoyMsgBundle[%s]::getMsg('%s') lookup failed", tag, id);

			boolean isFirstTime = failedMsgLookups.add(id);

			if (failOnError) {
				throw new IllegalStateException(msg);
			} else if (isFirstTime) {
				log.info(msg);
			}
		}
		return got;
	}

	@Override
	public int getNumMsgs() {
		return wrapped.getNumMsgs();
	}

	@Override
	public Iterator<SoyMsg> iterator() {
		return wrapped.iterator();
	}

	public String toString() {
		return this.getClass().getName() + "[locale=" + wrapped.getLocaleString() + ", failOnError: " + failOnError + ", tag: " + tag + ", origin: " + origin + "]";
	}
}
