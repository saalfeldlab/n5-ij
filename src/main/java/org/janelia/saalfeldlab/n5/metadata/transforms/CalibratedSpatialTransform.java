package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;

public class CalibratedSpatialTransform {

	private final SpatialTransform transform;
	private final String unit;
	
	public CalibratedSpatialTransform( final SpatialTransform transform, final String unit) {
		this.transform = transform;
		this.unit = unit;
	}

	public SpatialTransform getSpatialTransform() {

		return transform;
	}

	public RealTransform getTransform() {

		return transform.getTransform();
	}
	
	public String getUnit() {
		return unit;
	}

}
