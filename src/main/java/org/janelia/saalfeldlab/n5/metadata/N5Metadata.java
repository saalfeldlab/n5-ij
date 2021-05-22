package org.janelia.saalfeldlab.n5.metadata;

public interface N5Metadata {

  /**
   * @return the path to this metadata, with respect to the base of the container
   */
  String getPath();

  default String getName() {

	String[] split = getPath().split("/");
	return split[split.length - 1];
  }
}
