package org.janelia.saalfeldlab.n5.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffMultiScaleGroupAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.NgffMultiScaleGroupAttributes.MultiscaleDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


import ij.ImagePlus;
import ij.gui.NewImage;

public class NgffTests {

	private final String n5Root = "src/test/resources/ngff.n5";

	private static File baseDir;

	@BeforeClass
	public static void setup() {

		try {
			baseDir = Files.createTempDirectory("ngff-tests-").toFile();
			baseDir.deleteOnExit();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testNgffGroupAttributeParsing() {

		final double eps = 1e-9;
		try( final N5FSReader n5 = new N5FSReader(n5Root) ) {

			NgffMultiScaleGroupAttributes[] multiscales = n5.getAttribute("ngff_grpAttributes", "multiscales",
					NgffMultiScaleGroupAttributes[].class);
			Assert.assertEquals("one set of multiscales", 1, multiscales.length);

			MultiscaleDataset[] datasets = multiscales[0].datasets;
			Assert.assertEquals("num levels", 6, datasets.length);

			double scale = 4;
			for (int i = 0; i < datasets.length; i++) {

				String pathName = String.format("s%d", i);
				Assert.assertEquals("path name " + i, pathName, datasets[i].path);
				Assert.assertEquals("scale " + i, scale, datasets[i].transform.scale[2], eps);

				scale *= 2;
			}

		} catch (N5Exception e) {
			fail("Ngff parsing failed");
			e.printStackTrace();
		}
	}

	@Test
	public void testNgffExportAxisOrder() {

		testNgfffAxisOrder("xyczt", new int[] { 10, 8, 6, 4, 2 });

		testNgfffAxisOrder("xyzt", new int[] { 10, 8, 1, 4, 2 });
		testNgfffAxisOrder("xyct", new int[] { 10, 8, 6, 1, 2 });
		testNgfffAxisOrder("xycz", new int[] { 10, 8, 6, 4, 1 });

		testNgfffAxisOrder("xyc", new int[] { 10, 8, 6, 1, 1 });
		testNgfffAxisOrder("xyz", new int[] { 10, 8, 1, 4, 1 });
		testNgfffAxisOrder("xyt", new int[] { 10, 8, 1, 1, 2 });
	}

	public void testNgfffAxisOrder(final String dataset, int[] size) {

		final int nx = size[0];
		final int ny = size[1];
		final int nc = size[2];
		final int nz = size[3];
		final int nt = size[4];

		final String metadataType = N5Importer.MetadataOmeZarrKey;
		final String compressionType = N5ScalePyramidExporter.RAW_COMPRESSION;

		final ImagePlus imp = NewImage.createImage("test", nx, ny, nz * nc * nt, 8, NewImage.FILL_BLACK);
		imp.setDimensions(nc, nz, nt);

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions(imp, baseDir.getAbsolutePath(), dataset, N5ScalePyramidExporter.ZARR_FORMAT, "64", false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionType);
		writer.run();

		final long[] expectedDims = Arrays.stream(new long[] { nx, ny, nz, nc, nt }).filter(x -> x > 1).toArray();

		try (final N5Reader n5 = new N5Factory().openReader(baseDir.getAbsolutePath())) {

			assertTrue(n5.exists(dataset));
			assertTrue(n5.datasetExists(dataset + "/s0"));

			final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(dataset + "/s0");
			assertArrayEquals("dimensions", expectedDims, dsetAttrs.getDimensions());

			int i = 0;
			final Axis[] axes = n5.getAttribute(dataset, "multiscales[0]/axes", Axis[].class);

			if (nt > 1)
				assertEquals("t", axes[i++].getName());

			if (nc > 1)
				assertEquals("c", axes[i++].getName());

			if (nz > 1)
				assertEquals("z", axes[i++].getName());

			assertEquals("y", axes[i++].getName());
			assertEquals("x", axes[i++].getName());
		}

	}

}
