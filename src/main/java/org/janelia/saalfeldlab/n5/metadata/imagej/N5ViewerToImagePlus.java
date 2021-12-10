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

	final int nd = (imp.getNSlices() > 1) ? 3 : 2;

	// unit
	final String[] units = new String[nd];
	Arrays.fill(units, imp.getCalibration().getUnit());

	final double[] scale = new double[3];
	final double[] translation = new double[3];

	if (nd == 2) {
	  scale[0] = imp.getCalibration().pixelWidth;
	  scale[1] = imp.getCalibration().pixelHeight;
	  scale[2] = 1;

	  translation[0] = imp.getCalibration().xOrigin;
	  translation[1] = imp.getCalibration().yOrigin;
	  translation[2] = 0;

	} else if (nd == 3) {
	  scale[0] = imp.getCalibration().pixelWidth;
	  scale[1] = imp.getCalibration().pixelHeight;
	  scale[2] = imp.getCalibration().pixelDepth;

	  translation[0] = imp.getCalibration().xOrigin;
	  translation[1] = imp.getCalibration().yOrigin;
	  translation[2] = imp.getCalibration().zOrigin;
	}

	final double[] downsamplingFactors = new double[]{1, 1, 1};
	final AffineTransform3D transform = N5SingleScaleMetadataParser.buildTransform(downsamplingFactors, scale, Optional.empty());
	return new N5SingleScaleMetadata("", transform, downsamplingFactors,
			scale, translation, units[0],
			null);
  }

}
