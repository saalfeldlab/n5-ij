package org.janelia.saalfeldlab.n5.metadata.translation;

import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.google.gson.Gson;

public class ContainerTranslation extends JqFunction<ContainerMetadataNode,ContainerMetadataNode> {

	public ContainerTranslation(String translation, Gson gson ) {
		super(translation, gson, ContainerMetadataNode.class );
	}

}
