package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;

public class DefaultMetadata implements N5Metadata, 
	N5MetadataParser< DefaultMetadata >, N5MetadataWriter< DefaultMetadata >, ImageplusMetadata< DefaultMetadata >
{
	private String path;

	private final FinalVoxelDimensions voxDims;
	
	public DefaultMetadata( int nd )
	{
		this( "", nd );
	}

	public DefaultMetadata( final String path, final int nd )
	{
		this.path = path;
		voxDims = new FinalVoxelDimensions( "pixel", 
			DoubleStream.iterate( 1, x -> x ).limit( nd ).toArray());
	}
	
	@Override
	public DefaultMetadata parseMetadata( N5Reader n5, N5TreeNode node ) throws Exception
	{
		return new DefaultMetadata( node.path, 
				n5.getDatasetAttributes( node.path ).getNumDimensions() );
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

	@Override
	public String getPath()
	{
		return path;
	}

}
