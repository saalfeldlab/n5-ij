package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataGroup;

public class CanonicalMultichannelMetadata extends CanonicalMetadata implements N5MetadataGroup<CanonicalMetadata> {

	public CanonicalMultichannelMetadata(final String path, 
			final SpatialMetadataCanonical spatialTransform,
			final MultiResolutionSpatialMetadataCanonical multiscales,
			final MultiChannelMetadataCanonical multichannels,
			final DatasetAttributes attributes) {
		super( path, spatialTransform, multiscales, multichannels, attributes );
	}

	@Override
	public String[] getPaths() {
		return super.getMultichannels().getPaths();
	}

	@Override
	public CanonicalMetadata[] getChildrenMetadata() {
		return super.getMultichannels().getChildrenMetadata();
	}

}
