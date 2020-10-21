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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;

public class N5CosemMetadata extends AbstractN5Metadata implements
	N5GsonMetadataParser< N5CosemMetadata >, N5MetadataWriter< N5CosemMetadata >, ImageplusMetadata< N5CosemMetadata >
{
	public static final String pixelResolutionKey = "pixelResolution";

	private boolean separateChannels = true;

	private final CosemTransform cosemTransformMeta;

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

	public N5CosemMetadata( final String path, final CosemTransform cosemTransformMeta,
			final DatasetAttributes attributes )
	{
		super( path, attributes );
		this.cosemTransformMeta = cosemTransformMeta;

		keysToTypes = new HashMap<>();
		keysToTypes.put( CosemTransform.KEY, CosemTransform.class );
		AbstractN5Metadata.addDatasetAttributeKeys( keysToTypes );
	}

	public CosemTransform getTransform()
	{
		return cosemTransformMeta;
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
	public boolean check( final Map< String, Object > metaMap )
	{
		final Map< String, Class< ? > > requiredKeys = AbstractN5Metadata.datasetAtttributeKeys();
		for( final String k : requiredKeys.keySet() )
		{
			if ( !metaMap.containsKey( k ) )
				return false;
			else if( metaMap.get( k ) == null )
				return false;
		}

		// needs to contain one of pixelResolution key
		if( !metaMap.containsKey( CosemTransform.KEY ))
		{
			return false;
		}

		return true;
	}

	@Override
	public N5CosemMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if ( !check( metaMap ) )
			return null;

		final DatasetAttributes attributes = N5MetadataParser.parseAttributes( metaMap );
		if( attributes == null )
			return null;

		final String dataset = ( String ) metaMap.get( "dataset" );
		final CosemTransform transform = ( CosemTransform ) metaMap.get( CosemTransform.KEY );

		if( transform == null )
			return null;

		return new N5CosemMetadata( dataset, transform, attributes );
	}

	@Override
	public void writeMetadata( final N5CosemMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		if( t.cosemTransformMeta != null )
			n5.setAttribute( dataset, CosemTransform.KEY, t.cosemTransformMeta );
	}

	@Override
	public void writeMetadata( final N5CosemMetadata t, final ImagePlus imp ) throws IOException
	{
		final CosemTransform transform = t.cosemTransformMeta;

		if ( transform != null )
		{
			final int nd = transform.scale.length;

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
	}

	@Override
	public N5CosemMetadata readMetadata( final ImagePlus imp ) throws IOException
	{
		int nd = 2;
		if( !separateChannels && imp.getNChannels() > 1 ){ nd++; }
		if( imp.getNSlices() > 1 ){ nd++; }
		if( imp.getNFrames() > 1 ){ nd++; }

		final String[] axes = new String[ nd ];
		axes[ 0 ] = "x";
		axes[ 1 ] = "y";

		int c = 2;
		if ( !separateChannels && imp.getNChannels() > 1 ){ axes[ c++ ] = "c"; }
		if ( imp.getNSlices() > 1 ){ axes[ c++ ] = "z"; }
		if ( imp.getNFrames() > 1 ){ axes[ c++ ] = "t"; }

		// unit
		final String[] units = new String[ nd ];
		Arrays.fill( units, imp.getCalibration().getUnit());

		final double[] scale = new double[ nd ];
		final double[] translation = new double[ nd ];

		scale[ 0 ] = imp.getCalibration().pixelWidth;
		scale[ 1 ] = imp.getCalibration().pixelHeight;
		if( nd > 2 ){ scale[ 2 ] = imp.getCalibration().pixelDepth; }

		translation[ 0 ] = imp.getCalibration().xOrigin;
		translation[ 1 ] = imp.getCalibration().yOrigin;
		if( nd > 2 ){ translation[ 2 ] = imp.getCalibration().zOrigin; }

		return new N5CosemMetadata( "", new CosemTransform( axes, scale, translation, units ) );
	}

	public static class CosemTransform
	{
		public transient static final String KEY = "transform";
		public final String[] axes;
		public final double[] scale;
		public final double[] translate;
		public final String[] units;

		public CosemTransform( final String[] axes, final double[] scale, final double[] translate, final String[] units )
		{
			this.axes = axes;
			this.scale = scale;
			this.translate = translate;
			this.units = units;
		}

		public AffineTransform3D toAffineTransform3d()
		{
			assert( scale.length == 3  && translate.length == 3 );

			final AffineTransform3D transform = new AffineTransform3D();
			transform.set(	scale[0], 0, 0, translate[0],
							0, scale[1], 0, translate[1],
							0, 0, scale[2], translate[2] );
			return transform;
		}
	}

}
