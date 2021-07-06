package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.RealTransform;

public abstract class AbstractSpatialTransform implements SpatialTransform {

	public String type;
	
	public AbstractSpatialTransform( String type ) {
		this.type = type;
	}
	
	public abstract RealTransform getTransform();

}
