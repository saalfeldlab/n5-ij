package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineTransform3D;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;

import java.io.IOException;
import java.util.Arrays;

public class CosemToImagePlus extends SpatialMetadataToImagePlus<N5CosemMetadata> {

	@Override
	public void writeMetadata(final N5CosemMetadata t, final ImagePlus ip) throws IOException {

		ip.setTitle(t.getPath());
		final Calibration cal = ip.getCalibration();

		final int nd = t.getAttributes().getNumDimensions();
		final long[] dims = t.getAttributes().getDimensions();

		final CosemTransform transform = t.getCosemTransform();
		if( nd == 2 )
		{
			cal.pixelWidth = transform.scale[1];
			cal.pixelHeight = transform.scale[0];
			cal.pixelDepth = 1;

			cal.xOrigin = transform.translate[1];
			cal.yOrigin = transform.translate[0];
			cal.zOrigin = 0;
		}
		else if( nd == 3 )
		{
			cal.pixelWidth = transform.scale[2];
			cal.pixelHeight = transform.scale[1];
			cal.pixelDepth = transform.scale[0];

			cal.xOrigin = transform.translate[2];
			cal.yOrigin = transform.translate[1];
			cal.zOrigin = transform.translate[0];
		}

		cal.setUnit(t.unit());

		if (nd == 3)
			ip.setDimensions(1, (int) dims[2], 1);
		else if (nd == 4)
			ip.setDimensions((int) dims[3], (int) dims[2], 1);
	}

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

	final double[] scale = new double[3];
	final double[] translation = new double[3];

	if (nd == 2) {
	  scale[0] = imp.getCalibration().pixelHeight;
	  scale[1] = imp.getCalibration().pixelWidth;
	  scale[2] = 1;

	  translation[0] = imp.getCalibration().yOrigin;
	  translation[1] = imp.getCalibration().xOrigin;
	  translation[2] = 0;
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
			new DatasetAttributes(new long[]{}, imp.getDimensions(), DataType.FLOAT32, new GzipCompression()));
  }
}
