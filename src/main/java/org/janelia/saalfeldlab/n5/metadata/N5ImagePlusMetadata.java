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
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.ScaleAndTranslation;

public class N5ImagePlusMetadata extends AbstractN5Metadata<N5ImagePlusMetadata>
	implements ImageplusMetadata<N5ImagePlusMetadata>, PhysicalMetadata
{
	public static final String titleKey = "title";
	public static final String fpsKey = "fps";
	public static final String frameIntervalKey = "frameInterval";
	public static final String pixelWidthKey = "pixelWidth";
	public static final String pixelHeightKey = "pixelHeight";
	public static final String pixelDepthKey = "pixelDepth";
	public static final String pixelUnitKey = "pixelUnit";
	public static final String xOriginKey = "xOrigin";
	public static final String yOriginKey = "yOrigin";
	public static final String zOriginKey = "zOrigin";

	public static final String numChannelsKey = "numChannels";
	public static final String numSlicesKey = "numSlices";
	public static final String numFramesKey = "numFrames";

	public static final String typeKey = "ImagePlusType";

	public static final String imagePropertiesKey = "imageProperties";

	public static final String downsamplingFactorsKey = "downsamplingFactors";

	private String name;

	private double fps;
	private double frameInterval;

	private double pixelWidth;
	private double pixelHeight;
	private double pixelDepth;
	private double xOrigin;
	private double yOrigin;
	private double zOrigin;

	private int numChannels;
	private int numSlices;
	private int numFrames;

	private int type;
	private String unit;

	private Map< String, Object > properties;

	public N5ImagePlusMetadata( final String path )
	{
		this( path, "pixel" );
	}

	public N5ImagePlusMetadata( final String path, final DatasetAttributes attributes )
	{
		super( path, attributes );
	}

	public N5ImagePlusMetadata( final String path, final String unit, final double... resolution )
	{
		this( path, unit, null, resolution );
	}

	public N5ImagePlusMetadata( final String path, final String unit, final DatasetAttributes attributes,
			final double... resolution )
	{
		super( path, attributes );
		this.unit = unit;
		pixelWidth = 1.0;
		pixelHeight = 1.0;
		pixelDepth = 1.0;

		if( resolution.length > 0 )
			pixelWidth = resolution[ 0 ];

		if( resolution.length > 1 )
			pixelWidth = resolution[ 1 ];

		if( resolution.length > 2 )
			pixelDepth = resolution[ 2 ];
	}
	

	public N5ImagePlusMetadata( final String path, final DatasetAttributes attributes,
			final String name, final double fps, final double frameInterval,
			final String unit,
			final Double pixelWidth, final Double pixelHeight, final Double pixelDepth,
			final Double xOrigin, final Double yOrigin, final Double zOrigin, 
			final Integer numChannels, final Integer numSlices, final Integer numFrames,
			final Integer type)
	{
		super( path, attributes );

		this.fps = Objects.requireNonNull( fps, "fps must be non null");
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
		this.type = type;
	}

	public void crop( final Interval cropInterval )
	{
		int i = 2;
		if( numChannels > 1 )
			numChannels = (int)cropInterval.dimension( i++ );

		if( numSlices > 1 )
			numSlices = (int)cropInterval.dimension( i++ );

		if( numFrames > 1 )
			numFrames = (int)cropInterval.dimension( i++ );
	}

	public static double[] getPixelSpacing( final N5Reader n5, final String dataset ) throws IOException
	{
		final double rx = n5.getAttribute( dataset, pixelWidthKey, double.class );
		final double ry = n5.getAttribute( dataset, pixelHeightKey, double.class );
		final double rz = n5.getAttribute( dataset, pixelDepthKey, double.class );
		return new double[] { rx, ry, rz };
	}

	public int getType()
	{
		return type;
	}

	@Override
	public void writeMetadata( final N5ImagePlusMetadata t, final ImagePlus ip ) throws IOException
	{
		ip.setTitle( t.name );

		final Calibration cal = ip.getCalibration();
		cal.fps = t.fps;
		cal.frameInterval = t.frameInterval ;
		cal.pixelWidth = t.pixelWidth;
		cal.pixelHeight = t.pixelHeight;
		cal.pixelDepth = t.pixelDepth;
		cal.setUnit( t.unit );

		cal.xOrigin = t.xOrigin;
		cal.yOrigin = t.yOrigin;
		cal.zOrigin = t.zOrigin;

		ip.setDimensions( t.numChannels, t.numSlices, t.numFrames );

		final Properties props = ip.getProperties();
		if ( properties != null )
		{
			for ( final String k : properties.keySet() )
			{
				try
				{
					props.put( k, properties.get( k ));
				}
				catch ( final Exception e ){}
			}
		}
	}

	@Override
	public N5ImagePlusMetadata readMetadata( final ImagePlus ip ) throws IOException
	{
		final Calibration cal = ip.getCalibration();

		final AffineTransform3D xfm = new AffineTransform3D();
		final N5ImagePlusMetadata t = new N5ImagePlusMetadata( "" );

		if( ip.getTitle() == null )
			t.name = "ImagePlus";
		else
			t.name = ip.getTitle();

		t.fps = cal.fps;
		t.frameInterval = cal.frameInterval;
		t.pixelWidth = cal.pixelWidth;
		t.pixelHeight = cal.pixelHeight;
		t.pixelDepth = cal.pixelDepth;
		t.unit = cal.getUnit();

		t.numChannels = ip.getNChannels();
		t.numSlices = ip.getNSlices();
		t.numFrames = ip.getNFrames();

		t.type = ip.getType();

		xfm.set( t.pixelWidth, 0, 0 );
		xfm.set( t.pixelHeight, 1, 1 );
		xfm.set( t.pixelDepth, 2, 2 );

		t.xOrigin = cal.xOrigin;
		t.yOrigin = cal.yOrigin;
		t.zOrigin = cal.zOrigin;

		final Properties props = ip.getProperties();
		if ( props != null )
		{
			properties = new HashMap< String, Object >();
			for ( final Object k : props.keySet() )
			{
				try
				{
					properties.put( k.toString(), props.get( k ) );
				}
				catch ( final Exception e )
				{}
			}
		}
		return t;
	}


	@Override
	public N5ImagePlusMetadata parseMetadata( N5Reader n5, String dataset ) throws IOException
	{

		final DatasetAttributes attributes = n5.getDatasetAttributes( dataset );
		if( attributes == null )
			return null;
		

		String name = n5.getAttribute( dataset, titleKey, String.class );

		Double pixelWidth = n5.getAttribute( dataset, pixelWidthKey, Double.class);
		Double pixelHeight = n5.getAttribute( dataset, pixelHeightKey, Double.class);
		Double pixelDepth = n5.getAttribute( dataset, pixelDepthKey, Double.class );
		String unit = n5.getAttribute( dataset, pixelUnitKey, String.class );

		Double xOrigin = n5.getAttribute( dataset, xOriginKey, Double.class );
		Double yOrigin = n5.getAttribute( dataset, yOriginKey, Double.class );
		Double zOrigin = n5.getAttribute( dataset, zOriginKey, Double.class );

		Integer numChannels = n5.getAttribute( dataset, numChannelsKey, Integer.class );
		Integer numSlices = n5.getAttribute( dataset, numSlicesKey, Integer.class );
		Integer numFrames = n5.getAttribute( dataset, numFramesKey, Integer.class );

		Double fps = n5.getAttribute( dataset, fpsKey, Double.class );
		Double frameInterval = n5.getAttribute( dataset, fpsKey, Double.class);

		Integer type = n5.getAttribute( dataset, typeKey, Integer.class );
		
		try {
		final N5ImagePlusMetadata meta = new N5ImagePlusMetadata( dataset, attributes,
				name, fps, frameInterval,
				unit,
				pixelWidth, pixelHeight, pixelDepth,
				xOrigin, yOrigin, zOrigin, 
				numChannels, numSlices, numFrames,
				type);
			return meta;
		}
		catch( Exception e )
		{
			return null;
		}

	}

	@Override
	public void writeMetadata( final N5ImagePlusMetadata t, final N5Writer n5, final String dataset ) throws Exception
	{
		if( !n5.datasetExists( dataset ))
			throw new Exception( "Can't write into " + dataset + ".  Must be a dataset." );
		
		HashMap<String, Object> attrs = new HashMap<>();
		attrs.put( titleKey, t.name );

		attrs.put( fpsKey, t.fps );
		attrs.put( frameIntervalKey, t.frameInterval );
		attrs.put( pixelWidthKey, t.pixelWidth );
		attrs.put( pixelHeightKey, t.pixelHeight );
		attrs.put( pixelDepthKey, t.pixelDepth );
		attrs.put( pixelUnitKey, t.unit );

		attrs.put( xOriginKey, t.xOrigin );
		attrs.put( yOriginKey, t.yOrigin );
		attrs.put( zOriginKey, t.zOrigin );

		attrs.put( numChannelsKey, t.numChannels );
		attrs.put( numSlicesKey, t.numSlices );
		attrs.put( numFramesKey, t.numFrames );

		attrs.put( typeKey, t.type );

		if ( t.properties != null )
		{
			for ( final Object k : t.properties.keySet() )
			{
				try
				{
					attrs.put( k.toString(), t.properties.get( k ).toString() );
				}
				catch ( final Exception e )
				{}
			}
		}

		n5.setAttributes( dataset, attrs );
	}
	
	public AffineGet physicalTransform()
	{
		final int nd = numSlices > 1 ? 3 : 2;
		final double[] spacing = new double[ nd ];
		final double[] offset = new double[ nd ];

		spacing[ 0 ] = pixelWidth;
		spacing[ 1 ] = pixelHeight;
		if( numSlices > 1 )
			spacing[ 2 ] = pixelDepth;

		offset[ 0 ] = xOrigin;
		offset[ 1 ] = yOrigin;
		if( numSlices > 1 )
			offset[ 2 ] = zOrigin;

		return new ScaleAndTranslation( spacing, offset );
	}
	
	public String[] units()
	{
		final int nd = numSlices > 1 ? 3 : 2;
		return Stream.generate( () -> unit ).limit( nd ).toArray( String[]::new );
	}
	
}