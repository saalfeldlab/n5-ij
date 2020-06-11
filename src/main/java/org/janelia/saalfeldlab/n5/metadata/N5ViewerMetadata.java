package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

import ij.ImagePlus;

public class N5ViewerMetadata implements N5Metadata< ImagePlus >
{
	protected static final String nameKey = "name";
	protected static final String scalesKey = "scales";
	protected static final String downsamplingFactorsKey = "downsamplingFactors";
	protected static final String pixelResolutionKey = "pixelResolution";

	public static final String sep = File.separator;

	public N5ViewerMetadata(){}

	public void metadataFromN5( N5Reader n5, String dataset, ImagePlus imp ) throws IOException
	{
		double[] downsamplingFactors = n5.getAttribute( dataset, downsamplingFactorsKey, double[].class );
		FinalVoxelDimensions voxdims = n5.getAttribute( dataset, pixelResolutionKey, FinalVoxelDimensions.class );

		if( downsamplingFactors != null )
		{
			double[] newres = new double[ voxdims.numDimensions() ];
			for( int i = 0; i < voxdims.numDimensions(); i++ )
				newres[ i ] = voxdims.dimension( i ) * downsamplingFactors[ i ];

			voxdims = new FinalVoxelDimensions( voxdims.unit(), newres );
		}

		String name = n5.getAttribute( dataset, nameKey, String.class );
		imp.setTitle( name + " " + dataset );

		if( voxdims.numDimensions() > 0 )
			imp.getCalibration().pixelWidth = voxdims.dimension( 0 );

		if( voxdims.numDimensions() > 1 )
			imp.getCalibration().pixelHeight = voxdims.dimension( 1 );

		if( voxdims.numDimensions() > 2 )
			imp.getCalibration().pixelDepth = voxdims.dimension( 2 );

		imp.getCalibration().setUnit( voxdims.unit() );

		/*
		 * this only makes sense if we're only opening one image
		 * but not if we're combining channels 
		 */
//		imp.setDimensions( 1, imp.getImageStackSize(), 1 );
	}

	public void metadataToN5( ImagePlus imp, N5Writer n5, String dataset ) throws IOException
	{
		double[] pixelResolution = new double[]{
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth
		};
		String unit = imp.getCalibration().getUnit();

		n5.setAttribute( dataset, nameKey, imp.getTitle() );

		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( unit, pixelResolution );
		n5.setAttribute( dataset, pixelResolutionKey, voxdims );
	}

}
