package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

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

	public AffineSpatialTransform( final N5Reader n5, final String path ) {
		super("affine");
		this.affine = getParameters(n5);
		buildTransform( affine );
	}

	@Override
	public AffineGet buildTransform( double[] parameters ) {
		if( parameters.length == 6 ) {
			AffineTransform2D tmp = new AffineTransform2D();
			tmp.set( parameters );
			transform = tmp;
		}
		else if( parameters.length == 12 ) {
			AffineTransform3D tmp = new AffineTransform3D();
			tmp.set( parameters );
			transform = tmp;
		}
		else {
			int nd = (int)Math.floor( Math.sqrt( parameters.length ));
			AffineTransform tmp = new AffineTransform( nd );
			tmp.set(parameters);
			transform = tmp;
		}
		return transform;
	}

	@Override
	public AffineGet getTransform() {
		return transform;
	}
	
}
