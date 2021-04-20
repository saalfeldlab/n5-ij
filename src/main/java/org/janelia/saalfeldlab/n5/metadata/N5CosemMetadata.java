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

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;

public class N5CosemMetadata extends AbstractN5Metadata<N5CosemMetadata> 
	implements ImageplusMetadata< N5CosemMetadata >, PhysicalMetadata
{
	public static final String pixelResolutionKey = "pixelResolution";

	private final CosemTransform cosemTransformMeta;
	
	private final boolean separateChannels = true;

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
	}

	public CosemTransform getTransform()
	{
		return cosemTransformMeta;
	}

	@Override
	public N5CosemMetadata parseMetadata( final N5Reader n5, final String dataset ) throws IOException
	{
		final DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
		if( attrs == null )
			return null;

		final CosemTransform transform = n5.getAttribute( dataset, CosemTransform.KEY, CosemTransform.class );
		if( transform == null )
			return null;

		return new N5CosemMetadata( dataset, transform, attrs );
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
		imp.setTitle( t.getPath() );

		if ( transform != null )
		{
			final int nd = transform.scale.length;

			if ( nd > 0 )
			{
				imp.getCalibration().pixelWidth = transform.scale[ nd - 1 ];
				imp.getCalibration().setXUnit( transform.units[ nd - 1 ] );
				imp.getCalibration().xOrigin = transform.translate[ nd - 1 ];
			}

			if ( nd > 1 )
			{
				imp.getCalibration().pixelHeight = transform.scale[ nd - 2 ];
				imp.getCalibration().setYUnit( transform.units[ nd - 2 ] );
				imp.getCalibration().yOrigin = transform.translate[ nd - 2 ];
			}

			if ( nd > 2 )
			{
				imp.getCalibration().pixelDepth = transform.scale[ nd - 3 ];
				imp.getCalibration().setZUnit( transform.units[ nd - 3 ] );
				imp.getCalibration().zOrigin = transform.translate[ nd - 3 ];
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
		if ( !separateChannels && imp.getNChannels() > 1 ){ axes[ c++ ] = "c"; }
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

		return new N5CosemMetadata( "", new CosemTransform( axes, scale, translation, units ) );
	}

	public static class CosemTransform
	{
		// COSEM scales and translations are in c-order
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

		public AffineGet getAffine()
		{
			assert( scale.length == 3  && translate.length == 3 );

			// COSEM scales and translations are in c-order
			double[] scaleRev = new double[ scale.length ];
			double[] translateRev = new double[ translate.length ];

			int j = scale.length - 1;
			for( int i = 0; i < scale.length; i++ )
			{
				scaleRev[ i ] = scale[ j ];
				translateRev[ i ] = translate[ j ];
				j--;
			}

			return new ScaleAndTranslation( scaleRev, translateRev );
		}

		public AffineTransform3D toAffineTransform3d()
		{
			assert( scale.length == 3  && translate.length == 3 );

			// COSEM scales and translations are in c-order
			final AffineTransform3D transform = new AffineTransform3D();
			transform.set(	scale[2], 0, 0, translate[2],
							0, scale[1], 0, translate[1],
							0, 0, scale[0], translate[0] );
			return transform;
		}
	}
	
	@Override
	public AffineGet physicalTransform()
	{
		return getTransform().getAffine();
	}
	
	@Override
	public String[] units()
	{
		String[] rawUnits = getTransform().units;
		String[] out = new String[ rawUnits.length ];
		int j = rawUnits.length - 1;
		for( int i = 0; i < rawUnits.length; i++ )
		{
			out[ i ] = rawUnits[ j ];
			j--;
		}
		return out;
	}

	@Override
	public AffineTransform3D physicalTransform3d()
	{
		return getTransform().toAffineTransform3d();
	}

}
