package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadataGroup;

public class CanonicalMultiscaleMetadata extends CanonicalMetadata implements SpatialMetadataGroup<CalibratedTransformMetadata> {

	public CanonicalMultiscaleMetadata(final String path, 
			final SpatialMetadataCanonical spatialTransform,
			final MultiResolutionSpatialMetadataCanonical multiscales,
			final MultiChannelMetadataCanonical multichannels,
			final DatasetAttributes attributes) {
		super( path, spatialTransform, multiscales, multichannels, attributes );
	}

	@Override
	public String[] getPaths() {
		return super.getMultiscales().getPaths();
	}

	@Override
	public CalibratedTransformMetadata[] getChildrenMetadata() {
		return super.getMultiscales().getChildrenMetadata();
	}

	@Override
	public String[] units() {
		return super.getMultiscales().units();
	}
}
