package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

public interface PhysicalMetadata
{
	public AffineGet physicalTransform();
	
	public String[] units();

	public default AffineTransform3D physicalTransform3d()
	{
		final AffineGet transform = physicalTransform();

		// return identity if null
		if( transform == null )
			return new AffineTransform3D();
		else if( transform instanceof AffineTransform3D )
			return ( AffineTransform3D ) transform;
		else
		{
			final int N = transform.numSourceDimensions() < 3 ? transform.numSourceDimensions() : 3;
			final double[] params = new double[ 12 ];
			int k = 0;
			for( int i = 0; i < N; i++ )
				for( int j = 0; j < N+1; j++ )
					params[ k++ ] = transform.get( i, j );

			final AffineTransform3D transform3d = new AffineTransform3D();
			transform3d.set( params );
			
			return transform3d;
		}
	}

}
