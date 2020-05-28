package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.bdv.N5ExportMetadataReader;
import org.janelia.saalfeldlab.n5.bdv.N5ExportMetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

import ij.ImagePlus;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;

public class N5ViewerMetadata implements N5Metadata< ImagePlus >
{
	public static final String sep = File.separator;

	public N5ViewerMetadata(){}

	public void metadataFromN5( N5Reader n5, String dataset, ImagePlus imp ) throws IOException
	{
		N5ExportMetadataReader meta = new N5ExportMetadataReader( n5 );

		// parse channel from dataset according to n5 viewer convention
		int channel = datasetToChannel( dataset );

		String name = meta.getName();
		imp.setTitle( name + " " + dataset );

		VoxelDimensions voxdims = meta.getPixelResolution( channel );
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

	private int datasetToChannel( final String dataset )
	{
		int channel = -1;
		Pattern pattern = Pattern.compile("c\\d+");
		for( String x : dataset.split( sep ))
			if( pattern.matcher( x ).matches() )
			{
				channel = Integer.parseInt( x.substring(1) );
				break;
			}

		return channel;
	}

	public void metadataToN5( ImagePlus imp, N5Writer n5, String dataset ) throws IOException
	{
		double[] pixelResolution = new double[]{
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth
		};
		String unit = imp.getCalibration().getUnit();

		N5ExportMetadataWriter meta = new N5ExportMetadataWriter( n5 );
		meta.setName( imp.getTitle() );

		FinalVoxelDimensions voxdims = new FinalVoxelDimensions(unit, pixelResolution );
		meta.setPixelResolution( datasetToChannel( dataset ), voxdims );
	}

}
