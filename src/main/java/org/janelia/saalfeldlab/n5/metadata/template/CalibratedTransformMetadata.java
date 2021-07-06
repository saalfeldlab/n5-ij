package org.janelia.saalfeldlab.n5.metadata.template;

import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.CalibratedSpatialTransform;

import net.imglib2.realtransform.AffineGet;

public class CalibratedTransformMetadata extends CalibratedSpatialTransform implements N5Metadata, SpatialMetadata {

	public String path;

	public CalibratedTransformMetadata(AffineSpatialTransform transform, String unit, String path) {

		super(transform, unit);
		this.path = path;
	}

	@Override
	public String getPath() {

		return path;
	}

	@Override
	public AffineGet spatialTransform() {

		return (AffineGet)getTransform();
	}

	@Override
	public String unit() {

		return getUnit();
	}
}
