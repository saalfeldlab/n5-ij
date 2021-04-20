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
import java.util.Objects;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;

public class N5SingleScaleMetadata extends AbstractN5Metadata<N5SingleScaleMetadata> 
	implements ImageplusMetadata< N5SingleScaleMetadata >, PhysicalMetadata
{
    public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
    public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
    public static final String SCALES_KEY = "scales";
    public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

    public final AffineTransform3D transform;
    
    public final double[] pixelResolution;

    public final long[] downsamplingFactors;

    public final String unit;

    public N5SingleScaleMetadata( final String path, final AffineTransform3D transform,
    		final String unit,
    		final DatasetAttributes attributes )
    {
		super( path, attributes );

		Objects.requireNonNull( path );
		Objects.requireNonNull( transform );
        this.transform = transform;

        this.pixelResolution = null;
        this.downsamplingFactors = null;

        if( unit == null )
        	this.unit = "pixel";
        else
			this.unit = unit;
    }
    
    public N5SingleScaleMetadata( final String path, final double[] pixelResolution,
    		final long[] downsamplingFactors,
    		final String unit,
    		final DatasetAttributes attributes )
    {
		super( path, attributes );
		Objects.requireNonNull( path );
		
		if( pixelResolution != null )
			this.pixelResolution = pixelResolution;
		else
		{
			this.pixelResolution = new double[ 3 ];
			Arrays.fill( this.pixelResolution, 1 );
		}

		if( downsamplingFactors != null )
			this.downsamplingFactors = downsamplingFactors;
		else
		{
			this.downsamplingFactors = new long[ 3 ];
			Arrays.fill( this.downsamplingFactors, 1 );
		}

		this.transform = buildTransform( downsamplingFactors, pixelResolution, null );

        if( unit == null )
        	this.unit = "um";
        else
			this.unit = unit;
    }

    public N5SingleScaleMetadata(final String path, final AffineTransform3D transform, final String unit )
    {
    	this( path, transform, unit, null );
    }

    public N5SingleScaleMetadata( final String path, final AffineTransform3D transform )
    {
    	this( path, transform, null );
    }

    public N5SingleScaleMetadata( final String path )
    {
    	this( path, new AffineTransform3D(), null );
    }

    public N5SingleScaleMetadata()
    {
    	this( "", new AffineTransform3D(), null );
    }
	
	@Override
	public N5SingleScaleMetadata parseMetadata( final N5Reader n5, final String dataset ) throws IOException
	{
		final DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
		if( attrs == null )
			return null;

		FinalVoxelDimensions voxdims = null;

		try {
			voxdims = n5.getAttribute( dataset, PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class );
		} catch( Exception e ) {}

		double[] pixRes = null;
		String unit = "um";
		if( voxdims == null )
			pixRes = n5.getAttribute( dataset, PIXEL_RESOLUTION_KEY, double[].class );
		else
		{
			pixRes = new double[ 3 ];
			voxdims.dimensions( pixRes );
			unit = voxdims.unit();
		}

		long[] downsamplingFactors = null;
		try {
			downsamplingFactors = n5.getAttribute( dataset, DOWNSAMPLING_FACTORS_KEY, long[].class );
		} catch( Exception e ) {}
		
		if( downsamplingFactors == null && pixRes == null )
			return null;


		return new N5SingleScaleMetadata( dataset, pixRes, downsamplingFactors, unit, attrs );
	}

	@Override
	public void writeMetadata( final N5SingleScaleMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		final double[] pixelResolution = new double[]{
				t.transform.get( 0, 0 ),
				t.transform.get( 1, 1 ),
				t.transform.get( 2, 2 ) };

		final FinalVoxelDimensions voxdims = new FinalVoxelDimensions( t.unit, pixelResolution );
		n5.setAttribute( dataset, PIXEL_RESOLUTION_KEY, voxdims );
	}

	@Override
	public void writeMetadata( final N5SingleScaleMetadata t, final ImagePlus ip ) throws IOException
	{
		ip.setTitle( t.getPath() );
		ip.getCalibration().pixelWidth = t.transform.get( 0, 0 );
		ip.getCalibration().pixelHeight = t.transform.get( 1, 1 );
		ip.getCalibration().pixelDepth = t.transform.get( 2, 2 );
		ip.getCalibration().setUnit( t.unit );
		ip.setDimensions( 1, ip.getStackSize(), 1 );
	}

	@Override
	public N5SingleScaleMetadata readMetadata( final ImagePlus ip ) throws IOException
	{
		final double sx = ip.getCalibration().pixelWidth;
		final double sy = ip.getCalibration().pixelHeight;
		final double sz = ip.getCalibration().pixelDepth;
		final String unit = ip.getCalibration().getUnit();

		final AffineTransform3D xfm = new AffineTransform3D();
		xfm.set( sx, 0.0, 0.0, 0.0, 0.0, sy, 0.0, 0.0, 0.0, 0.0, sz, 0.0 );

		return new N5SingleScaleMetadata( "", xfm, unit );
	}


   public static AffineTransform3D buildTransform(
            long[] downsamplingFactors,
            double[] pixelResolution,
            final AffineTransform3D extraTransform)
    {
        if (downsamplingFactors == null)
            downsamplingFactors = new long[] {1, 1, 1};

        if (pixelResolution == null)
            pixelResolution = new double[] {1, 1, 1};

        final AffineTransform3D mipmapTransform = new AffineTransform3D();
        mipmapTransform.set(
                downsamplingFactors[ 0 ], 0, 0, 0.5 * ( downsamplingFactors[ 0 ] - 1 ),
                0, downsamplingFactors[ 1 ], 0, 0.5 * ( downsamplingFactors[ 1 ] - 1 ),
                0, 0, downsamplingFactors[ 2 ], 0.5 * ( downsamplingFactors[ 2 ] - 1 ) );

        final AffineTransform3D transform = new AffineTransform3D();
        transform.preConcatenate(mipmapTransform).preConcatenate(new Scale3D(pixelResolution));
        if (extraTransform != null)
            transform.preConcatenate(extraTransform);
        return transform;
    }
   
   @Override
	public AffineGet physicalTransform()
	{
		return transform;
	}
	
	@Override
	public String[] units()
	{
		return Stream.generate( () -> unit ).limit( 3 ).toArray( String[]::new );
	}

}
