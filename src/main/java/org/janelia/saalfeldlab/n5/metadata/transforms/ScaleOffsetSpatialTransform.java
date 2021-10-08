package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.ScaleAndTranslation;

public class ScaleOffsetSpatialTransform extends AbstractLinearSpatialTransform<double[][]> {

	public double[] scale;

	public double[] offset;

	public transient AffineGet transform;

	public ScaleOffsetSpatialTransform(final double[] scale, final double[] offset) {
		super("scale_offset");
		this.scale = scale;
		this.offset = offset;
		buildTransform(scale, offset);
	}

	public ScaleOffsetSpatialTransform(final N5Reader n5, final String path) {
		super("scale_offset", path);
		double[][] p = getParameters(n5);
		buildTransform(p);
	}

	public ScaleOffsetSpatialTransform(final String path) {
		super("scale_offset", path);
		this.scale = null;
		this.offset = null;
	}

	public AffineGet buildTransform(double[] scale, double[] offset) {
		transform = new ScaleAndTranslation(scale, offset);
		return transform;
	}

	@Override
	public AffineGet buildTransform(double[][] parameters) {
		this.scale = parameters[0];
		this.offset = parameters[1];
		transform = new ScaleAndTranslation(scale, offset);
		return transform;
	}

	@Override
	public AffineGet getTransform() {
		if( scale != null && offset != null && transform == null )
			transform = new ScaleAndTranslation(scale, offset);

		return transform;
	}

	@Override
	public double[][] getParameters(N5Reader n5) {
		return getDoubleArray2(n5, getParameterPath());
	}
	
}
