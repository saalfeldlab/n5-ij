package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;

public class CalibratedSpatialTransform {

	private final LinearSpatialTransform transform;
	private final String unit;
	
	public CalibratedSpatialTransform( final LinearSpatialTransform transform, final String unit) {
		this.transform = transform;
		this.unit = unit;
	}

	public LinearSpatialTransform getSpatialTransform() {

		return transform;
	}

	public AffineGet getTransform() {

		return transform.getTransform();
	}
	
	public String getUnit() {
		return unit;
	}

}
