package org.janelia.saalfeldlab.n5.metadata.transforms;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;

public class TranslationSpatialTransform extends AbstractLinearSpatialTransform {

	public final double[] translation;

	public transient AffineGet transform;

	public TranslationSpatialTransform( final double[] translation ) {
		super("translation");
		this.translation = translation;
	}

	public AffineGet buildTransform()
	{
		if( translation.length == 2 )
			transform = new Translation2D(translation);
		else if( translation.length == 3 )
			transform = new Translation3D(translation);
		else
			transform = new Translation(translation);

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
