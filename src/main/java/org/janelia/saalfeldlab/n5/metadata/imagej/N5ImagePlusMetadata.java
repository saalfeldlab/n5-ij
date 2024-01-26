/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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