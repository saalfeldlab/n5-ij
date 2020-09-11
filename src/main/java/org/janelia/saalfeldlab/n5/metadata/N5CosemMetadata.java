package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;

public class N5CosemMetadata implements N5Metadata, 
	N5GsonMetadataParser< N5CosemMetadata >, N5MetadataWriter< N5CosemMetadata >, ImageplusMetadata< N5CosemMetadata >
{
	public static final String pixelResolutionKey = "pixelResolution";

	private boolean separateChannels = true;

	private String path;

	private final CosemTransform cosemTransformMeta;

	private final FinalVoxelDimensions voxDims;

	private final HashMap< String, Class<?>> keysToTypes;

	public N5CosemMetadata()
	{
		this( "", null, null );
	}

	public N5CosemMetadata( final CosemTransform cosemTransformMeta )
	{
		this( "", cosemTransformMeta, null );
	}

	public N5CosemMetadata( final String path, final CosemTransform cosemTransformMeta )
	{
		this( path, cosemTransformMeta, null );
	}

	public N5CosemMetadata( final String path, final FinalVoxelDimensions voxelDimensions )
	{
		this( path, null, voxelDimensions );
	}

	public N5CosemMetadata( final String path, final CosemTransform cosemTransformMeta, final FinalVoxelDimensions voxDims )
	{
		this.path = path;
		this.cosemTransformMeta = cosemTransformMeta;
		this.voxDims = voxDims;

		keysToTypes = new HashMap<>();
		keysToTypes.put( pixelResolutionKey, FinalVoxelDimensions.class );
		keysToTypes.put( CosemTransform.KEY, CosemTransform.class );
	}
	
	public void setSeparateChannels( final boolean separateChannels )
	{
		this.separateChannels = separateChannels;
	}
	
	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	@Override
	public N5CosemMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if( !N5MetadataParser.hasRequiredKeys( keysToTypes(), metaMap ))
			throw new Exception( "Could not parse as N5CosemMetadata.");

		String dataset = ( String ) metaMap.get( "dataset" );
		CosemTransform transform = ( CosemTransform ) metaMap.get( CosemTransform.KEY );
		FinalVoxelDimensions voxdims = ( FinalVoxelDimensions ) metaMap.get( pixelResolutionKey );

		if( transform == null && voxdims == null)
			return null;

		return new N5CosemMetadata( dataset, transform, voxdims );
	}

	@Override
	public void writeMetadata( final N5CosemMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		if( t.cosemTransformMeta != null )
			n5.setAttribute( dataset, CosemTransform.KEY, t.cosemTransformMeta );

		if( t.voxDims != null )
			n5.setAttribute( dataset, pixelResolutionKey, t.voxDims );
	}

	@Override
	public void writeMetadata( final N5CosemMetadata t, final ImagePlus imp ) throws IOException
	{
		CosemTransform transform = t.cosemTransformMeta;
		FinalVoxelDimensions voxdims = t.voxDims;

		if ( transform != null )
		{
			int nd = transform.scale.length;

			if ( nd > 0 )
			{
				imp.getCalibration().pixelWidth = transform.scale[ 0 ];
				imp.getCalibration().setXUnit( transform.units[ 0 ] );
				imp.getCalibration().xOrigin = transform.translate[ 0 ];
			}

			if ( nd > 1 )
			{
				imp.getCalibration().pixelHeight = transform.scale[ 1 ];
				imp.getCalibration().setYUnit( transform.units[ 1 ] );
				imp.getCalibration().yOrigin = transform.translate[ 1 ];
			}

			if ( nd > 2 )
			{
				imp.getCalibration().pixelDepth = transform.scale[ 2 ];
				imp.getCalibration().setZUnit( transform.units[ 2 ] );
				imp.getCalibration().zOrigin = transform.translate[ 2 ];
				imp.setDimensions( 1, imp.getStackSize(), 1 );
			}

			imp.getCalibration().setUnit( transform.units[ 0 ] );

		}
		else if ( voxdims != null )
		{
			if ( voxdims.numDimensions() > 0 )
				imp.getCalibration().pixelWidth = voxdims.dimension( 0 );

			if ( voxdims.numDimensions() > 1 )
				imp.getCalibration().pixelHeight = voxdims.dimension( 1 );

			if ( voxdims.numDimensions() > 2 )
			{
				imp.getCalibration().pixelDepth = voxdims.dimension( 2 );
				imp.setDimensions( 1, imp.getStackSize(), 1 );
			}

			imp.getCalibration().setUnit( voxdims.unit() );
		}
	}

	@Override
	public N5CosemMetadata readMetadata( ImagePlus imp ) throws IOException
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

		return new N5CosemMetadata( "", new CosemTransform( axes, scale, translation, units ), 
				new FinalVoxelDimensions( imp.getCalibration().getUnit(), scale ));
	}

	@Override
	public String getPath()
	{
		return path;
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
