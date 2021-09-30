package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;

public class CanonicalSpatialDatasetMetadata extends CanonicalDatasetMetadata implements CanonicalMetadata, N5DatasetMetadata {

	private final SpatialMetadataCanonical spatialTransform;

	public CanonicalSpatialDatasetMetadata(final String path,
			final SpatialMetadataCanonical spatialTransform,
			final DatasetAttributes attributes) {
		super( path, attributes);
		this.spatialTransform = spatialTransform;
	}

	public SpatialMetadataCanonical getSpatialTransform() {
		return spatialTransform;
	}

}
