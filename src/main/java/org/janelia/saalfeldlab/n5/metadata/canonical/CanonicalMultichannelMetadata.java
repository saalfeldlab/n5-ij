package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataGroup;

public class CanonicalMultichannelMetadata extends AbstractN5Metadata implements CanonicalMetadata, N5MetadataGroup<CanonicalMetadata> {

	private final MultiChannelMetadataCanonical multichannels;

	public CanonicalMultichannelMetadata(final String path, 
			final MultiChannelMetadataCanonical multichannels) {
		super( path );
		this.multichannels = multichannels;
	}

	@Override
	public String[] getPaths() {
		return multichannels.getPaths();
	}

	@Override
	public CanonicalMetadata[] getChildrenMetadata() {
		return multichannels.getChildrenMetadata();
	}

}
