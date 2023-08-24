package org.janelia.saalfeldlab.n5.metadata.ome.ngff.v04;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.TestExportImports;
import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.NgffExporter;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;

public class WriteAxesTests {

	private final String UNIT = "nm";

	final int nx = 10;
	final int ny = 8;

	private static String tempPathName() {

		try {
			final File parent = Files.createTempDirectory("ome-zarr-test-").toFile();
			parent.deleteOnExit();
			return parent.getCanonicalPath();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testXYZ() throws IOException, InterruptedException, ExecutionException {

		final int nc = 1;
		final int nz = 6;
		final int nt = 1;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xyz.zarr", imp );

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertTrue(Arrays.stream(ngffMeta.multiscales[0].axes).allMatch(x -> x.getUnit().equals(UNIT)));
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage( rootLocation );
		assertTrue( TestExportImports.equal(imp, impRead));
	}

	@Test
	public void testXYC() throws IOException, InterruptedException, ExecutionException {

		final int nc = 6;
		final int nz = 1;
		final int nt = 1;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xyc.zarr", imp );

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertEquals(2, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage( rootLocation );
		assertTrue( TestExportImports.equal(imp, impRead));
	}

	@Test
	public void testXYT() throws IOException, InterruptedException, ExecutionException {

		final int nc = 1;
		final int nz = 1;
		final int nt = 6;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xyt.zarr", imp );

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertEquals(2, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage( rootLocation );
		assertTrue( TestExportImports.equal(imp, impRead));
	}

	@Test
	public void testXYCZ() throws IOException, InterruptedException, ExecutionException {

		final int nc = 3;
		final int nz = 2;
		final int nt = 1;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xycz.zarr", imp );

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage( rootLocation );
		assertTrue( TestExportImports.equal(imp, impRead));
	}

	@Test
	public void testCZYX() throws IOException, InterruptedException, ExecutionException {

		final int nc = 3;
		final int nz = 2;
		final int nt = 1;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("czyx.zarr", imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(0, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());

		final ImagePlus impRead = readImage( rootLocation );
		assertTrue( TestExportImports.equal(imp, impRead));
		// TODO other checks?
	}

	@Test
	public void testXYCZT() throws IOException, InterruptedException, ExecutionException {

		final int nc = 4;
		final int nz = 3;
		final int nt = 2;
		final ImagePlus imp = createImage( nc, nz, nt );
		final String rootLocation = createDataset("xyczt.zarr", imp);

		final OmeNgffMetadata ngffMeta = readMetadata(rootLocation);
		assertEquals(3, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.SPACE)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.CHANNEL)).count());
		assertEquals(1, Arrays.stream(ngffMeta.multiscales[0].axes).filter(x -> x.getType().equals(Axis.TIME)).count());
	}

	private ImagePlus createImage( final int nc, final int nz, final int nt ) {
		final ImagePlus imp = NewImage.createImage("test", nx, ny, nc * nz * nt, 8, NewImage.FILL_NOISE);
		imp.setDimensions(nc, nz, nt);
		imp.getCalibration().setUnit(UNIT);
		return imp;
	}

	private String createDataset(final String containerName, final ImagePlus imp )
			throws IOException, InterruptedException, ExecutionException {

		final String rootLocation = tempPathName() + File.separator + containerName;
		final String dataset = "/";
		final String blockSizeArg = "32,32,32";
		final String compression = "gzip";
		final int nScales = 1;

		final NgffExporter exporter = new NgffExporter();
		exporter.setOptions(imp, rootLocation, dataset, blockSizeArg, compression, nScales,
				N5Exporter.OVERWRITE_OPTIONS.NO_OVERWRITE.toString(), "");

		exporter.process();
		return rootLocation;
	}

	private OmeNgffMetadata readMetadata(final String rootLocation ) {

		final N5Reader zarr = new N5Factory().openReader(rootLocation);
//		final N5TreeNode node = N5DatasetDiscoverer.discover(zarr, Collections.singletonList(new DatasetMetadataParser()),
//				Collections.singletonList(new OmeNgffMetadataParser()));
		final N5TreeNode node = N5DatasetDiscoverer.discover(zarr, Collections.singletonList(new N5GenericSingleScaleMetadataParser()),
				Collections.singletonList(new OmeNgffMetadataParser()));

		final N5Metadata meta = node.getMetadata();
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
			final List<ImagePlus> impList = N5Importer.process(zarr, rootLocation, Executors.newFixedThreadPool(1), metaList, false, null);
			return impList.size() == 0 ? null : impList.get(0);
		}
		return null;
	}

}
