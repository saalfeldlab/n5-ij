package org.janelia.saalfeldlab.n5.metadata.imagej;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.ScaleAndTranslation;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;

import java.util.Map;
import java.util.Objects;

public class N5ImagePlusMetadata extends AbstractN5DatasetMetadata implements SpatialMetadata, AxisMetadata {

  public final String name;

  public final double fps;
  public final double frameInterval;

  public final double pixelWidth;
  public final double pixelHeight;
  public final double pixelDepth;
  public final double xOrigin;
  public final double yOrigin;
  public final double zOrigin;

  public final int numChannels;
  public final int numSlices;
  public final int numFrames;

  public final int type;
  public final String unit;

  public final Map<String, Object> properties;

  private transient Axis[] axes;

	public N5ImagePlusMetadata(final String path, final DatasetAttributes attributes, final String name,
			final double fps, final double frameInterval, final String unit, final Double pixelWidth,
			final Double pixelHeight, final Double pixelDepth, final Double xOrigin, final Double yOrigin,
			final Double zOrigin, final Integer numChannels, final Integer numSlices, final Integer numFrames,
			final Integer type, final Map<String, Object> properties) {

		super(path, attributes);

		this.name = name;
		this.fps = Objects.requireNonNull(fps, "fps must be non null");
		this.frameInterval = Objects.requireNonNull(frameInterval, "frameInterval must be non null");

		this.unit = Objects.requireNonNull(unit, "unit must be non null");
		this.pixelWidth = Objects.requireNonNull(pixelWidth, "pixelWidth must be non null");
		this.pixelHeight = Objects.requireNonNull(pixelHeight, "pixelHeight must be non null");
		this.pixelDepth = Objects.requireNonNull(pixelDepth, "pixelDepth must be non null");

		this.xOrigin = Objects.requireNonNull(xOrigin, "xOrigin must be non null");
		this.yOrigin = Objects.requireNonNull(yOrigin, "yOrigin must be non null");
		this.zOrigin = Objects.requireNonNull(zOrigin, "zOrigin must be non null");

		this.numChannels = Objects.requireNonNull(numChannels, "numChannels must be non null");
		this.numSlices = Objects.requireNonNull(numSlices, "numSlices must be non null");
		this.numFrames = Objects.requireNonNull(numFrames, "numFrames must be non null");

		// type is not required and so may be null
		if (type == null)
			this.type = -1;
		else
			this.type = type;

		this.properties = properties;

		axes = buildAxes();
	}

	private Axis[] buildAxes() {

		int nd = 2;
		if( numChannels > 1 )
			nd++;

		if( numSlices > 1 )
			nd++;

		if( numFrames > 1 )
			nd++;

		axes = new Axis[nd];
		axes[0]  = new Axis(Axis.SPACE, "x", unit);
		axes[1]  = new Axis(Axis.SPACE, "y", unit);

		int i = 2;
		if( numChannels > 1 )
			axes[i++] = new Axis(Axis.CHANNEL, "c", "");

		if( numSlices > 1 )
			axes[i++] = new Axis(Axis.SPACE, "z", unit);

		if( numFrames > 1 )
			axes[i++] = new Axis(Axis.TIME, "t", "sec");

		return axes;
	}

	public int getType() {

		return type;
	}

	@Override
	public AffineGet spatialTransform() {

		final int nd = numSlices > 1 ? 3 : 2;
		final double[] spacing = new double[nd];
		final double[] offset = new double[nd];

		spacing[0] = pixelWidth;
		spacing[1] = pixelHeight;
		if (numSlices > 1)
			spacing[2] = pixelDepth;

		offset[0] = xOrigin;
		offset[1] = yOrigin;
		if (numSlices > 1)
			offset[2] = zOrigin;

		return new ScaleAndTranslation(spacing, offset);
	}

	@Override
	public String unit() {

		return unit;
	}

	@Override
	public Axis[] getAxes() {

		return axes;
	}

}