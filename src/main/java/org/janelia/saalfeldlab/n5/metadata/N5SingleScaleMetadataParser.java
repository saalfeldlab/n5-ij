package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class N5SingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata> {

//	 {
//		 keysToTypes.put(N5SingleScaleMetadata.DOWNSAMPLING_FACTORS_KEY, long[].class);
//		 keysToTypes.put(N5SingleScaleMetadata.PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class);
//		 keysToTypes.put(N5SingleScaleMetadata.AFFINE_TRANSFORM_KEY, AffineTransform3D.class);
//	 }

//	 @Override
//	 public boolean check(final Map<String, Object> metaMap) {
//	
//	 final Map<String, Class<?>> requiredKeys =
//	 AbstractN5DatasetMetadataParser.datasetAtttributeKeys();
//	 for (final String k : requiredKeys.keySet()) {
//	 if (!metaMap.containsKey(k))
//	 return false;
//	 else if (metaMap.get(k) == null)
//	 return false;
//	 }
//	
//	 // needs to contain one of pixelResolution key
//	 return metaMap.containsKey(N5SingleScaleMetadata.PIXEL_RESOLUTION_KEY);
//	 }

	@Override
	public Optional<N5SingleScaleMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

		// if (!check(metaMap))
		// return Optional.empty();
		try {

			final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());
			if (attributes == null)
				return Optional.empty();

			final int nd = attributes.getNumDimensions();

			final Optional<long[]> downsamplingFactors = Optional.ofNullable(
					n5.getAttribute(node.getPath(), N5SingleScaleMetadata.DOWNSAMPLING_FACTORS_KEY, long[].class));

			final Optional<FinalVoxelDimensions> voxdim = Optional.ofNullable(
					n5.getAttribute(node.getPath(), N5SingleScaleMetadata.PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class));

			final Optional<double[]> pixelResolution = voxdim.map(x -> {
				final double[] res = new double[nd];
				x.dimensions(res);
				return res;
			});

			final String unit = voxdim.map(x -> x.unit()).orElse("pixel");

			final Optional<AffineTransform3D> extraTransform = Optional.ofNullable(
					n5.getAttribute(node.getPath(), N5SingleScaleMetadata.AFFINE_TRANSFORM_KEY, AffineTransform3D.class));

			final AffineTransform3D transform = buildTransform(node.getPath(), downsamplingFactors, pixelResolution, extraTransform);

			return Optional.of(new N5SingleScaleMetadata(node.getPath(), transform, unit, attributes));

		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public static AffineTransform3D buildTransform(
			final String datasetName,
			final Optional<long[]> downsamplingFactorsOpt,
			final Optional<double[]> pixelResolutionOpt,
			final Optional<AffineTransform3D> extraTransformOpt ) {

		final long[] downsamplingFactors = downsamplingFactorsOpt.orElse( inferDownsamplingFactorsFromDataset(datasetName));
		final double[] pixelResolution = pixelResolutionOpt.orElse( new double[] { 1, 1, 1 });

		final AffineTransform3D mipmapTransform = new AffineTransform3D();
		mipmapTransform.set(
				downsamplingFactors[0], 0, 0, 0.5 * (downsamplingFactors[0] - 1),
				0, downsamplingFactors[1], 0, 0.5 * (downsamplingFactors[1] - 1),
				0, 0, downsamplingFactors[2], 0.5 * (downsamplingFactors[2] - 1));

		final AffineTransform3D transform = new AffineTransform3D();
		transform.preConcatenate(mipmapTransform).preConcatenate(new Scale3D(pixelResolution));
		extraTransformOpt.ifPresent(x -> transform.preConcatenate(x));
		return transform;
	}

	public static long[] inferDownsamplingFactorsFromDataset( final String dataset )
	{
		final long f = Long.parseLong( dataset.substring( dataset.lastIndexOf('/') + 2));
		return new long[] { f, f, f };
	}

	@Override
	public HashMap<String, Class<?>> keysToTypes() {
		// TODO this probably goes away 
		return null;
	}

	@Override
	public Optional<N5SingleScaleMetadata> parseMetadata(Map<String, Object> map) {
		// TODO this definitely goes away 
		return null;
	}
}
