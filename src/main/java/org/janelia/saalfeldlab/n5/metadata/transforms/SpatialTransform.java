package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;

public interface SpatialTransform {

	public RealTransform getTransform();
}
