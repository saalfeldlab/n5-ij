package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.metadata.axes.DefaultDatasetAxisMetadata;

import java.io.IOException;

public class ImagePlusAxes implements ImageplusMetadata<DefaultDatasetAxisMetadata> {

	@Override
	public DefaultDatasetAxisMetadata readMetadata(final ImagePlus imp) throws IOException {

		int nd = imp.getNDimensions();
		final String[] axes = new String[nd];
		final String[] units = new String[nd];

		axes[0] = "x";
		axes[1] = "y";

		units[0] = imp.getCalibration().getXUnit();
		units[1] = imp.getCalibration().getYUnit();

		int i = 2;
		if (imp.getNChannels() > 1) {
			axes[i] = "c";
			units[i] = "null";
			i++;
		}

		if (imp.getNSlices() > 1) {
			axes[i] = "z";
			units[i] = imp.getCalibration().getZUnit();
			i++;
		}

		if (imp.getNFrames() > 1) {
			axes[i] = "t";
			units[i] = imp.getCalibration().getTimeUnit();
			i++;
		}

		// TODO what to do about DatasetAttributes?
		return new DefaultDatasetAxisMetadata("", axes, AxisUtils.getDefaultTypes(axes), units,
				new DatasetAttributes( new long[] { 0, 0, 0 }, imp.getDimensions(), DataType.FLOAT32, new GzipCompression()));
	}

	@Override
	public void writeMetadata(DefaultDatasetAxisMetadata t, ImagePlus ip) throws IOException {
		// TODO nothing to do here because the permutations implemented
		// in N5Importer should cover the appropriate cases
		// revisit if necessary
//		System.out.println("ImagePlusAxes writeMetadata");
	}
}
