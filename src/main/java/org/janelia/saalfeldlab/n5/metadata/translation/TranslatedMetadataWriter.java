package org.janelia.saalfeldlab.n5.metadata.translation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataWriter;

import com.google.gson.JsonElement;


public class TranslatedMetadataWriter<T extends N5Metadata> extends TreeTranslation implements N5MetadataWriter<T>{

	private N5Writer n5;
	
	private ContainerMetadataWriter containerWriter;
	
	private final N5MetadataWriter<T> metaWriter; 

	public TranslatedMetadataWriter(
			final N5Writer n5,
			final String dataset,
			final String translation,
			final N5MetadataWriter<T> metaWriter ) {
		super(	ContainerMetadataNode.build(n5, dataset, JqUtils.buildGson(n5) ), 
				JqUtils.buildGson(n5), 
				translation );

		this.n5 = n5;
		this.metaWriter = metaWriter;
		containerWriter = new ContainerMetadataWriter( n5, this.rootTranslated );
	}
	
	public TranslatedMetadataWriter(
			final N5Writer n5,
			final String translation,
			final N5MetadataWriter<T> metaWriter ) {

		this( n5, "", translation, metaWriter );
	}

	@Override
	public void writeMetadata(T t, N5Writer n5, String path) throws Exception {
		this.n5 = n5;
		metaWriter.writeMetadata(t, getOrig(), path);
		updateTranslated();

		containerWriter.setN5Writer(n5);
		containerWriter.setMetadataTree(rootTranslated);
		containerWriter.writeAllAttributes();
	}

}
