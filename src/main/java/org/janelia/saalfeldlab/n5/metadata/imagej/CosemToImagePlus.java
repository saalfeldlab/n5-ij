package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;

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
		final int[] spatialIndexes = spatialIndexes( t.getCosemTransform().axes );

		final CosemTransform transform = t.getCosemTransform();
		cal.pixelWidth = spatialIndexes[0] > -1 ? transform.scale[spatialIndexes[0]] : 1;
		cal.pixelHeight = spatialIndexes[1] > -1 ? transform.scale[spatialIndexes[1]] : 1;
		cal.pixelDepth = spatialIndexes[2] > -1 ? transform.scale[spatialIndexes[2]] : 1;

		cal.xOrigin = spatialIndexes[0] > -1 ? transform.translate[spatialIndexes[0]] : 0 ;
		cal.yOrigin = spatialIndexes[1] > -1 ? transform.translate[spatialIndexes[1]] : 0 ;
		cal.zOrigin = spatialIndexes[2] > -1 ? transform.translate[spatialIndexes[2]] : 0 ;

		cal.setUnit(t.unit());

		if (nd == 3)
			ip.setDimensions(1, (int) dims[2], 1);
		else if (nd == 4)
			ip.setDimensions(1, (int) dims[2], (int) dims[4]);
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
	final double[] scale = new double[nd];
	Arrays.fill(scale, 1);

	final double[] translation = new double[nd];

	int k = nd-1;
	scale[k] = imp.getCalibration().pixelWidth;
	translation[k] = imp.getCalibration().xOrigin;
	axes[k--]="x";

	scale[k] = imp.getCalibration().pixelHeight;
	translation[k] = imp.getCalibration().yOrigin;
	axes[k--]="y";

	if (imp.getNChannels() > 1) {
	  axes[k--]="c";
	}
	if (imp.getNSlices() > 1) {
	  scale[k] = imp.getCalibration().pixelDepth;
	  translation[k] = imp.getCalibration().zOrigin;
	  axes[k--]="z";
	}
	if (imp.getNFrames() > 1) {
	  axes[k--]="t";
	}

	// unit
	final String[] units = new String[nd];
	Arrays.fill(units, imp.getCalibration().getUnit());

	//TODO what to do about DatasetAttributes?
	return new N5CosemMetadata("",
			new CosemTransform(axes, scale, translation, units),
			new DatasetAttributes(new long[]{}, imp.getDimensions(), DataType.FLOAT32, new GzipCompression()));
  }

  private int[] spatialIndexes( final String[] axes ) {
	final int[] spaceIndexes = new int[3];
	Arrays.fill(spaceIndexes, -1);

	// COSEM scales and translations are in c-order
	// but detect the axis types to be extra safe
	for( int i = 0; i < axes.length; i++ )
	{
		if( axes[i].equals("x"))
			spaceIndexes[0] = i;
		else if( axes[i].equals("y"))
			spaceIndexes[1] = i;
		else if( axes[i].equals("z"))
			spaceIndexes[2] = i;
	}
	return spaceIndexes;
  }

}
