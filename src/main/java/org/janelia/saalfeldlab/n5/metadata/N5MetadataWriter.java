package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.N5Writer;

public interface N5MetadataWriter< T extends N5Metadata >
{
	public void writeMetadata( T t, N5Writer n5, String dataset ) throws Exception;
}
