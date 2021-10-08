package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class TranslationSpatialTransform extends AbstractLinearSpatialTransform<double[]> {

	public double[] translation;

	public transient AffineGet transform;

	public TranslationSpatialTransform( final double[] translation ) {
		super("translation");
		this.translation = translation;
		buildTransform( translation );
	}

	public TranslationSpatialTransform( final N5Reader n5, final String path ) {
		super("translation", path );
		this.translation = getParameters( n5 );
		buildTransform( translation );
	}

	public TranslationSpatialTransform( final String path ) {
		super("translation", path );
		this.translation = null;
	}

	@Override
	public AffineGet buildTransform( double[] parameters )
	{
		this.translation = parameters;
		if( parameters.length == 2 )
			transform = new Translation2D(parameters);
		else if( parameters.length == 3 )
			transform = new Translation3D(parameters);
		else
			transform = new Translation(parameters);

		return transform;
	}

	@Override
	public AffineGet getTransform() {
		if( transform == null && translation != null )
			buildTransform(translation);

		return transform;
	}

	@Override
	public double[] getParameters(N5Reader n5) {
		return getDoubleArray( n5 , getParameterPath() );
	}

	
}
