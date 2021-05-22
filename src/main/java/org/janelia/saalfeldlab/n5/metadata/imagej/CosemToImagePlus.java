package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;

import ij.ImagePlus;

public class CosemToImagePlus extends PhysicalMetadataToImagePlus<N5CosemMetadata>{
	
	@Override
	public N5CosemMetadata readMetadata( final ImagePlus imp ) throws IOException
	{
		int nd = 2;
		if( imp.getNChannels() > 1 ){ nd++; }
		if( imp.getNSlices() > 1 ){ nd++; }
		if( imp.getNFrames() > 1 ){ nd++; }

		final String[] axes = new String[ nd ];
		if( nd == 2 )
		{
			axes[ 0 ] = "y";
			axes[ 1 ] = "x";
		}
		else if( nd == 3 )
		{
			axes[ 0 ] = "z";
			axes[ 1 ] = "y";
			axes[ 2 ] = "x";
		}

		int c = 2;
		if ( imp.getNChannels() > 1 ){ axes[ c++ ] = "c"; }
		if ( imp.getNSlices() > 1 ){ axes[ c++ ] = "z"; }
		if ( imp.getNFrames() > 1 ){ axes[ c++ ] = "t"; }

		// unit
		final String[] units = new String[ nd ];
		Arrays.fill( units, imp.getCalibration().getUnit());

		final double[] scale = new double[ nd ];
		final double[] translation = new double[ nd ];

		if( nd == 2 )
		{
			scale[ 0 ] = imp.getCalibration().pixelHeight;
			scale[ 1 ] = imp.getCalibration().pixelWidth;

			translation[ 0 ] = imp.getCalibration().yOrigin;
			translation[ 1 ] = imp.getCalibration().xOrigin;
		}
		else if( nd == 3 )
		{
			scale[ 0 ] = imp.getCalibration().pixelDepth;
			scale[ 1 ] = imp.getCalibration().pixelHeight;
			scale[ 2 ] = imp.getCalibration().pixelWidth;

			translation[ 2 ] = imp.getCalibration().zOrigin;
			translation[ 1 ] = imp.getCalibration().yOrigin;
			translation[ 0 ] = imp.getCalibration().xOrigin;
		}

		return new N5CosemMetadata( "", new CosemTransform( axes, scale, translation, units ), null );
	}
}
