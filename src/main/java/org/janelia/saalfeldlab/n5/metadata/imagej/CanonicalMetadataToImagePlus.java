package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineTransform3D;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.MultiResolutionSpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;

import java.io.IOException;

public class CanonicalMetadataToImagePlus implements ImageplusMetadata<CanonicalDatasetMetadata> {

  @Override
  public void writeMetadata(final CanonicalDatasetMetadata t, final ImagePlus ip) throws IOException {

	final AffineTransform3D xfm = t.getSpatialTransform().spatialTransform3d();
	final String unit = t.getSpatialTransform().unit();

	ip.setTitle( t.getPath() );
	final Calibration cal = ip.getCalibration();
	cal.pixelWidth = xfm.get(0, 0);
	cal.pixelHeight = xfm.get(1, 1);
	cal.pixelDepth = xfm.get(2, 2);
	cal.setUnit(unit);

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
	public CanonicalDatasetMetadata readMetadata(final ImagePlus imp) throws IOException
	{
		int nd = 2;
		if (imp.getNSlices() > 1) {
		  nd++;
		}
		
		double[] params;
		if( nd == 2 )
			params = new double[] {};
		else if( nd == 3 )
			params = new double[] {};
		else
			return null;

		// TODO what to about attrs?
		DatasetAttributes attributes = new DatasetAttributes(
				new long[]{0, 0, 0},
				imp.getDimensions(),
				DataType.FLOAT32,
				new GzipCompression());

		final SpatialMetadataCanonical spatialMeta = new SpatialMetadataCanonical(
				"", 
				new AffineSpatialTransform( params ),
				imp.getCalibration().getUnit());

		return new CanonicalDatasetMetadata( "", spatialMeta, null, attributes );
	}

}
