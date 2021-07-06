package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;

public class AffineSpatialTransform extends AbstractSpatialTransform{

	public double[] affine;

	public transient AffineGet transform;

	public AffineSpatialTransform( String type, double[] affine ) {
		super(type);
		this.affine = affine;
	}
	
	public AffineGet buildTransform()
	{
		if( affine.length == 6 )
		{
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set( affine );
			transform = tmp;
		}
		else if( affine.length == 12 )
		{
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set( affine );
			transform = tmp;
		}
		else
		{
			AffineTransform tmp = new AffineTransform();
			tmp.set(affine);
			transform = tmp;
		}
		return transform;
	}

	@Override
	public RealTransform getTransform()
	{
		if( transform == null )
			return buildTransform();
		else
			return transform;
	}
	
}
