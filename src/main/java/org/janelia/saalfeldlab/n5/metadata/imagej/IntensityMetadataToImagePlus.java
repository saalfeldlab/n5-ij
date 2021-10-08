package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;

import org.janelia.saalfeldlab.n5.metadata.IntensityMetadata;

import java.io.IOException;

public abstract class IntensityMetadataToImagePlus<T extends IntensityMetadata> implements ImageplusMetadata<T> {

	@Override
	public void writeMetadata(final T t, final ImagePlus ip) throws IOException {
		ip.setDisplayRange(t.minIntensity(), t.maxIntensity());
	}

	@Override
	public abstract T readMetadata(final ImagePlus ip) throws IOException;

}
