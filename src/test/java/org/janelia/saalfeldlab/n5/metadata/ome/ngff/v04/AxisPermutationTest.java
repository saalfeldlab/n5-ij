package org.janelia.saalfeldlab.n5.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;

public class AxisPermutationTest {

	private static final boolean C_ORDER = true;
	private static final boolean F_ORDER = false;

	private static final double EPS = 1e-6;

	public static final String[] defaultAxes = new String[]{"x", "y", "c", "z", "t"};

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

	protected String tempN5Location() throws URISyntaxException {

		final String basePath = new File(tempN5PathName()).toURI().normalize().getPath();
		return new URI("file", null, basePath, null).toString();
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
		final String[] names = new String[]{
				"xyz", "zyx", "yzx",
				"xyc", "xcy", "cyx",
				"xyt", "xty", "tyx",
				"xyzt", "xtyz", "tyzx", "zxty",
				"xyczt", "xyzct", "xytcz", "tzcyx", "ctzxy"
		};

		// check both c- and f-order storage
		for (final String axes : names) {
			writeAndTest(zarr, axes + "_c");
			writeAndTest(zarr, axes + "_f");
		}

	}

	protected void writeAndTest(final N5Writer zarr, final String dset) {

		writeAndTest(zarr, dset, NgffTests.isCOrderFromName(dset), NgffTests.permutationFromName(dset));
	}

	protected void writeAndTest(final N5Writer zarr, final String dset, final boolean cOrder, final int[] p) {

		// write
		NgffTests.writePermutedAxes(zarr, baseName(p, cOrder), cOrder, p);

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

	}

	private static String baseName(final int[] p, final boolean cOrder) {

		final String suffix = cOrder ? "_c" : "_f";
		return Arrays.stream(p).mapToObj(i -> defaultAxes[i]).collect(Collectors.joining()) + suffix;
	}

}
