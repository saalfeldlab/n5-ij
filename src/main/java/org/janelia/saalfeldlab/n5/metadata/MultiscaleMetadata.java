package org.janelia.saalfeldlab.n5.metadata;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * An abstract class for a {@link SpatialMetadataGroup} whose children contain
 * the same data sampled at different resolutions.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 *
 * @param <T> metadata type
 */
public abstract class MultiscaleMetadata<T extends N5DatasetMetadata & SpatialMetadata> implements SpatialMetadataGroup<T> {

  static final Predicate<String> scaleLevelPredicate = Pattern.compile("^s\\d+$").asPredicate();

  final private String basePath;

  final private String[] paths;

  final private String[] units;

  final protected T[] childrenMetadata;

  public MultiscaleMetadata(final String basePath, final T[] childrenMetadata) {

	Objects.requireNonNull(basePath);
	Objects.requireNonNull(childrenMetadata);

	this.basePath = basePath;
	this.childrenMetadata = childrenMetadata;

	final int N = childrenMetadata.length;
	paths = new String[N];
	units = new String[N];

	int i = 0;
	for (T meta : childrenMetadata) {
	  Objects.requireNonNull(meta);
	  paths[i] = meta.getPath();
	  units[i] = meta.unit();
	  i++;
	}
  }

  @Override public String getPath() {

	return basePath;
  }

  @Override public String[] getPaths() {

	return paths;
  }

  @Override public T[] getChildrenMetadata() {

	return childrenMetadata;
  }

  @Override
  public String[] units() {

	return units;
  }

  /**
   * Sort the array according to scale level; If not all metadata are scale sets, no sorting is done.
   * All metadata names as returned by {@code N5Metadata::getName()} should be of the form sN.
   *
   * @param metadataToBeSorted array of the unsorted scale metadata to be sorted
   * @param <T>                the type of the metadata
   */
  public static <T extends N5DatasetMetadata> boolean sortScaleMetadata(T[] metadataToBeSorted) {

	final boolean allAreScaleSets = Arrays.stream(metadataToBeSorted).allMatch(x -> x.getName().matches("^s\\d+$"));
	if (!allAreScaleSets)
	  return false;

	Arrays.sort(metadataToBeSorted, Comparator.comparingInt(s -> Integer.parseInt(s.getName().replaceAll("[^\\d]", ""))));
	return true;
  }

}
