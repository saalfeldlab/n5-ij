package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

public interface N5Metadata< T >
{
	public void metadataToN5( T t, N5Writer n5, String dataset ) throws IOException;

	public void metadataFromN5( N5Reader n5, String dataset, T t ) throws IOException;

}
