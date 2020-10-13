/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;

public class N5SingleScaleMetadata implements N5Metadata, N5GsonMetadataParser< N5SingleScaleMetadata >,
N5MetadataWriter< N5SingleScaleMetadata >, ImageplusMetadata< N5SingleScaleMetadata >
{
    public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
    public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
    public static final String SCALES_KEY = "scales";
    public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

	private final HashMap< String, Class< ? > > keysToTypes;

    public final String path;

    public final AffineTransform3D transform;

    public final String unit;

    public N5SingleScaleMetadata(final String path, final AffineTransform3D transform, final String unit )
    {
		Objects.requireNonNull( path );
		Objects.requireNonNull( transform );

        this.path = path;
        this.transform = transform;

        if( unit == null )
        	this.unit = "pixel";
        else
			this.unit = unit;

    	keysToTypes = new HashMap<>();
		keysToTypes.put( DOWNSAMPLING_FACTORS_KEY, long[].class );
		keysToTypes.put( PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class );
		keysToTypes.put( AFFINE_TRANSFORM_KEY, AffineTransform3D.class );
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
	public String getPath()
	{
		return path;
	}


	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

    @Override
	public N5SingleScaleMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if( !N5MetadataParser.hasRequiredKeys( keysToTypes(), metaMap ))
			throw new Exception( "Could not parse as N5SingleScaleMetadata.");

		String dataset = ( String ) metaMap.get( "dataset" );

		final long[] downsamplingFactors = ( long[] ) metaMap.get( DOWNSAMPLING_FACTORS_KEY );
		FinalVoxelDimensions voxdim = ( FinalVoxelDimensions ) metaMap.get( PIXEL_RESOLUTION_KEY );

		final double[] pixelResolution = new double[ voxdim.numDimensions() ];
		voxdim.dimensions( pixelResolution );

		final AffineTransform3D extraTransform = ( AffineTransform3D ) metaMap.get( AFFINE_TRANSFORM_KEY );
		final AffineTransform3D transform = buildTransform( downsamplingFactors, pixelResolution, extraTransform );
		return new N5SingleScaleMetadata( dataset, transform, voxdim.unit() );
	}

	@Override
	public void writeMetadata( N5SingleScaleMetadata t, N5Writer n5, String dataset ) throws Exception
	{
		double[] pixelResolution = new double[]{
				t.transform.get( 0, 0 ),
				t.transform.get( 1, 1 ),
				t.transform.get( 2, 2 ) };

		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( t.unit, pixelResolution );
		n5.setAttribute( dataset, PIXEL_RESOLUTION_KEY, voxdims );
	}

	@Override
	public void writeMetadata( N5SingleScaleMetadata t, ImagePlus ip ) throws IOException
	{
		ip.getCalibration().pixelWidth = t.transform.get( 0, 0 );
		ip.getCalibration().pixelHeight = t.transform.get( 1, 1 );
		ip.getCalibration().pixelDepth = t.transform.get( 2, 2 );
		ip.getCalibration().setUnit( t.unit );
		ip.setDimensions( 1, ip.getStackSize(), 1 );
	}

	@Override
	public N5SingleScaleMetadata readMetadata( ImagePlus ip ) throws IOException
	{
		double sx = ip.getCalibration().pixelWidth;
		double sy = ip.getCalibration().pixelHeight;
		double sz = ip.getCalibration().pixelDepth;
		String unit = ip.getCalibration().getUnit();

		AffineTransform3D xfm = new AffineTransform3D();
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

}
