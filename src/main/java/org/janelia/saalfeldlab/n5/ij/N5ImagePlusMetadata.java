package org.janelia.saalfeldlab.n5.ij;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

import ij.ImagePlus;
import ij.measure.Calibration;

public class N5ImagePlusMetadata {

	protected static final String titleKey = "title";
	protected static final String fpsKey = "fps";
	protected static final String frameIntervalKey = "frameInterval";
	protected static final String pixelWidthKey = "pixelWidth";
	protected static final String pixelHeightKey = "pixelHeight";
	protected static final String pixelDepthKey = "pixelDepth";
	protected static final String pixelUnitKey = "pixelUnit";
	protected static final String xOriginKey = "xOrigin";
	protected static final String yOriginKey = "yOrigin";
	protected static final String zOriginKey = "zOrigin";

	protected static final String downsamplingFactorsKey = "downsamplingFactors";

	public static void writeMetadata(N5Writer n5, String dataset, ImagePlus imp) throws IOException {
		Calibration cal = imp.getCalibration();

		n5.setAttribute(dataset, titleKey, imp.getTitle());

		n5.setAttribute(dataset, fpsKey, cal.fps);
		n5.setAttribute(dataset, frameIntervalKey, cal.frameInterval);
		n5.setAttribute(dataset, pixelWidthKey, cal.pixelWidth);
		n5.setAttribute(dataset, pixelHeightKey, cal.pixelHeight);
		n5.setAttribute(dataset, pixelDepthKey, cal.pixelDepth);
		n5.setAttribute(dataset, pixelUnitKey, cal.getUnit());

		n5.setAttribute(dataset, xOriginKey, cal.xOrigin);
		n5.setAttribute(dataset, yOriginKey, cal.yOrigin);
		n5.setAttribute(dataset, zOriginKey, cal.zOrigin);

	}

	public static void readMetadata( N5Reader n5, String dataset, ImagePlus imp ) throws IOException
	{
		imp.setTitle( n5.getAttribute( dataset, titleKey, String.class ));

		imp.getCalibration().fps = n5.getAttribute( dataset, fpsKey, double.class);
		imp.getCalibration().frameInterval = n5.getAttribute( dataset, fpsKey, double.class);

		imp.getCalibration().pixelWidth = n5.getAttribute( dataset, pixelWidthKey, double.class);
		imp.getCalibration().pixelHeight = n5.getAttribute( dataset, pixelHeightKey, double.class);
		imp.getCalibration().pixelDepth = n5.getAttribute( dataset, pixelDepthKey, double.class);
		imp.getCalibration().setUnit( n5.getAttribute( dataset, pixelUnitKey, String.class));

		imp.getCalibration().xOrigin = n5.getAttribute( dataset, xOriginKey, double.class);
		imp.getCalibration().yOrigin = n5.getAttribute( dataset, yOriginKey, double.class);
		imp.getCalibration().zOrigin = n5.getAttribute( dataset, zOriginKey, double.class);
	
	}
}
