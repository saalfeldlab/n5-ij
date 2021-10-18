package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.metadata.IntensityMetadata;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata.IntensityLimits;

public class CanonicalSpatialMetadata extends AbstractN5Metadata implements CanonicalMetadata, AxisMetadata, IntensityMetadata {

	private final SpatialMetadataCanonical spatialTransform;

	private IntensityLimits intensityLimits;

	public CanonicalSpatialMetadata(final String path,
			final SpatialMetadataCanonical spatialTransform) {
		super( path );
		this.spatialTransform = spatialTransform;
	}

	public CanonicalSpatialMetadata(final String path,
			final SpatialMetadataCanonical spatialTransform,
			final IntensityLimits limits) {
		super( path );
		this.spatialTransform = spatialTransform;
		this.intensityLimits = limits;
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

	@Override
	public DatasetAttributes getAttributes() {
		return null;
	}

	/**
	 * @return the minimum intensity value of the data
	 */
	@Override
	public double minIntensity() {
		return intensityLimits == null ? 0 : intensityLimits.min;
	}

	/**
	 * @return the maximum intensity value of the data
	 */
	@Override
	public double maxIntensity() {
		return intensityLimits == null ? 
				IntensityMetadata.maxForDataType(getAttributes().getDataType()) :
					intensityLimits.max;
	}
}
