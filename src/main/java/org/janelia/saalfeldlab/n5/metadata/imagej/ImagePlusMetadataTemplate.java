package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;

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
