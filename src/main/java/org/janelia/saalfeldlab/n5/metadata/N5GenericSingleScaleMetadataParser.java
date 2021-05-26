package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5LabelMultisets;

import java.io.IOException;
import java.util.Optional;

public class N5GenericSingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata>, N5MetadataWriter<N5SingleScaleMetadata> {

  public static final String MIN = "min";
  public static final String MAX = "max";
  public static final String RESOLUTION = "resolution";
  public static final String OFFSET = "offset";
  public static final String IS_LABEL_MULTISET = "isLabelMultiset";
  public static final String DOWNSAMPLING_FACTORS = "downsamplingFactors";

  @Override
  public Optional<N5SingleScaleMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

	try {
	  n5.getAttribute(node.getPath(), MIN, double.class);
	  n5.getAttribute(node.getPath(), MAX, double.class);
	  n5.getAttribute(node.getPath(), RESOLUTION, double[].class);
	  n5.getAttribute(node.getPath(), OFFSET, double[].class);
	  n5.getAttribute(node.getPath(), DOWNSAMPLING_FACTORS, double[].class);
	  n5.getAttribute(node.getPath(), IS_LABEL_MULTISET, boolean.class);
	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());

	  if (attributes == null)
		return Optional.empty();

	  final String path = node.getPath();

	  final double[] resolution = Optional.ofNullable(n5.getAttribute(node.getPath(), RESOLUTION, double[].class)).orElse(new double[]{1.0, 1.0, 1.0});
	  if (resolution.length != 3)
		return Optional.empty();

	  final double[] offset = Optional.ofNullable(n5.getAttribute(node.getPath(), OFFSET, double[].class)).orElse(new double[]{1.0, 1.0, 1.0});
	  if (offset.length != 3)
		return Optional.empty();

	  final double[] downsamplingFactors = Optional.ofNullable(n5.getAttribute(node.getPath(), DOWNSAMPLING_FACTORS, double[].class)).orElse(new double[]{1.0, 1.0, 1.0});

	  final double min = Optional.ofNullable(n5.getAttribute(node.getPath(), MIN, double.class)).orElse(0.0);
	  final double max = Optional.ofNullable(n5.getAttribute(node.getPath(), MAX, double.class)).orElseGet(() -> IntensityMetadata.maxForDataType(attributes.getDataType()));

	  final Boolean isLabelMultiset = N5LabelMultisets.isLabelMultisetType(n5, node.getPath());

	  final AffineTransform3D transform = new AffineTransform3D();
	  transform.set(
			  resolution[0], 0, 0, offset[0],
			  0, resolution[1], 0, offset[1],
			  0, 0, resolution[2], offset[2]
	  );

	  N5SingleScaleMetadata metadata = new N5SingleScaleMetadata(path, transform, downsamplingFactors, resolution, offset, "pixel", attributes, min, max, isLabelMultiset);
	  return Optional.of(metadata);
	} catch (IOException e) {
	  return Optional.empty();
	}
  }

  @Override public void writeMetadata(N5SingleScaleMetadata n5SingleScaleMetadata, N5Writer n5, String group) throws Exception {
	//TODO write this out.
  }
}
