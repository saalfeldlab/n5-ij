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
package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.DatasetAttributes;

import java.util.Objects;
import java.util.stream.Stream;

public class N5SingleScaleMetadata extends AbstractN5DatasetMetadata implements PhysicalMetadata {

  public final AffineTransform3D transform;
  final double[] downsamplingFactors;
  final double[] pixelResolution;
  final double[] offset;

  public final String unit;

  public N5SingleScaleMetadata(final String path, final AffineTransform3D transform,
		  final double[] downsamplingFactors,
		  final double[] pixelResolution,
		  final double[] offset,
		  final String unit,
		  final DatasetAttributes attributes) {

	super(path, attributes);

	Objects.requireNonNull(path);
	Objects.requireNonNull(transform);
	Objects.requireNonNull(downsamplingFactors);
	Objects.requireNonNull(pixelResolution);
	Objects.requireNonNull(offset);
	this.transform = transform;
	this.downsamplingFactors = downsamplingFactors;
	this.pixelResolution = pixelResolution;
	this.offset = offset;

	if (unit == null)
	  this.unit = "pixel";
	else
	  this.unit = unit;
  }

  @Override
  public AffineGet physicalTransform() {

	return transform;
  }

  @Override
  public String[] units() {

	return Stream.generate(() -> unit).limit(3).toArray(String[]::new);
  }

}
