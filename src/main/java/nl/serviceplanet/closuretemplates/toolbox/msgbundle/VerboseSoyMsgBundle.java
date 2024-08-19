package nl.serviceplanet.closuretemplates.toolbox.msgbundle;

import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.restricted.SoyMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class VerboseSoyMsgBundle extends SoyMsgBundle {
	private final Logger log = LoggerFactory.getLogger(VerboseSoyMsgBundle.class);

	private final String tag;
	private final SoyMsgBundle wrapped;
	private final Set<Long> failedMsgLookups;

	public VerboseSoyMsgBundle(String tag, SoyMsgBundle wrapped) {
		checkNotNull(tag, "tag");
		checkNotNull(wrapped, "wrapped");

		this.tag = tag;
		this.wrapped = wrapped;
		this.failedMsgLookups = new HashSet<>();
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
			log.info("SoyMsgBundle[{}]::getMsg({}) lookup failed", tag, id);
			failedMsgLookups.add(id);
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
}
