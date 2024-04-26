package org.janelia.saalfeldlab.n5.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.TestExportImports;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.metadata.imagej.CanonicalMetadataToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ViewerToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.NgffToImagePlus;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.util.Pair;

public class WriteAxesTests {

	private final String UNIT = "nm";

	final int nx = 10;
	final int ny = 8;

	private HashMap<Class<?>, ImageplusMetadata<?>> impWriters;

	private static String tempPathName() {

		try {
			final File parent = Files.createTempDirectory("ome-zarr-test-").toFile();
			parent.deleteOnExit();
			return parent.getCanonicalPath();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static HashMap<Class<?>, ImageplusMetadata<?>> defaultImagePlusMetadataWriters()
	{
		final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes = new HashMap<>();
		impMetaWriterTypes.put(N5ImagePlusMetadata.class, new ImagePlusLegacyMetadataParser());
		impMetaWriterTypes.put(NgffSingleScaleAxesMetadata.class, new NgffToImagePlus());
		impMetaWriterTypes.put(N5CosemMetadata.class, new CosemToImagePlus());
		impMetaWriterTypes.put(N5SingleScaleMetadata.class, new N5ViewerToImagePlus());
		impMetaWriterTypes.put(CanonicalDatasetMetadata.class, new CanonicalMetadataToImagePlus());
		impMetaWriterTypes.put(CanonicalSpatialDatasetMetadata.class, new CanonicalMetadataToImagePlus());
		return impMetaWriterTypes;
	}

	@Before
	public void before() {

		/* To explicitly test headless */
		System.setProperty("java.awt.headless", "true");

		impWriters = defaultImagePlusMetadataWriters();
	}

	private void remove(final String rootLocation) {

		final N5Writer zarr = new N5Factory().openWriter(rootLocation);
		zarr.remove();
	}

	@Test
	public void testXYZ() throws IOException, InterruptedException, ExecutionException {

		final int nc = 1;
		final int nz = 6;
		final int nt = 1;
		final String dataset = "";
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xyz.zarr", dataset, imp );

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertTrue(Arrays.stream(ngffMeta.multiscales[0].axes).allMatch(x -> x.getUnit().equals(UNIT)));
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage(rootLocation);
		assertTrue(TestExportImports.equal(imp, impRead));
		remove(rootLocation);
	}

	@Test
	public void testXYC() throws IOException, InterruptedException, ExecutionException {

		final int nc = 6;
		final int nz = 1;
		final int nt = 1;
		final String dataset = "";
		final ImagePlus imp = createImage(nc, nz, nt);
		final String rootLocation = createDataset("xyc.zarr", dataset, imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertEquals(2, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage(rootLocation);
		assertTrue(TestExportImports.equal(imp, impRead));
		remove(rootLocation);
	}

	@Test
	public void testXYT() throws IOException, InterruptedException, ExecutionException {

		final int nc = 1;
		final int nz = 1;
		final int nt = 6;
		final String dataset = "";
		final ImagePlus imp = createImage(nc, nz, nt);
		final String rootLocation = createDataset("xyt.zarr", dataset, imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertEquals(2, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage(rootLocation);
		assertTrue(TestExportImports.equal(imp, impRead));
		remove(rootLocation);
	}

	@Test
	public void testXYCZ() throws IOException, InterruptedException, ExecutionException {

		final int nc = 3;
		final int nz = 2;
		final int nt = 1;
		final String dataset = "";
		final ImagePlus imp = createImage(nc, nz, nt);
		final String rootLocation = createDataset("xycz.zarr", dataset, imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage(rootLocation);
		assertTrue(TestExportImports.equal(imp, impRead));
		remove(rootLocation);
	}

	@Test
	public void testCZYX() throws IOException, InterruptedException, ExecutionException {

		final int nc = 3;
		final int nz = 2;
		final int nt = 1;
		final String dataset = "";
		final ImagePlus imp = createImage(nc, nz, nt);
		final String rootLocation = createDataset("czyx.zarr", dataset, imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage(rootLocation);
		assertTrue(TestExportImports.equal(imp, impRead));
		remove(rootLocation);
		// TODO other checks?
	}

	@Test
	public void testXYCZT() throws IOException, InterruptedException, ExecutionException {

		final int nc = 4;
		final int nz = 3;
		final int nt = 2;
		final String dataset = "";
		final ImagePlus imp = createImage(nc, nz, nt);
		final String rootLocation = createDataset("xyczt.zarr", dataset, imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation, dataset);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());
		remove(rootLocation);
	}

	private ImagePlus createImage(final int nc, final int nz, final int nt) {

		final ImagePlus imp = NewImage.createImage("test", nx, ny, nc * nz * nt, 8, NewImage.FILL_NOISE);
		imp.setDimensions(nc, nz, nt);
		imp.getCalibration().setUnit(UNIT);
		return imp;
	}

	private String createDataset(final String containerName, final String dataset, final ImagePlus imp)
			throws IOException, InterruptedException, ExecutionException {

		final String rootLocation = tempPathName() + File.separator + containerName;
		final String blockSizeArg = "32,32,32";
		final String compression = N5ScalePyramidExporter.GZIP_COMPRESSION;

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions(imp, rootLocation, dataset, N5ScalePyramidExporter.ZARR_FORMAT, blockSizeArg, false,
				N5ScalePyramidExporter.DOWN_SAMPLE, N5Importer.MetadataOmeZarrKey, compression);
		writer.run(); // run() closes the n5 writer

		return rootLocation;
	}

	private OmeNgffMetadata readMetadata(final String rootLocation, final String dataset) {

		final N5Reader zarr = new N5Factory().openReader(rootLocation);
		final String prefix = String.format("%s - %s", rootLocation, dataset);

		assertNotNull(prefix + " reader null", zarr);
		assertTrue(prefix + " root exists", zarr.exists(""));

		final N5TreeNode node = N5DatasetDiscoverer.discover(zarr,
				Collections.singletonList(new N5GenericSingleScaleMetadataParser()),
				Collections.singletonList(new OmeNgffMetadataParser()));

		assertNotNull(prefix + " node null", node);
		assertNotNull( zarr.getAttribute(dataset, "", JsonElement.class).getAsJsonObject().get("multiscales"));
		assertTrue(prefix + " is not group", zarr.exists(dataset));

		final N5Metadata meta = node.getMetadata();
		assertNotNull(prefix + " metadata null", meta);
		assertTrue(prefix + " metadata not OmeNgff", (meta instanceof OmeNgffMetadata));
		if( meta instanceof OmeNgffMetadata ) {
			return (OmeNgffMetadata) meta;
		}
		return null;
	}

	private ImagePlus readImage(final String rootLocation ) {

		final N5Reader zarr = new N5Factory().openReader(rootLocation);
		final N5TreeNode node = N5DatasetDiscoverer.discover(zarr,
				Collections.singletonList(new N5GenericSingleScaleMetadataParser()),
				Collections.singletonList(new OmeNgffMetadataParser()));

		final N5Metadata meta = node.getDescendant("s0").get().getMetadata();
		if( meta instanceof N5DatasetMetadata ) {

			final N5DatasetMetadata dsetmeta = (N5DatasetMetadata)meta;
			final List<N5DatasetMetadata> metaList = Collections.singletonList( dsetmeta );
			final List<ImagePlus> impList = N5Importer.process(zarr, rootLocation, Executors.newFixedThreadPool(1), metaList, false, null, false, impWriters);
			return impList.size() == 0 ? null : impList.get(0);
		}
		return null;
	}

}
