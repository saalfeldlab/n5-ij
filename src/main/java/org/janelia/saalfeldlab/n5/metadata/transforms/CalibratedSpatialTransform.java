package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;

public class CalibratedSpatialTransform implements SpatialTransform {

	private final AffineSpatialTransform transform;
	private final String unit;
	
	public CalibratedSpatialTransform( AffineSpatialTransform transform, String unit)
	{
		this.transform = transform;
		this.unit = unit;
	}

	@Override
	public RealTransform getTransform() {

		return transform.getTransform();
	}
	
	public String getUnit()
	{
		return unit;
	}

}
