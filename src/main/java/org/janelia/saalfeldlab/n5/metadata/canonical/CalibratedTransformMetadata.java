package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.transforms.CalibratedSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.LinearSpatialTransform;

import net.imglib2.realtransform.AffineGet;

public class CalibratedTransformMetadata implements SpatialMetadata {

	private final String path;
	private final CalibratedSpatialTransform spatialTransform;
	
	public CalibratedTransformMetadata(final String path, final CalibratedSpatialTransform spatialTransform)
	{
		this.path = path;
		this.spatialTransform = spatialTransform;
	}

	@Override
	public String getPath() {
		return path;
	}

	public CalibratedSpatialTransform getTransform() {
		return spatialTransform;
	}

	@Override
	public AffineGet spatialTransform() {
		if( spatialTransform.getTransform() instanceof LinearSpatialTransform ) {
			return ((LinearSpatialTransform) spatialTransform.getTransform()).getTransform();
		}
		else
			return null;
	}

	@Override
	public String unit() {
		return spatialTransform.getUnit();
	}

}
