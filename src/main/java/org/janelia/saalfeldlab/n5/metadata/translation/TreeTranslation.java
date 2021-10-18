package org.janelia.saalfeldlab.n5.metadata.translation;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.google.gson.Gson;

public class TreeTranslation {

	protected ContainerMetadataNode rootOrig;
	protected ContainerMetadataNode rootTranslated;
	protected ContainerTranslation fwdFun;
	protected Gson gson;

	public TreeTranslation( 
			final ContainerMetadataNode root,
			final Gson gson,
			final String fwd ) {
		this.rootOrig = root;
		this.gson = gson;
		fwdFun = new ContainerTranslation( fwd, gson );
		rootTranslated = fwdFun.apply(rootOrig);
	}

	public ContainerMetadataNode getOrig() {
		return rootOrig;
	}

	public ContainerMetadataNode getTranslated() {
		return rootTranslated;
	}
	
	public void updateTranslated() {
		rootTranslated = fwdFun.apply(rootOrig);
	}

	public <T> void setAttribute( String dataset, String key, T attribute ) {
		Optional<ContainerMetadataNode> childOpt = rootOrig.getNode(dataset);
		if( childOpt.isPresent() ) { 
			childOpt.get().getAttributes().put(key, gson.toJsonTree(attribute));
			updateTranslated();
		}
	}

}
