package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;

import ij.ImagePlus;
import ij.measure.Calibration;

public class CosemToImagePlus extends SpatialMetadataToImagePlus<N5CosemMetadata> {

	private boolean includeChannelAxis = false;

	public void includeChannelAxis(boolean includeChannelAxis) {

		this.includeChannelAxis = includeChannelAxis;
	}

	@Override
	public void writeMetadata(final N5CosemMetadata t, final ImagePlus ip) throws IOException {

		ip.setTitle(t.getPath());
		final Calibration cal = ip.getCalibration();

		// dims are aligned with t.getAxes()
		final long[] dims = ArrayUtils.clone(t.getAttributes().getDimensions());
		// now they're aligned with t.getCosmTransform.axes
		ArrayUtils.reverse(dims);

		// spatial indices are aligned with t.getCosemTransform().axes);
		final int[] spatialIndexes = spatialIndexes(t.getCosemTransform().axes);

		final CosemTransform transform = t.getCosemTransform();
		cal.pixelWidth = spatialIndexes[0] > -1 ? transform.scale[spatialIndexes[0]] : 1;
		cal.pixelHeight = spatialIndexes[1] > -1 ? transform.scale[spatialIndexes[1]] : 1;
		cal.pixelDepth = spatialIndexes[2] > -1 ? transform.scale[spatialIndexes[2]] : 1;

		cal.xOrigin = spatialIndexes[0] > -1 ? transform.translate[spatialIndexes[0]] : 0;
		cal.yOrigin = spatialIndexes[1] > -1 ? transform.translate[spatialIndexes[1]] : 0;
		cal.zOrigin = spatialIndexes[2] > -1 ? transform.translate[spatialIndexes[2]] : 0;

		cal.setUnit(t.unit());

		final int ci = channelIndex(t.getAxisLabels());
		final int nc = ci == -1 ? 1 : (int)dims[ci];

		final int ti = timeIndex(transform.axes);
		final int nt = ti == -1 ? 1 : (int)dims[ti];
		if (ti != -1)
			cal.frameInterval = transform.scale[ti];

		final int nz = spatialIndexes.length > 2 && spatialIndexes[2] != -1 ? (int)dims[spatialIndexes[2]] : 1;

		// ip.setDimensions(nc, nz, nt);
	}

	@Override
	public N5CosemMetadata readMetadata(final ImagePlus imp) throws IOException {

		int nd = 2;
		if (includeChannelAxis && imp.getNChannels() > 1) {
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

		int k = nd - 1;
		scale[k] = imp.getCalibration().pixelWidth;
		translation[k] = imp.getCalibration().xOrigin;
		axes[k--] = "x";

		scale[k] = imp.getCalibration().pixelHeight;
		translation[k] = imp.getCalibration().yOrigin;
		axes[k--] = "y";

		if (includeChannelAxis && imp.getNChannels() > 1) {
			axes[k--] = "c";
		}
		if (imp.getNSlices() > 1) {
			scale[k] = imp.getCalibration().pixelDepth;
			translation[k] = imp.getCalibration().zOrigin;
			axes[k--] = "z";
		}
		if (imp.getNFrames() > 1) {
			axes[k--] = "t";
		}

		// unit
		final String[] units = new String[nd];
		Arrays.fill(units, imp.getCalibration().getUnit());

		return new N5CosemMetadata("",
				new CosemTransform(axes, scale, translation, units),
				ImageplusMetadata.datasetAttributes(imp));
	}

	private int[] spatialIndexes(final String[] axes) {

		final int[] spaceIndexes = new int[3];
		Arrays.fill(spaceIndexes, -1);

		// COSEM scales and translations are in c-order
		// but detect the axis types to be extra safe
		for (int i = 0; i < axes.length; i++) {
			if (axes[i].equals("x"))
				spaceIndexes[0] = i;
			else if (axes[i].equals("y"))
				spaceIndexes[1] = i;
			else if (axes[i].equals("z"))
				spaceIndexes[2] = i;
		}
		return spaceIndexes;
	}

	private int timeIndex(final String[] axes) {

		for (int i = 0; i < axes.length; i++) {
			if (axes[i].equals("t"))
				return i;
		}
		return -1;
	}

	private int channelIndex(final String[] axes) {

		for (int i = 0; i < axes.length; i++) {
			if (axes[i].equals("c"))
				return i;
		}
		return -1;
	}

}
