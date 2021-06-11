package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.Scale3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5LabelMultisets;

import java.io.IOException;
import java.util.Optional;

/**
 * Default {@link N5MetadataParser} for {@link N5SingleScaleMetadata}.
 * <p>
 * This parser provides default values for all metadata fields when values for the corresponding
 * keys are not found, i.e., it returns empty metadata only when parsing throws an exception.
 * As a result, this parser should appear as the last element of parser lists that are passed to
 * {@link N5DatasetDiscoverer}.
 * <p>
 * When <i>downsamplingFactors</i> are specified, this parser assumes downsampling was performed using
 * averaging and includes an offset to the resulting spatial transformation. Specifically, for a downsamplling
 * factor of <i>f</i>, this parser yields an offset of <i>(f-1)/2</i>.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class N5SingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata>, N5MetadataWriter<N5SingleScaleMetadata> {

  public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
  public static final String PIXEL_RESOLUTION_KEY = "pixelResolution";
  public static final String AFFINE_TRANSFORM_KEY = "affineTransform";

  public static AffineTransform3D buildTransform(
		  final double[] downsamplingFactors,
		  final double[] pixelResolution,
		  final Optional<AffineTransform3D> extraTransformOpt) {

	final AffineTransform3D mipmapTransform = new AffineTransform3D();
	if( downsamplingFactors.length >= 3 )
	{
		mipmapTransform.set(
				downsamplingFactors[0], 0, 0, 0.5 * (downsamplingFactors[0] - 1),
				0, downsamplingFactors[1], 0, 0.5 * (downsamplingFactors[1] - 1),
				0, 0, downsamplingFactors[2], 0.5 * (downsamplingFactors[2] - 1));
	}
	else if( downsamplingFactors.length == 2 )
	{
		mipmapTransform.set(
				downsamplingFactors[0], 0, 0, 0.5 * (downsamplingFactors[0] - 1),
				0, downsamplingFactors[1], 0, 0.5 * (downsamplingFactors[1] - 1),
				0, 0, 1.0, 0.0);
	}

	final AffineTransform3D transform = new AffineTransform3D();
	transform.preConcatenate(mipmapTransform).preConcatenate(new Scale3D(pixelResolution));
	extraTransformOpt.ifPresent(x -> transform.preConcatenate(x));
	return transform;
  }

  public static Optional<double[]> inferDownsamplingFactorsFromDataset(final String dataset) {

	final String datasetNumber = dataset.replaceAll("^s", "");
	try {
	  final long f = Long.parseLong(datasetNumber) + 1;
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

	  Optional<FinalVoxelDimensions> voxdim;
	  try {
		  voxdim = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), PIXEL_RESOLUTION_KEY, FinalVoxelDimensions.class));
	  } catch ( Exception e ) {
		  voxdim = Optional.empty();
	  }

	  final String unit;
	  final double[] pixelResolution;
	  if( voxdim.isPresent() ) {

		  pixelResolution = voxdim.map(x -> {
			final double[] res = new double[nd];
			x.dimensions(res);
			return res;
		  }).orElseGet(() -> new double[]{1.0, 1.0, 1.0});
		  unit = voxdim.map(x -> x.unit()).orElse("pixel");

	  } else {

		pixelResolution =
				Optional.ofNullable(n5.getAttribute(node.getPath(), PIXEL_RESOLUTION_KEY, double[].class))
						.orElseGet(() -> new double[]{1.0, 1.0, 1.0});
		unit = "pixel";
	  }

	  final Optional<AffineTransform3D> extraTransform = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), AFFINE_TRANSFORM_KEY, AffineTransform3D.class));

	  final AffineTransform3D transform = buildTransform(downsamplingFactors, pixelResolution, extraTransform);

	  double[] offset = new double[]{transform.get(0, 3), transform.get(1, 3), transform.get(2, 3)};

	  final boolean isLabelMultiset = N5LabelMultisets.isLabelMultisetType(n5, node.getPath());

	  return Optional.of(new N5SingleScaleMetadata(node.getPath(), transform, downsamplingFactors, pixelResolution, offset, unit, attributes, isLabelMultiset));

	} catch (IOException e) {
		e.printStackTrace();
	  return Optional.empty();
	}
  }

  @Override
  public void writeMetadata(final N5SingleScaleMetadata t, final N5Writer n5, final String group) throws Exception {

	final double[] pixelResolution = new double[]{
			t.spatialTransform3d().get(0, 0),
			t.spatialTransform3d().get(1, 1),
			t.spatialTransform3d().get(2, 2)};

	final FinalVoxelDimensions voxdims = new FinalVoxelDimensions(t.unit(), pixelResolution);
	n5.setAttribute(group, PIXEL_RESOLUTION_KEY, voxdims);

	if (t.getDownsamplingFactors() != null)
	  n5.setAttribute(group, DOWNSAMPLING_FACTORS_KEY, t.getDownsamplingFactors());

  }

}
