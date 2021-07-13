package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;

public class SequenceSpatialTransform extends AbstractSpatialTransform {

	private final SpatialTransform[] transformations;

	public SequenceSpatialTransform(final SpatialTransform[] transformations) {
		super("sequence");
		this.transformations = transformations;
	}

	@Override
	public RealTransform getTransform()
	{
		RealTransformSequence transform = new RealTransformSequence();
		for( SpatialTransform t : getTransformations() )
			transform.add( t.getTransform() );

		return transform;
	}

	public SpatialTransform[] getTransformations() {
		return transformations;
	}

}
