package org.janelia.saalfeldlab.n5.ij;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.VirtualStackAdapter;

public class PaddingTest {
	
	@Test
	public void testPaddingDifferentDims() {

		final int nx = 8;
		final int ny = 6;
		for (int nc = 1; nc < 6; nc += 4) {
			for (int nz = 1; nz < 5; nz += 3) {
				for (int nt = 1; nt < 5; nt += 2) {

					final ImagePlus imp = testImage(nx, ny, nc, nz, nt);
					final RandomAccessibleInterval img = VirtualStackAdapter.wrap(imp);

					RandomAccessibleInterval<?> imgXYCZT = N5IJUtils.toImgXYCZT(imp);
					assertArrayEquals(
							String.format("c%d_z%d_t%d", nc, nz, nt),
							new long[]{nx, ny, nc, nz, nt}, imgXYCZT.dimensionsAsLongArray());
				}
			}
		}
	}

	private ImagePlus testImage(int nx, int ny, int nc, int nz, int nt) {

		final ImagePlus imp = NewImage.createImage("test", nx, ny, nc * nz * nt, 16, NewImage.FILL_NOISE);
		imp.setDimensions(nc, nz, nt);
		return imp;
	}

}
