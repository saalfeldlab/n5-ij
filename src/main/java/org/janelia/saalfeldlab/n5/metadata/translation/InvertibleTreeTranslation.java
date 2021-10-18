package org.janelia.saalfeldlab.n5.metadata.translation;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.google.gson.Gson;

public class InvertibleTreeTranslation extends TreeTranslation {

	protected ContainerTranslation invFun;

	public InvertibleTreeTranslation( 
			final ContainerMetadataNode root,
			final Gson gson,
			final String fwd, final String inv) {
		super( root, gson, fwd );
		invFun = new ContainerTranslation( inv, gson );
	}

	public <T> void setTranslatedAttribute(String dataset, String key, T attribute) {
		Optional<ContainerMetadataNode> childOpt = rootTranslated.getNode(dataset);
		if (childOpt.isPresent()) {
			childOpt.get().getAttributes().put(key, gson.toJsonTree(attribute));
			rootOrig = invFun.apply(rootTranslated);
		}
	}

}
