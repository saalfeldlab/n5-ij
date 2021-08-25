package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class AffineSpatialTransform extends AbstractLinearSpatialTransform {

	public final double[] affine;

	public transient AffineGet transform;

	public AffineSpatialTransform( final double[] affine ) {
		super("affine");
		this.affine = affine;
	}
	
	public AffineGet buildTransform() {
		if( affine.length == 6 ) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set( affine );
			transform = tmp;
		}
		else if( affine.length == 12 ) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set( affine );
			transform = tmp;
		}
		else {
			int nd = (int)Math.floor( Math.sqrt( affine.length ));
			AffineTransform tmp = new AffineTransform( nd );
			tmp.set(affine);
			transform = tmp;
		}
		return transform;
	}

	@Override
	public AffineGet getTransform() {
		if( transform == null )
			return buildTransform();
		else
			return transform;
	}
	
}
