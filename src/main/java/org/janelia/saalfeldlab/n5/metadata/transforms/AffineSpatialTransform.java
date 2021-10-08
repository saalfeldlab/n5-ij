package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

public class AffineSpatialTransform extends AbstractLinearSpatialTransform<double[]> {

	public double[] affine;

	public transient AffineGet transform;

	public AffineSpatialTransform( final double[] affine ) {
		super("affine");
		this.affine = affine;
		buildTransform( affine );
	}

	public AffineSpatialTransform( final N5Reader n5, final String path ) {
		super("affine");
		this.affine = getParameters(n5);
		buildTransform( affine );
	}

	public AffineSpatialTransform( final String path ) {
		super("affine", path );
		this.affine = null;
	}

	@Override
	public AffineGet buildTransform( double[] parameters ) {
		this.affine = parameters;
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
		if( affine != null && transform == null )
			buildTransform(affine);
		return transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {
		return getDoubleArray( n5 , getParameterPath() );
	}
	
}
