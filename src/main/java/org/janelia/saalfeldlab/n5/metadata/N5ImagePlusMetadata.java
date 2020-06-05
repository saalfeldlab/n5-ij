package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

import ij.ImagePlus;
import ij.measure.Calibration;

public class N5ImagePlusMetadata implements N5Metadata< ImagePlus >
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

	public void metadataFromN5( N5Reader n5, String dataset, ImagePlus imp ) throws IOException
	{
		imp.setTitle( readAttribute( n5, dataset, titleKey, String.class ).orElse( "ImagePlus" ));

		imp.getCalibration().pixelWidth = readAttribute( n5, dataset, pixelWidthKey, Double.class ).orElse( 1.0 );
		imp.getCalibration().pixelHeight = readAttribute( n5, dataset, pixelHeightKey, Double.class ).orElse( 1.0 );
		imp.getCalibration().pixelDepth = readAttribute( n5, dataset, pixelDepthKey, Double.class ).orElse( 1.0 );
		imp.getCalibration().setUnit( readAttribute( n5, dataset, pixelUnitKey, String.class ).orElse( "pixel" ));

		imp.getCalibration().xOrigin = readAttribute( n5, dataset, xOriginKey, Double.class ).orElse( 0.0 );
		imp.getCalibration().yOrigin = readAttribute( n5, dataset, yOriginKey, Double.class ).orElse( 0.0 );
		imp.getCalibration().zOrigin = readAttribute( n5, dataset, zOriginKey, Double.class ).orElse( 0.0 );

		imp.getCalibration().fps = readAttribute( n5, dataset, fpsKey, Double.class ).orElse( 1.0 );
		imp.getCalibration().frameInterval = readAttribute( n5, dataset, fpsKey, Double.class ).orElse( 1.0 );
		
		int numChannels = imp.getNChannels();
		int numSlices = imp.getNSlices();
		int numFrames = imp.getNFrames();
		numChannels = readAttribute( n5, dataset, numChannelsKey, Integer.class ).orElse( numChannels );
		numSlices = readAttribute( n5, dataset, numSlicesKey, Integer.class ).orElse( numSlices );
		numFrames = readAttribute( n5, dataset, numFramesKey, Integer.class ).orElse( numFrames );
		imp.setDimensions( numChannels, numSlices, numFrames );

		Optional<Properties> readProperties = readAttribute( n5, dataset, imagePropertiesKey, Properties.class );
		if( readProperties.isPresent())
		{
			Properties rprops = readProperties.get();
			for( Object k : rprops.keySet() )
				imp.setProperty( k.toString(), rprops.get( k ));
		}
	}

}
