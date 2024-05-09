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
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;

public class AxisPermutationTest {

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

	@Test
	public void testPermutations() {

		final N5Writer zarr = new N5Factory().openWriter(containerUri.toString());
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
