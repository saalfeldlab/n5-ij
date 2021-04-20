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
package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;

public class ImagePlusMetadataTemplate extends AbstractN5Metadata<ImagePlusMetadataTemplate> 
	implements ImageplusMetadata< ImagePlusMetadataTemplate >
{
	public final String name;

	public final double xResolution;
	public final double yResolution;
	public final double zResolution;
	public final double tResolution;

	public final double xOrigin;
	public final double yOrigin;
	public final double zOrigin;
	public final double tOrigin;

	public final String xUnit;
	public final String yUnit;
	public final String zUnit;
	public final String tUnit;

	public final String globalUnit;

	public final String axis0;
	public final String axis1;
	public final String axis2;
	public final String axis3;
	public final String axis4;

	public final Map<String,String> otherMetadata;

	public ImagePlusMetadataTemplate( final String path )
	{
		super( path );

		name = "";

		xResolution = -1;
		yResolution = -1;
		zResolution = -1;
		tResolution = -1;

		xOrigin = -1;
		yOrigin = -1;
		zOrigin = -1;
		tOrigin = -1;

		xUnit = "";
		yUnit = "";
		zUnit = "";
		tUnit = "";

		globalUnit = "";

		axis0 = "x";
		axis1 = "y";
		axis2 = "c";
		axis3 = "z";
		axis4 = "t";

		otherMetadata = new HashMap<>();
	}

	public ImagePlusMetadataTemplate( final String path, final ImagePlus imp )
	{
		this( path, imp, null );
	}

	public ImagePlusMetadataTemplate( final String path, final ImagePlus imp, final DatasetAttributes attributes )
	{
		super( path, attributes );
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

		axis0 = "x";
		axis1 = "y";
		axis2 = "c";
		axis3 = "z";
		axis4 = "t";

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
	public ImagePlusMetadataTemplate readMetadata( final ImagePlus ip )
	{
		return new ImagePlusMetadataTemplate( "", ip );
	}

	@Override
	public void writeMetadata( ImagePlusMetadataTemplate t, N5Writer n5, String dataset ) throws Exception
	{
		// TODO Auto-generated method stub
	}

	@Override
	public ImagePlusMetadataTemplate parseMetadata( N5Reader n5, String dataset ) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
