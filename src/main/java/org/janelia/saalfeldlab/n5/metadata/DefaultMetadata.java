package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5Writer;

import com.google.gson.JsonElement;

import ij.ImagePlus;

public class DefaultMetadata extends AbstractN5Metadata implements N5GsonMetadataParser< DefaultMetadata >, 
	N5MetadataWriter< DefaultMetadata >, ImageplusMetadata< DefaultMetadata >
{
	private final FinalVoxelDimensions voxDims;

	private HashMap<String,Class<?>> keysToTypes;

	public static final String dimensionsKey = "dimensions";

	public DefaultMetadata( int nd )
	{
		this( "", nd );
	}

	public DefaultMetadata( final String path, final DatasetAttributes attributes )
	{
		super( path, attributes );
		int nd = attributes.getNumDimensions();
		if( nd > 0 )
		{
			voxDims = new FinalVoxelDimensions( "pixel", 
				DoubleStream.iterate( 1, x -> x ).limit( nd ).toArray());
		}
		else
			voxDims = null;

		keysToTypes = new HashMap<>();
		keysToTypes.put( dimensionsKey, long[].class ); // n5 datasets need this
	}

	public DefaultMetadata( final String path, final int nd )
	{
		super( path, null );
		if( nd > 0 )
		{
			voxDims = new FinalVoxelDimensions( "pixel", 
				DoubleStream.iterate( 1, x -> x ).limit( nd ).toArray());
		}
		else
			voxDims = null;

		keysToTypes = new HashMap<>();
		keysToTypes.put( dimensionsKey, long[].class ); // n5 datasets need this
	}

	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	@Override
	public < R extends AbstractGsonReader> DefaultMetadata parseMetadataGson( final R n5, final String dataset, final HashMap< String, JsonElement > map ) throws Exception
	{
		final long[] dimensions = GsonAttributesParser.parseAttribute(map, "dimensions", long[].class, n5.getGson());
		return new DefaultMetadata( dataset, dimensions.length );
	}

	@Override
	public DefaultMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		String dataset = ( String ) metaMap.get( "dataset" );
		long[] dims = ( long[] ) metaMap.get( dimensionsKey );
		DatasetAttributes attributes = ( DatasetAttributes ) metaMap.get( "attributes" );
		if( attributes == null )
			return new DefaultMetadata( dataset, dims.length );
		else
			return new DefaultMetadata( dataset, attributes );
	}

	@Override
	public void writeMetadata( DefaultMetadata t, N5Writer n5, String dataset ) throws Exception
	{
		// does nothing
	}

	@Override
	public void writeMetadata( DefaultMetadata t, ImagePlus imp ) throws IOException
	{
		FinalVoxelDimensions voxdims = t.voxDims;
		if ( voxdims.numDimensions() > 0 )
			imp.getCalibration().pixelWidth = voxdims.dimension( 0 );

		if ( voxdims.numDimensions() > 1 )
			imp.getCalibration().pixelHeight = voxdims.dimension( 1 );

		if ( voxdims.numDimensions() > 2 )
			imp.getCalibration().pixelDepth = voxdims.dimension( 2 );

		imp.getCalibration().setUnit( voxdims.unit() );
	}

	@Override
	public DefaultMetadata readMetadata( ImagePlus imp ) throws IOException
	{
		int nd = 2;
		if( imp.getNSlices() > 1 ){ nd++; }
		if( imp.getNFrames() > 1 ){ nd++; }

		return new DefaultMetadata( "", nd );
	}

}
