package org.janelia.saalfeldlab.n5.metadata.axes;

import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

/**
 * Default implementation of {@link AxisMetadata}.
 *
 * @author John Bogovic
 *
 */
public class DefaultAxisMetadata implements AxisMetadata, N5Metadata {

	private final String path;

	private final String[] labels;

	private final String[] types;

	private final String[] units;

	public DefaultAxisMetadata(String path, String[] labels, String[] types, String[] units ) {
		this.path = path;
		this.labels = labels;
		this.types = types;
		this.units = units;
	}

	@Override
	public String[] getAxisLabels() {
		return labels;
	}
	
	@Override
	public String[] getAxisTypes() {
		return types;
	}

	@Override
	public String[] getUnits() {
		return units;
	}

	@Override
	public String getPath() {
		return path;
	}

}
