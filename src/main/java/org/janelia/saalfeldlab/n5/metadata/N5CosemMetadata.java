package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;

public class N5CosemMetadata  implements N5Metadata< ImagePlus >
{
	public static final String pixelResolutionKey = "pixelResolution";
	
	private boolean separateChannels = true;
	
	public void setSeparateChannels( final boolean separateChannels )
	{
		this.separateChannels = separateChannels;
	}

	@Override
	public void metadataToN5( final ImagePlus imp, final N5Writer n5, final String dataset) throws IOException
	{
		int nd = 2;
		if( !separateChannels && imp.getNChannels() > 1 ){ nd++; }
		if( imp.getNSlices() > 1 ){ nd++; }
		if( imp.getNFrames() > 1 ){ nd++; }

		String[] axes = new String[ nd ];
		axes[ 0 ] = "x";
		axes[ 1 ] = "y";

		int c = 2;
		if ( !separateChannels && imp.getNChannels() > 1 ){ axes[ c++ ] = "c"; }
		if ( imp.getNSlices() > 1 ){ axes[ c++ ] = "z"; }
		if ( imp.getNFrames() > 1 ){ axes[ c++ ] = "t"; }

		// unit
		String[] units = new String[ nd ];
		Arrays.fill( units, imp.getCalibration().getUnit());

		double[] scale = new double[ nd ];
		double[] translation = new double[ nd ];

		scale[ 0 ] = imp.getCalibration().pixelWidth;
		scale[ 1 ] = imp.getCalibration().pixelHeight;
		if( nd > 2 ){ scale[ 2 ] = imp.getCalibration().pixelDepth; }

		translation[ 0 ] = imp.getCalibration().xOrigin;
		translation[ 1 ] = imp.getCalibration().yOrigin;
		if( nd > 2 ){ translation[ 2 ] = imp.getCalibration().zOrigin; }


		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( imp.getCalibration().getUnit(), scale );
		n5.setAttribute( dataset, pixelResolutionKey, voxdims);

		CosemTransform transformMeta = new CosemTransform( axes, scale, translation, units );
		n5.setAttribute( dataset, CosemTransform.KEY, transformMeta);

	}

	@Override
	public void metadataFromN5( final N5Reader n5, final String dataset, final ImagePlus imp ) throws IOException
	{
		FinalVoxelDimensions voxdims = n5.getAttribute( dataset, pixelResolutionKey, FinalVoxelDimensions.class );

		CosemTransform transform = n5.getAttribute( dataset, CosemTransform.KEY, CosemTransform.class );
		
		if( transform != null )
		{
			int nd = transform.scale.length;

			if( nd > 0 )
			{ 
				imp.getCalibration().pixelWidth = transform.scale[ 0 ];
				imp.getCalibration().setXUnit( transform.units[ 0 ] );
				imp.getCalibration().xOrigin = transform.translate[ 0 ];
			}

			if( nd > 1 )
			{
				imp.getCalibration().pixelHeight = transform.scale[ 1 ];
				imp.getCalibration().setYUnit( transform.units[ 1 ] );
				imp.getCalibration().yOrigin = transform.translate[ 1 ];
			}

			if( nd > 2 )
			{
				imp.getCalibration().pixelDepth = transform.scale[ 2 ];
				imp.getCalibration().setZUnit( transform.units[ 2 ] );
				imp.getCalibration().zOrigin = transform.translate[ 2 ];
			}

			imp.getCalibration().setUnit( transform.units[ 0 ] );

		}
		else if( voxdims != null )
		{

			if( voxdims.numDimensions() > 0 )
				imp.getCalibration().pixelWidth = voxdims.dimension( 0 );

			if( voxdims.numDimensions() > 1 )
				imp.getCalibration().pixelHeight = voxdims.dimension( 1 );

			if( voxdims.numDimensions() > 2 )
				imp.getCalibration().pixelDepth = voxdims.dimension( 2 );

			imp.getCalibration().setUnit( voxdims.unit() );
		}
	}
	
	public static class CosemTransform
	{
		public transient static final String KEY = "transform";
		private String[] axes;
		private double[] scale;
		private double[] translate;
		private String[] units;

		public CosemTransform(){}

		public CosemTransform( final String[] axes, final double[] scale, final double[] translate, final String[] units )
		{
			this.axes = axes;
			this.scale = scale;
			this.translate = translate;
			this.units = units;
		}
	}

}
