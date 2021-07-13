package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;

public class CanonicalDatasetMetadata extends CanonicalMetadata implements N5DatasetMetadata {

	public CanonicalDatasetMetadata(final String path, final SpatialMetadataCanonical spatialTransform,
			final MultiResolutionSpatialMetadataCanonical multiscales, final DatasetAttributes attributes) {
		super( path, spatialTransform, multiscales, attributes );
	}
}
