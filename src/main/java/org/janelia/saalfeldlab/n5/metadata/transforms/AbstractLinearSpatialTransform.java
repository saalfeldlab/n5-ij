package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;

public abstract class AbstractLinearSpatialTransform implements LinearSpatialTransform {

	public String type;
	
	public AbstractLinearSpatialTransform( String type ) {
		this.type = type;
	}
	
	public abstract AffineGet getTransform();

}
