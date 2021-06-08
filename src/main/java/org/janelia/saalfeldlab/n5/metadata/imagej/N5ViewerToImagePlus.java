package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadataParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class N5ViewerToImagePlus extends SpatialMetadataToImagePlus<N5SingleScaleMetadata> {

  @Override
  public N5SingleScaleMetadata readMetadata(final ImagePlus imp) throws IOException {

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

	final double[] downsamplingFactors = new double[]{1, 1, 1};
	final AffineTransform3D transform = N5SingleScaleMetadataParser.buildTransform(downsamplingFactors, scale, Optional.empty());
	return new N5SingleScaleMetadata("", transform, downsamplingFactors,
			scale, translation, units[0],
			null);
  }

}
