package org.janelia.saalfeldlab.n5.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

import ij.ImagePlus;

public class ImagePlusMetadataTemplate extends  AbstractN5Metadata implements 
	ImageplusMetadata< ImagePlusMetadataTemplate >
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
		Properties props = imp.getProperties();
		if ( props != null )
			for ( Object k : props.keySet() )
				otherMetadata.put( k.toString(), props.get( k ).toString() );

	}

	@Override
	public void writeMetadata( ImagePlusMetadataTemplate t, ImagePlus ip )
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

		Properties props = ip.getProperties();
		if( t.otherMetadata != null )
			for( String k : t.otherMetadata.keySet() )
				props.put( k, t.otherMetadata.get( k ));

	}

	@Override
	public ImagePlusMetadataTemplate readMetadata( ImagePlus ip )
	{
		return new ImagePlusMetadataTemplate( "", ip );
	}

}
