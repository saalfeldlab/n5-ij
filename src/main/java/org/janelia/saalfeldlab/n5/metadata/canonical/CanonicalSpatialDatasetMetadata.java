package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisMetadata;

public class CanonicalSpatialDatasetMetadata extends CanonicalDatasetMetadata implements CanonicalMetadata, N5DatasetMetadata, AxisMetadata {

	private final SpatialMetadataCanonical spatialTransform;

	public CanonicalSpatialDatasetMetadata(final String path,
			final SpatialMetadataCanonical spatialTransform,
			final DatasetAttributes attributes) {
		super( path, attributes);
		this.spatialTransform = spatialTransform;
	}

	public CanonicalSpatialDatasetMetadata(final String path,
			final SpatialMetadataCanonical spatialTransform,
			final DatasetAttributes attributes,
			final IntensityLimits limits) {
		super( path, attributes, limits );
		this.spatialTransform = spatialTransform;
	}

	public SpatialMetadataCanonical getSpatialTransform() {
		return spatialTransform;
	}

	@Override
	public String[] getAxisLabels() {
		return spatialTransform.getAxisLabels();
	}

	@Override
	public String[] getAxisTypes() {
		return spatialTransform.getAxisTypes();
	}

	@Override
	public String[] getUnits() {
		return spatialTransform.getUnits();
	}

}
