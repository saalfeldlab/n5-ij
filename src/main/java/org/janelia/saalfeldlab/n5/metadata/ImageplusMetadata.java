package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;

import ij.ImagePlus;

public interface ImageplusMetadata< T extends N5Metadata >
{
	public void writeMetadata( T t, ImagePlus ip ) throws IOException;

	public T readMetadata( ImagePlus ip ) throws IOException;
}
