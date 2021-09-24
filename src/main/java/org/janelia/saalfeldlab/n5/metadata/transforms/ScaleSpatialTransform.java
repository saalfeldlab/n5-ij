package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;

public class ScaleSpatialTransform extends AbstractLinearSpatialTransform {

	public final double[] scale;

	public transient AffineGet transform;

	public ScaleSpatialTransform( final double[] scale ) {
		super("scale");
		this.scale = scale;
		buildTransform( scale );
	}

	public ScaleSpatialTransform( final N5Reader n5, final String path ) {
		super("scale");
		this.scale = getParameters(n5);
		buildTransform( scale );
	}

	@Override
	public AffineGet buildTransform( double[] parameters )
	{
		if( parameters.length == 2 )
			transform = new Scale2D(parameters);
		else if( parameters.length == 3 )
			transform = new Scale3D(parameters);
		else
			transform = new Scale(parameters);

		return transform;
	}

	@Override
	public AffineGet getTransform()
	{
		return transform;
	}
	
}
