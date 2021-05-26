package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;

import java.io.IOException;
import java.util.Arrays;

public class CosemToImagePlus extends PhysicalMetadataToImagePlus<N5CosemMetadata> {

  @Override
  public N5CosemMetadata readMetadata(final ImagePlus imp) throws IOException {

	int nd = 2;
	if (imp.getNChannels() > 1) {
	  nd++;
	}
	if (imp.getNSlices() > 1) {
	  nd++;
	}
	if (imp.getNFrames() > 1) {
	  nd++;
	}

	final String[] axes = new String[nd];
	if (nd == 2) {
	  axes[0] = "y";
	  axes[1] = "x";
	} else if (nd == 3) {
	  axes[0] = "z";
	  axes[1] = "y";
	  axes[2] = "x";
	}

	// unit
	final String[] units = new String[nd];
	Arrays.fill(units, imp.getCalibration().getUnit());

	final double[] scale = new double[nd];
	final double[] translation = new double[nd];

	if (nd == 2) {
	  scale[0] = imp.getCalibration().pixelHeight;
	  scale[1] = imp.getCalibration().pixelWidth;

	  translation[0] = imp.getCalibration().yOrigin;
	  translation[1] = imp.getCalibration().xOrigin;
	} else if (nd == 3) {
	  scale[0] = imp.getCalibration().pixelDepth;
	  scale[1] = imp.getCalibration().pixelHeight;
	  scale[2] = imp.getCalibration().pixelWidth;

	  translation[2] = imp.getCalibration().zOrigin;
	  translation[1] = imp.getCalibration().yOrigin;
	  translation[0] = imp.getCalibration().xOrigin;
	}

	//TODO what to do about DatasetAttributes?
	return new N5CosemMetadata("",
			new CosemTransform(axes, scale, translation, units),
			new DatasetAttributes(new long[]{0, 0, 0}, imp.getDimensions(), DataType.FLOAT32, new GzipCompression()));
  }
}
