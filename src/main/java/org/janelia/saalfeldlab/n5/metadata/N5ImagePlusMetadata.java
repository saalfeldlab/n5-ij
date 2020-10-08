package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineTransform3D;

public class N5ImagePlusMetadata extends N5SingleScaleMetadata implements N5Metadata, ImageplusMetadata<N5ImagePlusMetadata>, 
	N5MetadataWriter< N5ImagePlusMetadata >, N5GsonMetadataParser< N5ImagePlusMetadata >
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

	private String unit;
	
	private Map< String, Object > properties;

	private HashMap< String, Class< ? > > keysToTypes;

	public N5ImagePlusMetadata( String path )
	{
		this( path, new AffineTransform3D() );
	}

	public N5ImagePlusMetadata( String path, AffineTransform3D transform )
	{
		super( path, transform );
		pixelWidth = transform.get( 0, 0 );
		pixelHeight = transform.get( 1, 1 );
		pixelDepth = transform.get( 2, 2 );

		keysToTypes = new HashMap<>();
		keysToTypes.put( titleKey, String.class );
		keysToTypes.put( pixelWidthKey, Double.class );
		keysToTypes.put( pixelHeightKey, Double.class );
		keysToTypes.put( pixelDepthKey, Double.class );
		keysToTypes.put( pixelUnitKey, String.class );
		keysToTypes.put( xOriginKey, Double.class );
		keysToTypes.put( yOriginKey, Double.class );
		keysToTypes.put( zOriginKey, Double.class );
		keysToTypes.put( fpsKey, Double.class );
	}

	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	public void metadataToN5( ImagePlus imp, N5Writer n5, String dataset ) throws IOException
	{
		Calibration cal = imp.getCalibration();

		n5.setAttribute( dataset, titleKey, imp.getTitle() );

		n5.setAttribute( dataset, fpsKey, cal.fps );
		n5.setAttribute( dataset, frameIntervalKey, cal.frameInterval );
		n5.setAttribute( dataset, pixelWidthKey, cal.pixelWidth );
		n5.setAttribute( dataset, pixelHeightKey, cal.pixelHeight );
		n5.setAttribute( dataset, pixelDepthKey, cal.pixelDepth );
		n5.setAttribute( dataset, pixelUnitKey, cal.getUnit() );

		n5.setAttribute( dataset, xOriginKey, cal.xOrigin );
		n5.setAttribute( dataset, yOriginKey, cal.yOrigin );
		n5.setAttribute( dataset, zOriginKey, cal.zOrigin );

		n5.setAttribute( dataset, imagePropertiesKey, imp.getProperties() );

		Properties props = imp.getProperties();
		if ( props != null )
		{
			for ( Object k : props.keySet() )
			{
				try
				{
					n5.setAttribute( dataset, k.toString(), props.get( k ).toString() );
				}
				catch ( Exception e )
				{}
			}
		}
	}

	public static double[] getPixelSpacing( final N5Reader n5, final String dataset ) throws IOException
	{
		double rx = n5.getAttribute( dataset, pixelWidthKey, double.class );
		double ry = n5.getAttribute( dataset, pixelHeightKey, double.class );
		double rz = n5.getAttribute( dataset, pixelDepthKey, double.class );
		return new double[] { rx, ry, rz };
	}

	@Override
	public void writeMetadata( N5ImagePlusMetadata t, ImagePlus ip ) throws IOException
	{
		Calibration cal = ip.getCalibration();

		ip.setTitle( t.name );

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

		Properties props = ip.getProperties();
		if ( properties != null )
		{
			for ( String k : properties.keySet() )
			{
				try
				{
					props.put( k, properties.get( k ));
				}
				catch ( Exception e ){}
			}
		}
	}

	@Override
	public N5ImagePlusMetadata readMetadata( ImagePlus ip ) throws IOException
	{
		Calibration cal = ip.getCalibration();

		AffineTransform3D xfm = new AffineTransform3D();
		N5ImagePlusMetadata t = new N5ImagePlusMetadata( "", xfm );

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
		
		xfm.set( t.pixelWidth, 0, 0 );
		xfm.set( t.pixelHeight, 1, 1 );
		xfm.set( t.pixelDepth, 2, 2 );

		t.xOrigin = cal.xOrigin;
		t.yOrigin = cal.yOrigin;
		t.zOrigin = cal.zOrigin;

		Properties props = ip.getProperties();
		if ( props != null )
		{
			properties = new HashMap< String, Object >();
			for ( Object k : props.keySet() )
			{
				try
				{
					properties.put( k.toString(), props.get( k ) );
				}
				catch ( Exception e )
				{}
			}
		}
		return t;
	}

	@Override
	public N5ImagePlusMetadata parseMetadata( final Map< String, Object > metaMap ) throws Exception
	{
		if( !N5MetadataParser.hasRequiredKeys( keysToTypes(), metaMap ))
			throw new Exception( "Could not parse as N5ImagePlusMetadata.");

		String dataset = ( String ) metaMap.get( "dataset" );

		N5ImagePlusMetadata meta = new N5ImagePlusMetadata( dataset );
		meta.name = ( String ) metaMap.get( titleKey );

		meta.pixelWidth = ( Double ) metaMap.get( pixelWidthKey );
		meta.pixelHeight = ( Double ) metaMap.get( pixelHeightKey );
		meta.pixelDepth = ( Double ) metaMap.get( pixelDepthKey );
		meta.unit = ( String ) metaMap.get( pixelUnitKey );

		meta.xOrigin = ( Double ) metaMap.get( xOriginKey );
		meta.yOrigin = ( Double ) metaMap.get( yOriginKey );
		meta.zOrigin = ( Double ) metaMap.get( zOriginKey );
		
		meta.fps = ( Double ) metaMap.get( fpsKey );
		meta.frameInterval = ( Double ) metaMap.get( fpsKey );

		return meta;
	}

	@Override
	public void writeMetadata( N5ImagePlusMetadata t, N5Writer n5, String dataset ) throws Exception
	{
		if( !n5.datasetExists( dataset ))
		{
			throw new Exception( "Can't write into " + dataset + ".  Must be dataset." );
		}

		n5.setAttribute( dataset, titleKey, t.name );

		n5.setAttribute( dataset, fpsKey, t.fps );
		n5.setAttribute( dataset, frameIntervalKey, t.frameInterval );
		n5.setAttribute( dataset, pixelWidthKey, t.pixelWidth );
		n5.setAttribute( dataset, pixelHeightKey, t.pixelHeight );
		n5.setAttribute( dataset, pixelDepthKey, t.pixelDepth );
		n5.setAttribute( dataset, pixelUnitKey, t.unit );

		n5.setAttribute( dataset, xOriginKey, t.xOrigin );
		n5.setAttribute( dataset, yOriginKey, t.yOrigin );
		n5.setAttribute( dataset, zOriginKey, t.zOrigin );

		if ( t.properties != null )
		{
			for ( Object k : t.properties.keySet() )
			{
				try
				{
					n5.setAttribute( dataset, k.toString(), t.properties.get( k ).toString() );
				}
				catch ( Exception e )
				{}
			}
		}	
	}

}
