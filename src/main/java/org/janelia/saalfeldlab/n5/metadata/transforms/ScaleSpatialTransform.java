package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;

public class ScaleSpatialTransform extends AbstractLinearSpatialTransform {

	public final double[] scale;

	public transient AffineGet transform;

	public ScaleSpatialTransform( final double[] scale ) {
		super("scale");
		this.scale = scale;
	}
	
	public AffineGet buildTransform()
	{
		if( scale.length == 2 )
			transform = new Scale2D(scale);
		else if( scale.length == 3 )
			transform = new Scale3D(scale);
		else
			transform = new Scale(scale);

		return transform;
	}

	@Override
	public AffineGet getTransform()
	{
		if( transform == null )
			return buildTransform();
		else
			return transform;
	}
	
}
