package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;

public class CanonicalDatasetMetadata extends AbstractN5DatasetMetadata implements CanonicalMetadata, N5DatasetMetadata {

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes) {
		super( path, attributes);
	}

}
