package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Interface for a metadata whose children are each {@link SpatialMetadata}.
 * <p>
 * The children metadata are usually related in some way. For example, a
 * {@link MultiscaleMetadata} is a set of SpatialMetadata where each
 * child is a resampling of the same underlying data at a different spatial 
 * resolution.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface SpatialMetadataGroup<T extends SpatialMetadata> extends N5MetadataGroup<T> {

  default AffineGet[] spatialTransforms() {

	return Arrays.stream(getChildrenMetadata()).map(SpatialMetadata::spatialTransform).toArray(AffineGet[]::new);
  }

  String[] units();

  default AffineTransform3D[] spatialTransforms3d() {

	final List<AffineTransform3D> transforms = new ArrayList<>();
	for (final AffineGet transform : spatialTransforms()) {
	  // return identity if null
	  if (transform == null) {
		transforms.add(new AffineTransform3D());
	  } else if (transform instanceof AffineTransform3D)
		transforms.add((AffineTransform3D)transform);
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
		transforms.add(transform3d);
	  }
	}
	return transforms.toArray(new AffineTransform3D[]{});
  }
}
