package nl.serviceplanet.closuretemplates.toolbox.msgbundle.util;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import com.google.template.soy.soytree.AbstractParentSoyNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.TemplateNode;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.PropertiesIcuMsgPlugin;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.VerboseSoyMsgBundle;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SoyUtil {
	public static SoyMsgBundle loadIcuMsgBundle(URL url) {
		PropertiesIcuMsgPlugin icuMsgPlugin = new PropertiesIcuMsgPlugin();
		SoyMsgBundleHandler msgBundleHandler = new SoyMsgBundleHandler(icuMsgPlugin);
		try {
			return new VerboseSoyMsgBundle("url: " + url, msgBundleHandler.createFromResource(url));
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	public record SoyCompilation(SoyFileSet soyFileSet, SoySauce soySauce, String rootTemplateName) {
	}

	public static SoyCompilation compileSoyFileSet(SoyFileSet fileSet) {
		fileSet.resetErrorReporter(); // must be called, otherwise an NPE is thrown in parse()

		return new SoyCompilation(
				fileSet,
				fileSet.compileTemplates(),
				findRootTemplate(gatherTemplateNodes(fileSet.parse().fileSet())).getTemplateName()
		);
	}

	public record TemplateNodeDepth(TemplateNode templateNode, int depth) {
		public String getTemplateName() {
			return templateNode.getTemplateName();
		}
	}

	public static List<TemplateNodeDepth> gatherTemplateNodes(SoyNode node) {
		List<TemplateNodeDepth> templateNodes = new ArrayList<>();
		gatherTemplateNodes(node, 0, templateNodes);
		return templateNodes;
	}

	public static void gatherTemplateNodes(SoyNode node, int depth, List<TemplateNodeDepth> out) {
		if (node instanceof TemplateNode tn) {
			out.add(new TemplateNodeDepth(tn, depth));
		} else if (node instanceof AbstractParentSoyNode<?> parent) {
			for (SoyNode child : parent.getChildren()) {
				gatherTemplateNodes(child, depth + 1, out);
			}
		}
	}

	public static TemplateNode findRootTemplate(List<TemplateNodeDepth> templateNodeDepths) {
		// find the shallowest template node
		return templateNodeDepths.stream()
				.min(Comparator.comparing(TemplateNodeDepth::depth)
						.thenComparing(TemplateNodeDepth::getTemplateName)
				)
				.orElseThrow(() -> new IllegalStateException("failed to find root TemplateNode"))
				.templateNode();
	}
}
