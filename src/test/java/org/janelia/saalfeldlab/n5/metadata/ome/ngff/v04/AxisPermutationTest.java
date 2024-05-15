package org.janelia.saalfeldlab.n5.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffTests;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;

public class AxisPermutationTest {

	private static final boolean C_ORDER = true;
	private static final boolean F_ORDER = false;

	private static final double EPS = 1e-6;

	public static final String[] AXIS_PERMUTATIONS = new String[]{
			"xyz", "zyx", "yzx",
			"xyc", "xcy", "cyx",
			"xyt", "xty", "tyx",
			"xyzt", "xtyz", "tyzx", "zxty",
			"xyczt", "xyzct", "xytcz", "tzcyx", "ctzxy"
	};

	private URI containerUri;

	@Before
	public void before() {

		System.setProperty("java.awt.headless", "true");

		try {
			containerUri = new File(tempN5PathName()).getCanonicalFile().toURI();
		} catch (final IOException e) {}
	}

	@After
	public void after() {

		final N5Writer n5 = new N5Factory().openWriter(containerUri.toString());
		n5.remove();
	}

	private static String tempN5PathName() {

		try {
			final File tmpFile = Files.createTempDirectory("n5-ij-ngff-test-").toFile();
			tmpFile.deleteOnExit();
			return tmpFile.getCanonicalPath();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * The default behavior of n5-imglib2 and n5-zarr is such that the dimensions of an imglib2 img
	 * are the same as the zarr arrays shape if the zarr is c-order, but will be reversed if
	 * c-order.
	 *
	 * Assuming that most people using zarr interpret their axes as ZYX regardless of f- or
	 * c-ordering, we'd like that the N5Importer outputs an ImagePlus that "reverses" the shape of
	 * for both c- and f-order zarr arrays.
	 */
	@Test
	public void testZarrDimensionReversal() {

		final N5Writer zarr = new N5Factory().openWriter(StorageFormat.ZARR, containerUri.toString());

		final long[] dims = new long[]{6, 5, 4}; // x y z
		final int[] blkSize = new int[]{6, 5, 4};
		final RawCompression compression = new RawCompression();
		final DataType type = DataType.UINT8;

		String dset = "COrder";
		// writes a c-order zarr array with shape [4,5,6] "ZYX"
		NgffTests.createDataset(zarr, C_ORDER, dset, dims, blkSize, type, compression);
		final String uriC = containerUri.toString() + "?" + dset;
		final ImagePlus impC = N5Importer.open(uriC, false);
		assertEquals("nx C", 6, impC.getWidth());
		assertEquals("ny C", 5, impC.getHeight());
		assertEquals("nz C", 4, impC.getNChannels());

		dset = "FOrder";
		// writes a f-order zarr array with shape [4,5,6] "ZYX"
		NgffTests.createDataset(zarr, F_ORDER, dset, dims, blkSize, type, compression);

		final String uriF = containerUri.toString() + "?" + dset;
		final ImagePlus impF = N5Importer.open(uriF, false);
		assertEquals("nx F", 6, impF.getWidth());
		assertEquals("ny F", 5, impF.getHeight());
		assertEquals("nz F", 4, impF.getNChannels());
	}

	@Test
	public void testNgffPermutations() {

		final N5Writer zarr = new N5Factory().openWriter(StorageFormat.ZARR, containerUri.toString());
		// don't check every axis permutation, but some relevant ones, and some strange ones
		// check both c- and f-order storage
		for (final String axes : AXIS_PERMUTATIONS) {
			final String dsetC = axes + "_c";
			writeAndTest((z, n) -> {
				writePermutedAxesNgff(zarr, dsetC);
			}, zarr, dsetC);

			final String dsetF = axes + "_f";
			writeAndTest((z, n) -> {
				writePermutedAxesNgff(zarr, dsetF);
			}, zarr, dsetF);
		}
	}

	@Test
	public void testCosemAxisPermutations() {

		final N5Writer zarr = new N5Factory().openWriter(StorageFormat.ZARR, containerUri.toString());
		// don't check every axis permutation, but some relevant ones, and some strange ones
		// check both c- and f-order storage
		for (final String axes : AXIS_PERMUTATIONS) {

			final String dsetC = axes + "_c";
			writeAndTest((z, n) -> {
				writePermutedAxesCosem(zarr, dsetC + "/s0");
			}, zarr, dsetC);

			final String dsetF = axes + "_f";
			writeAndTest((z, n) -> {
				writePermutedAxesCosem(zarr, dsetF + "/s0");
			}, zarr, dsetF);
		}
	}

	protected void writeAndTest(final BiConsumer<N5Writer, String> writeTestData, final N5Writer zarr, final String dset) {

		// write
		writeTestData.accept(zarr, dset);

		// read
		final ImagePlus imp = N5Importer.open(String.format("%s?%s", containerUri.toString(), dset + "/s0"), false);

		// test
		assertEquals("size x", NgffTests.NX, imp.getWidth());
		assertEquals("size y", NgffTests.NY, imp.getHeight());

		assertEquals("res x", NgffTests.RX, imp.getCalibration().pixelWidth, EPS);
		assertEquals("res y", NgffTests.RY, imp.getCalibration().pixelHeight, EPS);

		final char[] axes = dset.split("_")[0].toCharArray();
		if (ArrayUtils.contains(axes, NgffTests.Z)) {
			assertEquals("n slices", NgffTests.NZ, imp.getNSlices());
			assertEquals("res z", NgffTests.RZ, imp.getCalibration().pixelDepth, EPS);
		}

		if (ArrayUtils.contains(axes, NgffTests.C)) {
			assertEquals("n channels", NgffTests.NC, imp.getNChannels());
		}

		if (ArrayUtils.contains(axes, NgffTests.T)) {
			assertEquals("n timepoints", NgffTests.NT, imp.getNFrames());
			assertEquals("res t", NgffTests.RT, imp.getCalibration().frameInterval, EPS);
		}

		zarr.remove(dset);
	}

	public static void writePermutedAxesNgff(final N5Writer zarr, final String dsetPath) {

		NgffTests.writePermutedAxes(zarr, dsetPath,
				NgffTests.isCOrderFromName(dsetPath),
				NgffTests.permutationFromName(dsetPath));
	}

	public static void writePermutedAxesCosem(final N5Writer zarr, final String dsetPath) {

		writePermutedAxesCosem(zarr, dsetPath,
				NgffTests.isCOrderFromName(dsetPath),
				NgffTests.permutationFromName(dsetPath));
	}

	public static void writePermutedAxesCosem(final N5Writer zarr, final String dsetPath, final boolean cOrder, final int[] permutation) {

		final long[] dims = AxisUtils.permute(NgffTests.DEFAULT_DIMENSIONS, permutation);
		final int[] blkSize = Arrays.stream(dims).mapToInt(x -> (int)x).toArray();

		NgffTests.createDataset(zarr, cOrder, dsetPath, dims, blkSize, DataType.UINT8, new RawCompression());

		final DatasetAttributes dsetAttrs = zarr.getDatasetAttributes(dsetPath);

		final CosemTransform cosemTform = NgffTests.buildPermutedAxesCosemMetadata(permutation, true, dsetAttrs);
		zarr.setAttribute(dsetPath, "transform", cosemTform);
	}

}
