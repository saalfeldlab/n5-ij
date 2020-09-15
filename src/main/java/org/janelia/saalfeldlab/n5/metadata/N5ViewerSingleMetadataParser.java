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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class N5ViewerSingleMetadataParser implements N5GsonMetadataParser< N5SingleScaleMetadata >
{
    public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
    public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
    public static final String SCALES_KEY = "scales";
    public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

	private final HashMap< String, Class< ? > > keysToTypes;
  
    public N5ViewerSingleMetadataParser()
    {
    	this( true );
    }

    public N5ViewerSingleMetadataParser( final boolean parseMultiscale )
    {
    	keysToTypes = new HashMap<>();
		keysToTypes.put( DOWNSAMPLING_FACTORS_KEY, long[].class );
		keysToTypes.put( PIXEL_RESOLUTION_KEY, FinalVoxelDimensions[].class );
		keysToTypes.put( AFFINE_TRANSFORM_KEY, AffineTransform3D.class );
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

    private static AffineTransform3D buildTransform(
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
