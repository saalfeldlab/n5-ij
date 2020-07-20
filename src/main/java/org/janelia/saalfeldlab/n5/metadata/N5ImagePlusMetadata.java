package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineTransform3D;

public class N5ImagePlusMetadata extends N5SingleScaleMetadata implements N5Metadata, ImageplusMetadata<N5ImagePlusMetadata>, 
	N5MetadataWriter< N5ImagePlusMetadata >, N5MetadataParser< N5ImagePlusMetadata >
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

	private String unit;
	
	private Map< String, Object > properties;

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
	
	private <T> Optional<T> readAttribute( final N5Reader n5, final String pathName, final String key, Class<T> clazz )
	{
		try
		{
			T obj = n5.getAttribute(pathName, key, clazz );
			return Optional.ofNullable( obj );
		}
		catch (IOException e) { }

		return Optional.empty();
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
		t.name = ip.getTitle();

		t.fps = cal.fps;
		t.frameInterval = cal.frameInterval;
		t.pixelWidth = cal.pixelWidth;
		t.pixelHeight = cal.pixelHeight;
		t.pixelDepth = cal.pixelDepth;
		t.unit = cal.getUnit();
		
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
	public N5ImagePlusMetadata parseMetadata( N5Reader n5, N5TreeNode node ) throws Exception
	{
		AffineTransform3D xfm = new AffineTransform3D();
		N5ImagePlusMetadata meta = new N5ImagePlusMetadata( node.path, xfm );
		if( !node.isDataset )
		{
			throw new Exception( "Can't read from " + node.path + ".  Must be dataset." );
		}
		String dataset = node.path;

		meta.name = readAttribute( n5, dataset, titleKey, String.class ).orElse( "ImagePlus" );

		meta.pixelWidth = readAttribute( n5, dataset, pixelWidthKey, Double.class ).orElse( 1.0 );
		meta.pixelHeight = readAttribute( n5, dataset, pixelHeightKey, Double.class ).orElse( 1.0 );
		meta.pixelDepth = readAttribute( n5, dataset, pixelDepthKey, Double.class ).orElse( 1.0 );
		meta.unit = readAttribute( n5, dataset, pixelUnitKey, String.class ).orElse( "pixel" );

		meta.xOrigin = readAttribute( n5, dataset, xOriginKey, Double.class ).orElse( 0.0 );
		meta.yOrigin = readAttribute( n5, dataset, yOriginKey, Double.class ).orElse( 0.0 );
		meta.zOrigin = readAttribute( n5, dataset, zOriginKey, Double.class ).orElse( 0.0 );

		meta.fps = readAttribute( n5, dataset, fpsKey, Double.class ).orElse( 1.0 );
		meta.frameInterval = readAttribute( n5, dataset, fpsKey, Double.class ).orElse( 1.0 );
		
		Optional< Properties > readProperties = readAttribute( n5, dataset, imagePropertiesKey, Properties.class );
		if ( readProperties.isPresent() )
		{
			Properties rprops = readProperties.get();

			meta.properties = new HashMap< String, Object >();
			for ( Object k : rprops.keySet() )
				meta.properties.put( k.toString(), rprops.get( k ) );
		}

		return meta;
	}

	@Override
	public void writeMetadata( N5ImagePlusMetadata t, N5Writer n5, String dataset ) throws Exception
	{
		if( !n5.datasetExists( dataset ))
		{
			throw new Exception( "Can't write into " + dataset + ".  Must be dataset." );
		}

		n5.setAttribute( dataset, titleKey, name );

		n5.setAttribute( dataset, fpsKey, fps );
		n5.setAttribute( dataset, frameIntervalKey, frameInterval );
		n5.setAttribute( dataset, pixelWidthKey, pixelWidth );
		n5.setAttribute( dataset, pixelHeightKey, pixelHeight );
		n5.setAttribute( dataset, pixelDepthKey, pixelDepth );
		n5.setAttribute( dataset, pixelUnitKey, unit );

		n5.setAttribute( dataset, xOriginKey, xOrigin );
		n5.setAttribute( dataset, yOriginKey, yOrigin );
		n5.setAttribute( dataset, zOriginKey, zOrigin );

		if ( properties != null )
		{
			for ( Object k : properties.keySet() )
			{
				try
				{
					n5.setAttribute( dataset, k.toString(), properties.get( k ).toString() );
				}
				catch ( Exception e )
				{}
			}
		}	
	}

}
