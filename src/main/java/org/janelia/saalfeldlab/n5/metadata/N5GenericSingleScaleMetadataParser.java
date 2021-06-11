package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5LabelMultisets;

import java.io.IOException;
import java.util.Optional;

/**
 * A parser for {@link N5SingleScaleMetadata} with whose keys
 * can be specified. 
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class N5GenericSingleScaleMetadataParser implements N5MetadataParser<N5SingleScaleMetadata>, N5MetadataWriter<N5SingleScaleMetadata> {

  public static final String DEFAULT_MIN = "min";
  public static final String DEFAULT_MAX = "max";
  public static final String DEFAULT_RESOLUTION = "resolution";
  public static final String DEFAULT_OFFSET = "offset";
  public static final String DEFAULT_UNIT = "unit";
  public static final String DEFAULT_DOWNSAMPLING_FACTORS = "downsamplingFactors";
  public static final String DEFAULT_IS_LABEL_MULTISET = "isLabelMultiset";
  
  public final String minKey;
  public final String maxKey;
  public final String resolutionKey;
  public final String offsetKey;
  public final String downsamplingFactorsKey;
  public final String isLabelMultisetKey;

  public N5GenericSingleScaleMetadataParser() {
	  minKey = DEFAULT_MIN;
	  maxKey = DEFAULT_MAX;
	  resolutionKey = DEFAULT_RESOLUTION;
	  offsetKey = DEFAULT_OFFSET;
	  downsamplingFactorsKey = DEFAULT_DOWNSAMPLING_FACTORS;
	  isLabelMultisetKey = DEFAULT_IS_LABEL_MULTISET;
  }

  public N5GenericSingleScaleMetadataParser(final String minKey, final String maxKey,
		  final String resolutionKey, final String offsetKey,
		  final String downsamplingFactorsKey, final String isLabelMultisetKey) {

	this.minKey = minKey;
	this.maxKey = maxKey;
	this.resolutionKey = resolutionKey;
	this.offsetKey = offsetKey;
	this.downsamplingFactorsKey = downsamplingFactorsKey;
	this.isLabelMultisetKey = isLabelMultisetKey;
  }

  public static Builder builder() {

	return new Builder();
  }

  public static class Builder {

	private String minKey = DEFAULT_MIN;
	private String maxKey = DEFAULT_MAX;
	private String resolutionKey = DEFAULT_RESOLUTION;
	private String offsetKey = DEFAULT_OFFSET;
	private String downsamplingFactorsKey = DEFAULT_DOWNSAMPLING_FACTORS;
	private String isLabelMultisetKey = DEFAULT_IS_LABEL_MULTISET;

	public Builder min(String key) {

	  this.minKey = key;
	  return this;
	}

	public Builder max(String key) {

	  this.maxKey = key;
	  return this;
	}

	public Builder resolution(String key) {

	  this.resolutionKey = key;
	  return this;
	}

	public Builder offset(String key) {

	  this.offsetKey = key;
	  return this;
	}

	public Builder downsamplingFactors(String key) {

	  this.downsamplingFactorsKey = key;
	  return this;
	}

	public Builder isLabelMultiset(String key) {

	  this.isLabelMultisetKey = key;
	  return this;
	}

	public N5GenericSingleScaleMetadataParser build() {

	  return new N5GenericSingleScaleMetadataParser(minKey, maxKey, resolutionKey, offsetKey, downsamplingFactorsKey, isLabelMultisetKey);
	}
  }

  @Override
  public Optional<N5SingleScaleMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

	try {
	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());

	  if (attributes == null)
		return Optional.empty();

	  final String path = node.getPath();

	  final double[] resolution = Optional.ofNullable(n5.getAttribute(node.getPath(), resolutionKey, double[].class)).orElse(new double[]{1.0, 1.0, 1.0});
	  if (resolution.length < attributes.getNumDimensions() )
		return Optional.empty();


	  final double[] downsamplingFactors = Optional.ofNullable(n5.getAttribute(node.getPath(), downsamplingFactorsKey, double[].class)).orElse(new double[]{1.0, 1.0, 1.0});
	  if (downsamplingFactors.length < attributes.getNumDimensions() )
		return Optional.empty();

	  final double min = Optional.ofNullable(n5.getAttribute(node.getPath(), minKey, double.class)).orElse(0.0);
	  final double max = Optional.ofNullable(n5.getAttribute(node.getPath(), maxKey, double.class)).orElseGet(() -> IntensityMetadata.maxForDataType(attributes.getDataType()));

	  final Boolean isLabelMultiset = N5LabelMultisets.isLabelMultisetType(n5, node.getPath());
	  
	  final AffineTransform3D transform = N5SingleScaleMetadataParser.buildTransform(downsamplingFactors, resolution, Optional.empty());
	  
	  final Optional<double[]> offsetOpt = Optional.ofNullable(n5.getAttribute(node.getPath(), offsetKey, double[].class));

	  final double[] offset;
	  if (offsetOpt.isPresent()) {
		offset = offsetOpt.get();
		for (int i = 0; i < offset.length; i++)
		  transform.set(offset[i], i, 3);
	  } else {
		offset = new double[3];
		for (int i = 0; i < offset.length; i++)
		  offset[i] = transform.get(i, 3);
	  }

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
