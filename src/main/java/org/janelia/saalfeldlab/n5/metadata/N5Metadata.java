package org.janelia.saalfeldlab.n5.metadata;

/**
 * Base interfaces for metadata stored with N5.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5Metadata {

  /**
   * @return the path to this metadata, with respect to the base of the container
   */
  String getPath();

  /**
   * @return the name of the dataset or group corresponding to
   */
  default String getName() {

	String[] split = getPath().split("/");
	return split[split.length - 1];
  }
}
