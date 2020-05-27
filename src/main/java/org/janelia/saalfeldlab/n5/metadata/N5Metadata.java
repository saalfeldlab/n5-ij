package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

public interface N5Metadata< T >
{
	public void writeMetadata( N5Writer n5, String dataset, T t ) throws IOException;

	public void readMetadata( N5Reader n5, String dataset, T t ) throws IOException;

}
