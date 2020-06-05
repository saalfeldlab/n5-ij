package org.janelia.saalfeldlab.n5.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ij.ImagePlus;

public class ImagePlusMetadataTemplate
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
	
	public ImagePlusMetadataTemplate( final ImagePlus imp )
	{
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
		Properties props = imp.getProperties();
		if ( props != null )
			for ( Object k : props.keySet() )
				otherMetadata.put( k.toString(), props.get( k ).toString() );

	}
	
	public String toSpec( final String spec )
	{
		return "face";
	}
}
