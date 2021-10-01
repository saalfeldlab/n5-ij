package org.janelia.saalfeldlab.n5.metadata.axes;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;

/**
 * Default implementation of {@link AxisMetadata}.
 *
 * @author John Bogovic
 *
 */
public class DefaultDatasetAxisMetadata extends DefaultAxisMetadata implements N5DatasetMetadata {

	private final DatasetAttributes attributes;

	public DefaultDatasetAxisMetadata(String path, String[] labels, String[] types, String[] units,
			final DatasetAttributes attributes ) {
		super( path, labels, types, units );
		this.attributes = attributes;
	}

	@Override
	public DatasetAttributes getAttributes() {
		return attributes;
	}


}
