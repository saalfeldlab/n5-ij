package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

import java.util.Map;
import java.util.Optional;

import static org.janelia.saalfeldlab.n5.metadata.MultiscaleMetadata.IS_LABEL_MULTISET_KEY;

public class N5GenericSingleScaleMetadataParser extends AbstractN5DatasetMetadataParser<N5GenericSingleScaleMetadata> {

  public static final String MIN = "min";
  public static final String MAX = "max";
  public static final String RESOLUTION = "resolution";
  public static final String OFFSET = "offset";
  public static final String IS_LABEL_MULTISET = "isLabelMultiset";
  public static final String DOWNSAMPLING_FACTORS = "downsamplingFactors";

  {
	keysToTypes.put(MIN, double.class);
	keysToTypes.put(MAX, double.class);
	keysToTypes.put(RESOLUTION, double[].class);
	keysToTypes.put(OFFSET, double[].class);
	keysToTypes.put(DOWNSAMPLING_FACTORS, double[].class);
	keysToTypes.put(IS_LABEL_MULTISET, boolean.class);
  }

  @Override
  public boolean check(final Map<String, Object> metaMap) {

	final Map<String, Class<?>> requiredKeys = AbstractN5DatasetMetadataParser.datasetAtttributeKeys();
	for (final String k : requiredKeys.keySet()) {
	  if (!metaMap.containsKey(k))
		return false;
	  else if (metaMap.get(k) == null)
		return false;
	}

	return true;
  }

  @Override
  public Optional<N5GenericSingleScaleMetadata> parseMetadata(final Map<String, Object> metaMap) {

	final DatasetAttributes attributes = N5MetadataParser.parseAttributes(metaMap);
	if (attributes == null)
	  return Optional.empty();

	final String path = Optional.ofNullable(metaMap.get("dataset")).map(String.class::cast).get();

	final double min = Optional.ofNullable(metaMap.get(MIN))
			.filter(Double.class::isInstance)
			.map(double.class::cast)
			.orElse(0.0);

	final double max = Optional.ofNullable(metaMap.get(MAX))
			.filter(Double.class::isInstance)
			.map(double.class::cast)
			.orElseGet(() -> PainteraBaseMetadata.maxForDataType(attributes.getDataType()));

	final double[] resolution = Optional.ofNullable(metaMap.get(RESOLUTION))
			.filter(double[].class::isInstance)
			.map(double[].class::cast)
			.orElse(new double[]{1.0, 1.0, 1.0});

	if (resolution.length != 3) {
	  return Optional.empty();
	}

	final double[] offset = Optional.ofNullable(metaMap.get(OFFSET))
			.filter(double[].class::isInstance)
			.map(double[].class::cast)
			.orElse(new double[]{1.0, 1.0, 1.0});

	final double[] downsamplingFactors = Optional.ofNullable(metaMap.get(DOWNSAMPLING_FACTORS))
			.filter(double[].class::isInstance)
			.map(double[].class::cast)
			.orElse(new double[]{1.0, 1.0, 1.0});

	final Boolean isLabelMultiset = Optional.ofNullable(metaMap.get(IS_LABEL_MULTISET_KEY))
			.filter(Boolean.class::isInstance)
			.map(Boolean.class::cast)
			.orElse(false);

	N5GenericSingleScaleMetadata metadata = new N5GenericSingleScaleMetadata(path, attributes, min, max, resolution, offset, downsamplingFactors, isLabelMultiset);
	if (metadata.offset.length != 3)
	  return Optional.empty();

	return Optional.of(metadata);
  }
}
