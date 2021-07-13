package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class IdentitySpatialTransform extends AbstractSpatialTransform {

	public IdentitySpatialTransform() {
		super("identity");
	}

	@Override
	public RealTransform getTransform()
	{
		// an empty RealTransformSequence is the identity
		return new RealTransformSequence();
	}
	
}
