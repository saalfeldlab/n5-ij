package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;

import java.io.IOException;

public abstract class SpatialMetadataToImagePlus<T extends SpatialMetadata & N5DatasetMetadata> implements ImageplusMetadata<T> {

  @Override
  public void writeMetadata(final T t, final ImagePlus ip) throws IOException {

	AffineTransform3D xfm = t.spatialTransform3d();

	ip.setTitle( t.getPath() );
	final Calibration cal = ip.getCalibration();
	cal.pixelWidth = xfm.get(0, 0);
	cal.pixelHeight = xfm.get(1, 1);
	cal.pixelDepth = xfm.get(2, 2);
	cal.setUnit(t.unit());

	cal.xOrigin = xfm.get(0, 3);
	cal.yOrigin = xfm.get(1, 3);
	cal.zOrigin = xfm.get(2, 3);

	final int nd = t.getAttributes().getNumDimensions();
	final long[] dims = t.getAttributes().getDimensions();

	if (nd == 3)
	  ip.setDimensions(1, (int)dims[2], 1);
	else if (nd == 4)
	  ip.setDimensions((int)dims[3], (int)dims[2], 1);
  }
	
	@Override
	public abstract T readMetadata(final ImagePlus ip) throws IOException;

}
