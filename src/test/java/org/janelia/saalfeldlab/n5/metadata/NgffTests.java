package org.janelia.saalfeldlab.n5.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.realtransform.AffineTransform3D;

public class NgffTests {

	private final String n5Root = "src/test/resources/ngff.n5";

	private static File baseDir;
	
	private static Map<String,String> versionToAxesKey;

	@BeforeClass
	public static void setup() {

		try {
			baseDir = Files.createTempDirectory("ngff-tests-").toFile();
			baseDir.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}

		versionToAxesKey = new HashMap<>();
		versionToAxesKey.put(N5Importer.MetadataOmeZarrV04Key, "multiscales[0]/axes");
		versionToAxesKey.put(N5Importer.MetadataOmeZarrV05Key, "ome/multiscales[0]/axes");
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

		for (String metadataType : new String[]{
				N5Importer.MetadataOmeZarrV04Key,
				N5Importer.MetadataOmeZarrV05Key
		}) {
			testNgfffAxisOrder("xyczt", metadataType, new int[]{10, 8, 6, 4, 2});

			testNgfffAxisOrder("xyzt", metadataType, new int[]{10, 8, 1, 4, 2});
			testNgfffAxisOrder("xyct", metadataType, new int[]{10, 8, 6, 1, 2});
			testNgfffAxisOrder("xycz", metadataType, new int[]{10, 8, 6, 4, 1});

			testNgfffAxisOrder("xyc", metadataType, new int[]{10, 8, 6, 1, 1});
			testNgfffAxisOrder("xyz", metadataType, new int[]{10, 8, 1, 4, 1});
			testNgfffAxisOrder("xyt", metadataType, new int[]{10, 8, 1, 1, 2});
		}
	}

	public void testNgfffAxisOrder(final String dataset, String metadataType, int[] size) {

		final int nx = size[0];
		final int ny = size[1];
		final int nc = size[2];
		final int nz = size[3];
		final int nt = size[4];

		final String compressionType = N5ScalePyramidExporter.RAW_COMPRESSION;

		final ImagePlus imp = NewImage.createImage("test", nx, ny, nz * nc * nt, 8, NewImage.FILL_BLACK);
		imp.setDimensions(nc, nz, nt);

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions(imp, baseDir.getAbsolutePath(), dataset, N5ScalePyramidExporter.ZARR2_FORMAT, "64", false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionType);
		writer.setOverwrite(true);
		writer.run();

		final long[] expectedDims = Arrays.stream(new long[] { nx, ny, nz, nc, nt }).toArray();
		OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		try (final N5Reader n5 = new N5Factory().openReader(baseDir.getAbsolutePath())) {

			assertTrue(n5.exists(dataset));
			assertTrue(n5.datasetExists(dataset + "/s0"));

			final DatasetAttributes dsetAttrs = n5.getDatasetAttributes(dataset + "/s0");
			assertArrayEquals("dimensions", expectedDims, dsetAttrs.getDimensions());
			
			Optional<OmeNgffMetadata> metaOpt = parser.parseMetadata(n5, dataset);
			assertTrue(metaOpt.isPresent());
			
			final String axesKey = versionToAxesKey.get(metadataType);
			final Axis[] axes = n5.getAttribute(dataset, axesKey, Axis[].class);
			assertEquals("t", axes[0].getName());
			assertEquals("c", axes[1].getName());
			assertEquals("z", axes[2].getName());
			assertEquals("y", axes[3].getName());
			assertEquals("x", axes[4].getName());
		}

	}

	@Test
	public void testParseXY() {

		final double eps = 1e-6;
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		final String n5Root = "src/test/resources/v0.5.ome.zarr/ExpA_VIP_ASLM_off_MIP_XZ_1084to1115.zarr";

		try( final N5Reader n5 = new N5Factory().openReader(n5Root)) {

			assertTrue(n5.exists(""));
			Optional<OmeNgffMetadata> opt = parser.parseMetadata(n5, "");
			assertTrue(opt.isPresent());

			OmeNgffMetadata meta = opt.get();
			Axis[] axes = meta.multiscales[0].getAxes();
			assertEquals(2, axes.length);
			assertEquals("x", axes[0].getName());
			assertEquals("y", axes[1].getName());

			OmeNgffDataset[] datasets = meta.multiscales[0].datasets;
			assertEquals(6, datasets.length);
	
			OmeNgffMultiScaleMetadata ms = meta.multiscales[0];
			AffineTransform3D affine0 = ms.spatialTransforms3d()[0];
			assertEquals(6.550032422660492, affine0.get(0, 0), eps);
			assertEquals(6.550032422660492, affine0.get(1, 1), eps);
			assertEquals(1.0, affine0.get(2, 2), eps);

			final String dset = datasets[5].path;
			final ImagePlus imp = N5Importer.openVirtual(n5Root+"?"+dset, false);
			assertEquals(64, imp.getWidth());
			assertEquals(46, imp.getHeight());
			assertEquals(1, imp.getNChannels());
			assertEquals(1, imp.getNSlices());
			assertEquals(1, imp.getNFrames());

			assertEquals(209.60103752513575, imp.getCalibration().pixelWidth, eps);
			assertEquals(209.60103752513575, imp.getCalibration().pixelHeight, eps);
			assertEquals(1.0, imp.getCalibration().pixelDepth, eps);
		}
	}

	@Test
	public void testParseXYZ() {

		final double eps = 1e-6;
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		final String n5Root = "src/test/resources/v0.5.ome.zarr/Tonsil1.zarr";

		try (final N5Reader n5 = new N5Factory().openReader(n5Root)) {

			assertTrue(n5.exists(""));
			Optional<OmeNgffMetadata> opt = parser.parseMetadata(n5, "");
			assertTrue(opt.isPresent());

			OmeNgffMetadata meta = opt.get();
			Axis[] axes = meta.multiscales[0].getAxes();
			assertEquals(5, axes.length);
			assertEquals("x", axes[0].getName());
			assertEquals("y", axes[1].getName());
			assertEquals("z", axes[2].getName());
			assertEquals("c", axes[3].getName());
			assertEquals("t", axes[4].getName());

			OmeNgffDataset[] datasets = meta.multiscales[0].datasets;
			assertEquals(5, datasets.length);

			// t=1 and z=1 are singleton — effectively 3D xyz
			long[] shape = n5.getDatasetAttributes("0").getDimensions();
			assertEquals(1, shape[2]); // z
			assertEquals(1, shape[4]); // t

			OmeNgffMultiScaleMetadata ms = meta.multiscales[0];
			AffineTransform3D affine0 = ms.spatialTransforms3d()[0];
			assertEquals(1.0, affine0.get(0, 0), eps);
			assertEquals(1.0, affine0.get(1, 1), eps);
			assertEquals(1.0, affine0.get(2, 2), eps);

			final String dset = datasets[4].path;
			final ImagePlus imp = N5Importer.openVirtual(n5Root + "?" + dset, false);
			assertEquals(168, imp.getWidth());
			assertEquals(168, imp.getHeight());
			assertEquals(27, imp.getNChannels());
			assertEquals(1, imp.getNSlices());
			assertEquals(1, imp.getNFrames());

			assertEquals(16.0, imp.getCalibration().pixelWidth, eps);
			assertEquals(16.0, imp.getCalibration().pixelHeight, eps);
			assertEquals(1.0, imp.getCalibration().pixelDepth, eps);
		}
	}

	@Test
	public void testParseXYZT() {

		final double eps = 1e-6;
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		final String n5Root = "src/test/resources/v0.5.ome.zarr/180712_H2B_22ss_Courtney1_20180712-163837_p00_c00_preview.zarr";

		try (final N5Reader n5 = new N5Factory().openReader(n5Root)) {

			assertTrue(n5.exists(""));
			Optional<OmeNgffMetadata> opt = parser.parseMetadata(n5, "");
			assertTrue(opt.isPresent());

			OmeNgffMetadata meta = opt.get();
			Axis[] axes = meta.multiscales[0].getAxes();
			assertEquals(5, axes.length);
			assertEquals("x", axes[0].getName());
			assertEquals("y", axes[1].getName());
			assertEquals("z", axes[2].getName());
			assertEquals("c", axes[3].getName());
			assertEquals("t", axes[4].getName());

			OmeNgffDataset[] datasets = meta.multiscales[0].datasets;
			assertEquals(2, datasets.length);

			// c=1 is singleton — effectively 4D xyzt
			long[] shape = n5.getDatasetAttributes("0").getDimensions();
			assertEquals(1, shape[3]); // c

			OmeNgffMultiScaleMetadata ms = meta.multiscales[0];
			AffineTransform3D affine0 = ms.spatialTransforms3d()[0];
			assertEquals(1.0, affine0.get(0, 0), eps);
			assertEquals(1.0, affine0.get(1, 1), eps);
			assertEquals(1.0, affine0.get(2, 2), eps);

			final String dset = datasets[1].path;
			final ImagePlus imp = N5Importer.openVirtual(n5Root + "?" + dset, false);
			assertEquals(166, imp.getWidth());
			assertEquals(166, imp.getHeight());
			assertEquals(1, imp.getNChannels());
			assertEquals(201, imp.getNSlices());
			assertEquals(79, imp.getNFrames());

			assertEquals(2.0, imp.getCalibration().pixelWidth, eps);
			assertEquals(2.0, imp.getCalibration().pixelHeight, eps);
			assertEquals(1.0, imp.getCalibration().pixelDepth, eps);
		}
	}

	@Test
	public void testParseXYZC() {

		final double eps = 1e-6;
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		final String n5Root = "src/test/resources/v0.5.ome.zarr/4496763.zarr";

		try (final N5Reader n5 = new N5Factory().openReader(n5Root)) {

			assertTrue(n5.exists(""));
			Optional<OmeNgffMetadata> opt = parser.parseMetadata(n5, "");
			assertTrue(opt.isPresent());

			OmeNgffMetadata meta = opt.get();
			Axis[] axes = meta.multiscales[0].getAxes();
			assertEquals(4, axes.length);
			assertEquals("x", axes[0].getName());
			assertEquals("y", axes[1].getName());
			assertEquals("z", axes[2].getName());
			assertEquals("c", axes[3].getName());

			OmeNgffDataset[] datasets = meta.multiscales[0].datasets;
			assertEquals(6, datasets.length);

			OmeNgffMultiScaleMetadata ms = meta.multiscales[0];
			AffineTransform3D affine0 = ms.spatialTransforms3d()[0];
			assertEquals(1.0, affine0.get(0, 0), eps); // x
			assertEquals(1.0, affine0.get(1, 1), eps); // y
			assertEquals(0.20000000000000018, affine0.get(2, 2), eps); // z

			final String dset = datasets[5].path;
			final ImagePlus imp = N5Importer.openVirtual(n5Root + "?" + dset, false);
			assertEquals(64, imp.getWidth());
			assertEquals(64, imp.getHeight());
			assertEquals(4, imp.getNChannels());
			assertEquals(25, imp.getNSlices());
			assertEquals(1, imp.getNFrames());

			assertEquals(32.0, imp.getCalibration().pixelWidth, eps);
			assertEquals(32.0, imp.getCalibration().pixelHeight, eps);
			assertEquals(0.20000000000000018, imp.getCalibration().pixelDepth, eps);
		}
	}

	@Test
	public void testParseXYZCT() {

		final double eps = 1e-6;
		final OmeNgffMetadataParser parser = new OmeNgffMetadataParser();

		final String n5Root = "src/test/resources/v0.5.ome.zarr/4007801.zarr";

		try (final N5Reader n5 = new N5Factory().openReader(n5Root)) {

			assertTrue(n5.exists(""));
			Optional<OmeNgffMetadata> opt = parser.parseMetadata(n5, "");
			assertTrue(opt.isPresent());

			OmeNgffMetadata meta = opt.get();
			Axis[] axes = meta.multiscales[0].getAxes();
			assertEquals(5, axes.length);
			assertEquals("x", axes[0].getName());
			assertEquals("y", axes[1].getName());
			assertEquals("z", axes[2].getName());
			assertEquals("c", axes[3].getName());
			assertEquals("t", axes[4].getName());
			assertEquals("micrometer", axes[0].getUnit()); // x
			assertEquals("micrometer", axes[1].getUnit()); // y
			assertEquals("micrometer", axes[2].getUnit()); // z

			OmeNgffDataset[] datasets = meta.multiscales[0].datasets;
			assertEquals(5, datasets.length);

			OmeNgffMultiScaleMetadata ms = meta.multiscales[0];
			AffineTransform3D affine0 = ms.spatialTransforms3d()[0];
			assertEquals(1.0, affine0.get(0, 0), eps);
			assertEquals(1.0, affine0.get(1, 1), eps);
			assertEquals(1.0, affine0.get(2, 2), eps);

			final String dset = datasets[4].path;
			final ImagePlus imp = N5Importer.openVirtual(n5Root + "?" + dset, false);
			assertEquals(135, imp.getWidth());
			assertEquals(128, imp.getHeight());
			assertEquals(2, imp.getNChannels());
			assertEquals(988, imp.getNSlices());
			assertEquals(532, imp.getNFrames());

			assertEquals(16.0, imp.getCalibration().pixelWidth, eps);
			assertEquals(16.0, imp.getCalibration().pixelHeight, eps);
			assertEquals(1.0, imp.getCalibration().pixelDepth, eps);
		}
	}

}
