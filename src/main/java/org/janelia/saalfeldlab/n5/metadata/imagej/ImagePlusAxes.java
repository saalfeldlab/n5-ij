package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.metadata.axes.DefaultDatasetAxisMetadata;

import java.io.IOException;
import java.util.Arrays;

public class ImagePlusAxes implements ImageplusMetadata<DefaultDatasetAxisMetadata> {

	@Override
	public DefaultDatasetAxisMetadata readMetadata(final ImagePlus imp) throws IOException {

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

		// TODO what to do about DatasetAttributes?
		return new DefaultDatasetAxisMetadata("", axes, AxisUtils.getDefaultTypes(axes), new DatasetAttributes(
				new long[] { 0, 0, 0 }, imp.getDimensions(), DataType.FLOAT32, new GzipCompression()));
	}

	@Override
	public void writeMetadata(DefaultDatasetAxisMetadata t, ImagePlus ip) throws IOException {
		// TODO nothing to do here because the permutations implemented
		// in N5Importer should cover the appropriate cases
		// revisit if necessary
//		System.out.println("ImagePlusAxes writeMetadata");
	}
}
