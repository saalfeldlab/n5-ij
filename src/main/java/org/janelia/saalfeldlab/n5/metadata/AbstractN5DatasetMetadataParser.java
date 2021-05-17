package org.janelia.saalfeldlab.n5.metadata;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractN5DatasetMetadataParser<T extends N5DatasetMetadata> extends AbstractN5Metadata.AbstractN5MetadataParser<T> {

  protected final HashMap<String, Class<?>> keysToTypes = new HashMap<>(requiredDatasetAttributes());

  public static Map<String, Class<?>> datasetAtttributeKeys() {

	return new HashMap<>(requiredDatasetAttributes());
  }

  public static Map<String, Class<?>> requiredDatasetAttributes() {

	final HashMap<String, Class<?>> map = new HashMap<>();

	map.put("dimensions", long[].class);
	map.put("blockSize", int[].class);
	map.put("dataType", String.class);
	return map;
  }

  @Override public HashMap<String, Class<?>> keysToTypes() {

	return keysToTypes;
  }
}
