/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale;
import net.imglib2.realtransform.Scale2D;
import net.imglib2.realtransform.Scale3D;

public class DefaultMetadata extends AbstractN5Metadata<DefaultMetadata> implements ImageplusMetadata< DefaultMetadata >,
	PhysicalMetadata
{
	private final FinalVoxelDimensions voxDims;

	private HashMap<String,Class<?>> keysToTypes;

	public static final String dimensionsKey = "dimensions";

	public DefaultMetadata( final int nd )
	{
		this( "", nd );
	}

	public DefaultMetadata( final String path, final DatasetAttributes attributes )
	{
		super( path, attributes );
		final int nd = attributes.getNumDimensions();
		if( nd > 0 )
		{
			voxDims = new FinalVoxelDimensions( "pixel",
				DoubleStream.iterate( 1, x -> x ).limit( nd ).toArray());
		}
		else
			voxDims = null;

		keysToTypes = new HashMap<>();
		AbstractN5Metadata.addDatasetAttributeKeys( keysToTypes );
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
		AbstractN5Metadata.addDatasetAttributeKeys( keysToTypes );
	}

	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	@Override
	public DefaultMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if ( !check( metaMap ) )
			return null;

		final String dataset = ( String ) metaMap.get( "dataset" );

		final DatasetAttributes attributes = N5MetadataParser.parseAttributes( metaMap );
		if( attributes == null )
			return null;

		return new DefaultMetadata( dataset, attributes );
	}

	@Override
	public void writeMetadata( final DefaultMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		// does nothing
	}

	@Override
	public void writeMetadata( final DefaultMetadata t, final ImagePlus imp ) throws IOException
	{
		imp.setTitle( t.getPath() );
		final FinalVoxelDimensions voxdims = t.voxDims;
		if ( voxdims.numDimensions() > 0 )
			imp.getCalibration().pixelWidth = voxdims.dimension( 0 );

		if ( voxdims.numDimensions() > 1 )
			imp.getCalibration().pixelHeight = voxdims.dimension( 1 );

		if ( voxdims.numDimensions() > 2 )
			imp.getCalibration().pixelDepth = voxdims.dimension( 2 );

		imp.getCalibration().setUnit( voxdims.unit() );
	}

	@Override
	public DefaultMetadata readMetadata( final ImagePlus imp ) throws IOException
	{
		int nd = 2;
		if( imp.getNSlices() > 1 ){ nd++; }
		if( imp.getNFrames() > 1 ){ nd++; }
		if( imp.getNChannels() > 1 ){ nd++; }

		return new DefaultMetadata( "", nd );
	}

	@Override
	public AffineGet physicalTransform()
	{
		final int nd = voxDims != null ? voxDims.numDimensions() : 0;
		if( nd == 0 )
			return null;
		else if( nd == 2 )
			return new Scale2D( 1, 1 );
		else if( nd == 3 )
			return new Scale3D( 1, 1, 1 );
		else
			return new Scale( DoubleStream.generate( () -> 1.0 ).limit( nd ).toArray());
	}

	public String[] units()
	{
		final int nd = voxDims != null ? voxDims.numDimensions() : 0;	
		return Stream.generate( () -> "pixel" ).limit( nd ).toArray( String[]::new );
	}
}
