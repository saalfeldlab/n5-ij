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

    private static final Predicate<String> scaleLevelPredicate = Pattern.compile("^s\\d+$").asPredicate();
	private boolean parseMultiscale;

	private final HashMap< String, Class< ? > > keysToTypes;
  
    public N5ViewerSingleMetadataParser()
    {
    	this( true );
    }

    public N5ViewerSingleMetadataParser( final boolean parseMultiscale )
    {
    	this.parseMultiscale = parseMultiscale;
    	
    	keysToTypes = new HashMap<>();
		keysToTypes.put( "dataset", String.class );
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

//    @Override
//	public N5SingleScaleMetadata parseMetadata( final N5Reader n5, final N5TreeNode... nodes ) throws Exception
//    {
//    	N5TreeNode node = nodes[ 0 ];
//		if( !node.isDataset )
//		{
//			throw new Exception( "Can't read from " + node.path + ".  Must be dataset." );
//		}
//		return parseMetadata( n5, node.path, n5.getAttributes( node.path ));
//    }
//
//	public N5SingleScaleMetadata parseMetadata( final N5Reader n5, final String dataset, final HashMap< String, JsonElement > map ) throws Exception
//	{
//		final long[] downsamplingFactors = GsonAttributesParser.parseAttribute( map, DOWNSAMPLING_FACTORS_KEY, long[].class, n5.getGson() );
//		FinalVoxelDimensions voxdim = GsonAttributesParser.parseAttribute( map, PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class, n5.getGson() );
//
//		final double[] pixelResolution = new double[ voxdim.numDimensions() ];
//		voxdim.dimensions( pixelResolution );
//
//		final AffineTransform3D extraTransform = GsonAttributesParser.parseAttribute( map, AFFINE_TRANSFORM_KEY, AffineTransform3D.class, n5.getGson() );
//		final AffineTransform3D transform = buildTransform( downsamplingFactors, pixelResolution, extraTransform );
//		return new N5SingleScaleMetadata( dataset, transform, voxdim.unit() );
//	}

//	@Override
//	public < R extends AbstractGsonReader> N5ImagePlusMetadata parseMetadata( final R n5, final String dataset, final HashMap< String, JsonElement > map ) throws Exception
//    {
//
//        if( ! parseMultiscale )
//        	return null;
//
//        // Could be a multiscale group, need to check if it contains datasets named s0..sN
//        String basePath = node.path;
//        final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();
//        for (final N5TreeNode childNode : node.children)
//            if (scaleLevelPredicate.test(childNode.getNodeName()))
//                scaleLevelNodes.put(childNode.getNodeName(), childNode);
//
//        if (scaleLevelNodes.isEmpty())
//            return null;
//
//        for (final N5TreeNode scaleLevelNode : scaleLevelNodes.values())
//            if (!scaleLevelNode.isDataset)
//                return null;
//
//        for (int i = 0; i < scaleLevelNodes.size(); ++i) {
//            final String scaleLevelKey = "s" + i;
//            if (!scaleLevelNodes.containsKey(scaleLevelKey))
//                return null;
//        }
//
//        final List<AffineTransform3D> scaleLevelTransforms = new ArrayList<>();
//
//        final double[][] deprecatedScales = n5.getAttribute(node.path, SCALES_KEY, double[][].class);
//        if (deprecatedScales != null) {
//            // this is a multiscale group in deprecated format
//			FinalVoxelDimensions voxdim = getVoxelDimensions( n5, node );
//			final double[] pixelResolution = new double[ voxdim.numDimensions() ];
//			voxdim.dimensions( pixelResolution );
//
//            final AffineTransform3D extraTransform = n5.getAttribute(node.path, AFFINE_TRANSFORM_KEY, AffineTransform3D.class);
//            for (int i = 0; i < Math.min(deprecatedScales.length, scaleLevelNodes.size()); ++i) {
//                final long[] downsamplingFactors = new long[deprecatedScales[i].length];
//                for (int d = 0; d < downsamplingFactors.length; ++d)
//                    downsamplingFactors[d] = Math.round(deprecatedScales[i][d]);
//                scaleLevelTransforms.add(buildTransform(downsamplingFactors, pixelResolution, extraTransform));
//            }
//        } else {
//            // this is a multiscale group, where scale level transforms are available through dataset metadata
//            for (int i = 0; i < scaleLevelNodes.size(); ++i) {
//                final String scaleLevelKey = "s" + i;
//                final N5Metadata scaleLevelMetadata = scaleLevelNodes.get(scaleLevelKey).metadata;
//                if (!(scaleLevelMetadata instanceof N5SingleScaleMetadata))
//                    return null;
//                scaleLevelTransforms.add(((N5SingleScaleMetadata) scaleLevelMetadata).transform);
//            }
//        }
//
//        final List<String> scaleLevelPaths = new ArrayList<>();
//        for (int i = 0; i < scaleLevelNodes.size(); ++i) {
//            final String scaleLevelKey = "s" + i;
//            scaleLevelPaths.add(scaleLevelNodes.get(scaleLevelKey).path);
//        }
//
//        return new N5MultiScaleMetadata(
//        		basePath,
//                scaleLevelPaths.toArray(new String[scaleLevelPaths.size()]),
//                scaleLevelTransforms.toArray(new AffineTransform3D[scaleLevelTransforms.size()]));
//    }
    
//    public static boolean isN5ViewerMultiscale( final N5TreeNode node )
//    {
//        final Map<String, N5TreeNode> scaleLevelNodes = new HashMap<>();
//        for (final N5TreeNode childNode : node.children)
//            if (scaleLevelPredicate.test(childNode.getNodeName()))
//                scaleLevelNodes.put(childNode.getNodeName(), childNode);
//
//        if (scaleLevelNodes.isEmpty())
//            return false;
//
//
//    	return false;
//    }

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

//    private static FinalVoxelDimensions getVoxelDimensions(final N5Reader n5, final N5TreeNode node) throws IOException
//    {
//        // try to read the pixel resolution attribute as VoxelDimensions (resolution and unit)
//        try {
//            final FinalVoxelDimensions voxelDimensions = n5.getAttribute(node.path, PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class);
//            if (voxelDimensions != null) {
//            	return voxelDimensions;
//            }
//        } catch (final JsonSyntaxException e) {
//            // the attribute exists but its value is structured differently, try parsing as an array
//        }
//
//        // try to read the pixel resolution attribute as a plain array
//        double[] res = n5.getAttribute(node.path, PIXEL_RESOLUTION_KEY, double[].class);
//        if( res != null )
//        {
//        	return new FinalVoxelDimensions( "pixel", res );
//        }
//        return null;
//    }
//
//    private static double[] getPixelResolution(final N5Reader n5, final N5TreeNode node) throws IOException
//    {
//        // try to read the pixel resolution attribute as VoxelDimensions (resolution and unit)
//        try {
//            final FinalVoxelDimensions voxelDimensions = n5.getAttribute(node.path, PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class);
//            if (voxelDimensions != null) {
//                final double[] pixelResolution = new double[voxelDimensions.numDimensions()];
//                voxelDimensions.dimensions(pixelResolution);
//                return pixelResolution;
//            }
//        } catch (final JsonSyntaxException e) {
//            // the attribute exists but its value is structured differently, try parsing as an array
//        }
//
//        // try to read the pixel resolution attribute as a plain array
//        return n5.getAttribute(node.path, PIXEL_RESOLUTION_KEY, double[].class);
//    }
}
