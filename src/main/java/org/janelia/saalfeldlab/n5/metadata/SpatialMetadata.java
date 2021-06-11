package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface SpatialMetadata extends N5Metadata {

  /**
   * @return the transformation from pixel to physical space.
   */
  AffineGet spatialTransform();

  String unit();

  default AffineTransform3D spatialTransform3d() {

	final AffineGet transform = spatialTransform();

	// return identity if null
	if (transform == null)
	  return new AffineTransform3D();
	else if (transform instanceof AffineTransform3D)
	  return (AffineTransform3D)transform;
	else {
	  final int N = transform.numSourceDimensions();

	  int k = 0;
	  final AffineTransform3D transform3d = new AffineTransform3D();
	  final double[] params = transform3d.getRowPackedCopy();
	  for (int i = 0; i < 3; i++) {
		for (int j = 0; j < 3; j++) {
		  if (i < N && j < N)
			params[k] = transform.get(i, j);

		  k++;
		}

		// j == 4
		if (i < N)
		  params[k] = transform.get(i, N);

		k++;
	  }

	  transform3d.set(params);
	  return transform3d;
	}
  }

}
