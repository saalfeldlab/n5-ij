/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.ScaleAndTranslation;

public class ImagePlusMetadataTemplate extends AbstractN5DatasetMetadata
	implements ImageplusMetadata< ImagePlusMetadataTemplate >, SpatialMetadata
{
	public int numDims;
	public int numSpatialDims;

	public int numChannels;
	public int numFrames;
	public int numSlices;

	public String name;

	public double xResolution;
	public double yResolution;
	public double zResolution;
	public double tResolution;

	public double xOrigin;
	public double yOrigin;
	public double zOrigin;
	public double tOrigin;

	public String xUnit;
	public String yUnit;
	public String zUnit;
	public String tUnit;

	public String globalUnit;

	public Map<String,String> otherMetadata;

	public ImagePlusMetadataTemplate( ) {
		super("", null);
	}

	public ImagePlusMetadataTemplate( final String path, final ImagePlus imp ) {
		this( path, imp, null );
	}

	public ImagePlusMetadataTemplate( final String path, final DatasetAttributes attributes ) {
		this( path, null, attributes );
	}

	public ImagePlusMetadataTemplate( final String path, final ImagePlus imp, final DatasetAttributes attributes ) {

		super( path, attributes );

		numChannels = imp.getNChannels();
		numFrames = imp.getNFrames();
		numSlices = imp.getNSlices();

		numDims = imp.getNDimensions();
		numSpatialDims = numSlices > 1 ? 3 : 2;

		name = imp.getTitle();

		xResolution = imp.getCalibration().pixelWidth;
		yResolution = imp.getCalibration().pixelHeight;
		zResolution = imp.getCalibration().pixelDepth;
		tResolution = imp.getCalibration().frameInterval;

		xOrigin = imp.getCalibration().xOrigin;
		yOrigin = imp.getCalibration().yOrigin;
		zOrigin = imp.getCalibration().zOrigin;
		tOrigin = 0.0;

		xUnit = imp.getCalibration().getXUnit();
		yUnit = imp.getCalibration().getYUnit();
		zUnit = imp.getCalibration().getZUnit();
		tUnit = imp.getCalibration().getTimeUnit();

		globalUnit = imp.getCalibration().getUnit();

		otherMetadata = new HashMap<>();
		final Properties props = imp.getProperties();
		if ( props != null )
			for ( final Object k : props.keySet() )
				otherMetadata.put( k.toString(), props.get( k ).toString() );
	}

	@Override
	public void writeMetadata( final ImagePlusMetadataTemplate t, final ImagePlus ip )
	{
		ip.setTitle( t.name );
		ip.setDimensions(numChannels, numSlices, numFrames);

		ip.getCalibration().pixelWidth = t.xResolution;
		ip.getCalibration().pixelDepth = t.yResolution;
		ip.getCalibration().pixelHeight = t.zResolution;

		ip.getCalibration().xOrigin = t.xOrigin;
		ip.getCalibration().yOrigin = t.yOrigin;
		ip.getCalibration().zOrigin = t.zOrigin;

		ip.getCalibration().setXUnit( t.xUnit );
		ip.getCalibration().setYUnit( t.yUnit );
		ip.getCalibration().setZUnit( t.zUnit );
		ip.getCalibration().setUnit( t.globalUnit );

		ip.getCalibration().setTimeUnit( t.tUnit );

		final Properties props = ip.getProperties();
		if( t.otherMetadata != null )
			for( final String k : t.otherMetadata.keySet() )
				props.put( k, t.otherMetadata.get( k ));

	}

	@Override
	public ImagePlusMetadataTemplate readMetadata(ImagePlus ip) throws IOException {

		return new ImagePlusMetadataTemplate( "", ip, null );
	}

	public static ImagePlusMetadataTemplate readMetadataStatic(ImagePlus ip) throws IOException {

		return new ImagePlusMetadataTemplate( "", ip, null );
	}

	@Override
	public AffineGet spatialTransform() {

		if( numSpatialDims == 3 )
			return new ScaleAndTranslation(
					new double[] {xResolution, yResolution, zResolution},
					new double[] {xOrigin, yOrigin, zOrigin });
		else
			return new ScaleAndTranslation(
					new double[] {xResolution, yResolution },
					new double[] {xOrigin, yOrigin });
	}

	@Override
	public String unit() {

		return globalUnit;
	}

}
