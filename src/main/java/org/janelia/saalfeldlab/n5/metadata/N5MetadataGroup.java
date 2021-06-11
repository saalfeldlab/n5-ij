package org.janelia.saalfeldlab.n5.metadata;

/**
 * Base interfaces for metadata corresponding to an n5 group.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5MetadataGroup<T extends N5Metadata> extends N5Metadata {

  /**
   * @return an array of the paths to each of the children in this group, with respect to the base of the container
   */
  String[] getPaths();

  /**
   * Note: This is NOT gauranteeed to be sorted. Sort with
   *
   * @return an array of the metadata for the children of this group
   */
  T[] getChildrenMetadata();
}
