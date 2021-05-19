package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;

import java.io.IOException;
import java.util.Optional;

public class N5SingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata>, N5MetadataWriter<N5SingleScaleMetadata> {

  public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
  public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
  public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

  public static AffineTransform3D buildTransform(
		  final double[] downsamplingFactors,
		  final double[] pixelResolution,
		  final Optional<AffineTransform3D> extraTransformOpt) {

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

  public static Optional<double[]> inferDownsamplingFactorsFromDataset(final String dataset) {

	final String datasetNumber = dataset.replaceAll("^s", "");
	try {
	  final long f = Long.parseLong(datasetNumber);
	  return Optional.of(new double[]{f, f, f});
	} catch (Exception e) {
	  return Optional.empty();
	}
  }

  @Override
  public Optional<N5SingleScaleMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

	try {

	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());
	  if (attributes == null)
		return Optional.empty();

	  final int nd = attributes.getNumDimensions();

	  final double[] downsamplingFactors = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), DOWNSAMPLING_FACTORS_KEY, double[].class))
			  .orElseGet(() -> inferDownsamplingFactorsFromDataset(node.getNodeName()).orElseGet(() -> new double[]{1.0, 1.0, 1.0}));

	  final Optional<FinalVoxelDimensions> voxdim = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class));

	  final double[] pixelResolution = voxdim.map(x -> {
		final double[] res = new double[nd];
		x.dimensions(res);
		return res;
	  }).orElseGet(() -> new double[]{1.0, 1.0, 1.0});

	  final String unit = voxdim.map(x -> x.unit()).orElse("pixel");

	  final Optional<AffineTransform3D> extraTransform = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), AFFINE_TRANSFORM_KEY, AffineTransform3D.class));

	  final AffineTransform3D transform = buildTransform(downsamplingFactors, pixelResolution, extraTransform);

	  double[] offset = new double[]{transform.get(0, 3), transform.get(1, 3), transform.get(2, 3)};

	  return Optional.of(new N5SingleScaleMetadata(node.getPath(), transform, downsamplingFactors, pixelResolution, offset, unit, attributes));

	} catch (IOException e) {
	  return Optional.empty();
	}
  }

  @Override
  public void writeMetadata(final N5SingleScaleMetadata t, final N5Writer n5, final String group) throws Exception {

	final double[] pixelResolution = new double[]{
			t.transform.get(0, 0),
			t.transform.get(1, 1),
			t.transform.get(2, 2)};

	final FinalVoxelDimensions voxdims = new FinalVoxelDimensions(t.unit, pixelResolution);
	n5.setAttribute(group, PIXEL_RESOLUTION_KEY, voxdims);
  }

}
