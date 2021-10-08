package org.janelia.saalfeldlab.n5.metadata.canonical;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.IntensityMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;

public class CanonicalDatasetMetadata extends AbstractN5DatasetMetadata implements CanonicalMetadata, N5DatasetMetadata, IntensityMetadata {

	private IntensityLimits intensityLimits;

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes,
			final double min, final double max ) {
		super( path, attributes);
		intensityLimits = new IntensityLimits( min, max );
	}

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes,
			final IntensityLimits limits ) {
		super( path, attributes);
		intensityLimits = limits;
	}

	public CanonicalDatasetMetadata(final String path,
			final DatasetAttributes attributes) {
		super( path, attributes );
		intensityLimits = null;
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

	protected static class IntensityLimits {
		public final double min;
		public final double max;
		public IntensityLimits( double min, double max ) {
			this.min = min;
			this.max = max;
		}
	}

}
