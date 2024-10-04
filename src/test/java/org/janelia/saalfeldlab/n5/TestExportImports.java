package org.janelia.saalfeldlab.n5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class TestExportImports
{

	private static File baseDir;

	@BeforeClass
	public static void setup() {

		final URL configUrl = RunImportExportTest.class.getResource("/plugins.config");
		baseDir = new File(configUrl.getFile()).getParentFile();

		try {
			baseDir = Files.createTempDirectory("n5-ij-tests-").toFile();
			baseDir.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void tearDown() {

		baseDir.delete();
	}

	private static boolean deleteContainer(final String rootPath) {

		try ( final N5Writer n5w = new N5Factory().openWriter(rootPath) ) {
			return n5w.remove();
		} catch( N5Exception e ) {
			e.printStackTrace();
		}

		return false;
	}

	@Test
	public void testEmptyMeta() throws InterruptedException
	{
		final ImagePlus imp = NewImage.createImage("test", 8, 6, 2, 16, NewImage.FILL_NOISE);
		final String metaType = N5Importer.MetadataDefaultKey;

		final String n5RootPath = baseDir + "/test_none.n5";
		final String dataset = "/test";
		final String blockSizeString = "32,32,32";
		final String compressionString = N5ScalePyramidExporter.RAW_COMPRESSION;
		singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metaType, compressionString, false );
	}

	@Test
	public void test4dN5v()
	{
		final int nChannels = 3;
		final int nSlices = 5;
		final ImagePlus imp = NewImage.createImage("test", 8, 6, nChannels*nSlices, 16, NewImage.FILL_NOISE);
		imp.setDimensions( nChannels, nSlices, 1 ); // 3 channels, 5 slices

		final String n5RootPath = baseDir + "/test.n5" ;
		final String dataset = "n5v_4d";

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions( imp, n5RootPath, dataset, N5ScalePyramidExporter.AUTO_FORMAT, "32", false,
				N5ScalePyramidExporter.DOWN_SAMPLE, N5Importer.MetadataN5ViewerKey, N5ScalePyramidExporter.GZIP_COMPRESSION);
		writer.run(); // run() closes the n5 writer

		try {
			final N5Importer reader = new N5Importer();
			reader.setShow( false );
			for( int i = 0; i < nChannels; i++)
			{
				final String n5PathAndDataset = String.format("%s/%s/c%d/s0", n5RootPath, dataset, i);
				final List< ImagePlus > impList = reader.process( n5PathAndDataset, false );
				Assert.assertEquals("n5v load channel", 1, impList.size());
				Assert.assertTrue("n5v channel equals", equalChannel(imp, i, impList.get(0)));
			}
		}
		catch(final Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}

		deleteContainer(n5RootPath);
	}

	@Test
	public void testReadWriteParse() throws InterruptedException
	{
		final HashMap<String,String> typeToExtension = new HashMap<>();
		typeToExtension.put( "FILESYSTEM", "n5" );
		typeToExtension.put( "ZARR", "zarr" );
		typeToExtension.put( "HDF5", "h5" );

		final String blockSizeString = "16,16,16";
		final String compressionString = N5ScalePyramidExporter.GZIP_COMPRESSION;
		final String[] containerTypes = new String[] { "FILESYSTEM", "ZARR", "HDF5" };
		final String[] metadataTypes = new String[]{
				N5Importer.MetadataOmeZarrKey,
				N5Importer.MetadataImageJKey,
				N5Importer.MetadataN5CosemKey,
				N5Importer.MetadataN5ViewerKey
		};

		for( final int bitDepth : new int[]{ 8, 16, 32 })
		{
			final ImagePlus imp = NewImage.createImage("test", 8, 6, 4, bitDepth, NewImage.FILL_NOISE);
			imp.setDimensions( 1, 4, 1 );
			for( final String containerType : containerTypes )
			{
				for( final String metatype : metadataTypes )
				{
					final String n5RootPath = baseDir + "/test-" + metatype + "-" + bitDepth + "." + typeToExtension.get( containerType );
					final String datasetBase = "/test_"+metatype+"_"+bitDepth;
					final String dataset = datasetBase;

					singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metatype, compressionString, true );
//					Thread.sleep(25);
				}
			}
		}
	}



	public static < T extends RealType< T > & NativeType< T > > boolean equal( final ImagePlus a, final ImagePlus b )
	{
		try {
			final Img<T> imgA = ImageJFunctions.wrapRealNative( a );
			final Img<T> imgB = ImageJFunctions.wrapRealNative( b );

			final Cursor< T > c = imgA.cursor();
			final RandomAccess< T > r = imgB.randomAccess();

			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().getRealDouble() != r.get().getRealDouble() )
					return false;
			}
			return true;
		}catch( final Exception e )
		{
			return false;
		}
	}

	/**
	 * Checks that image b is equal to channel c of
	 * @param <T>
	 * @param a
	 * @param b
	 * @return
	 */
	private static < T extends RealType< T > & NativeType< T > > boolean equalChannel( final ImagePlus all, final int i, final ImagePlus cimg )
	{
		try {
			final Img<T> imgAll = ImageJFunctions.wrapRealNative( all  );
			final Img<T> imgC = ImageJFunctions.wrapRealNative( cimg );

			final IntervalView<T> channelGtImg = Views.hyperSlice( imgAll, 2, i);
			final Cursor< T > c = channelGtImg.cursor();
			final RandomAccess< T > r = imgC.randomAccess();
			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().getRealDouble() != r.get().getRealDouble() )
					return false;
			}
			return true;
		}catch( final Exception e )
		{
			return false;
		}
	}

	private static boolean equalRGB( final ImagePlus a, final ImagePlus b )
	{
		try {
			final Img<ARGBType> imgA = ImageJFunctions.wrapRGBA( a );
			final Img<ARGBType> imgB = ImageJFunctions.wrapRGBA( b );

			final Cursor< ARGBType > c = imgA.cursor();
			final RandomAccess< ARGBType > r = imgB.randomAccess();

			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().get() != r.get().get() )
					return false;
			}

			return true;

		}catch( final Exception e )
		{
			return false;
		}
	}

	public void singleReadWriteParseTest(
			final ImagePlus imp,
			final String outputPath,
			final String dataset,
			final String blockSizeString,
			final String metadataType,
			final String compressionType,
			final boolean testMeta ) throws InterruptedException
	{
		singleReadWriteParseTest( imp, outputPath, dataset, blockSizeString, metadataType, compressionType, testMeta, true);
	}

	public static void singleReadWriteParseTest(
			final ImagePlus imp,
			final String outputPath,
			final String dataset,
			final String blockSizeString,
			final String metadataType,
			final String compressionType,
			final boolean testMeta,
			final boolean testData ) throws InterruptedException
	{
		System.out.println("singleReadWriteParseTest : " + outputPath);
		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions( imp, outputPath, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionType);

		System.out.println("writer.run");
		writer.run(); // run() closes the n5 writer
		System.out.println("writer.run returned");

		// wait
		writer.getExecutorService().awaitTermination(1000, TimeUnit.MILLISECONDS);
		System.out.println("executor service terminated");

		final String readerDataset;
		if (metadataType.equals(N5Importer.MetadataN5ViewerKey) || (metadataType.equals(N5Importer.MetadataN5CosemKey) && imp.getNChannels() > 1))
			readerDataset = dataset + "/c0/s0";
		else if (metadataType.equals(N5Importer.MetadataOmeZarrKey) || metadataType.equals(N5Importer.MetadataN5CosemKey))
			readerDataset = dataset + "/s0";
		else
			readerDataset = dataset;

		final String n5PathAndDataset = outputPath + readerDataset;

		final File n5RootWritten = new File(outputPath);
		assertTrue("root does not exist: " + outputPath, n5RootWritten.exists());
		if (outputPath.endsWith(".h5"))
			assertTrue("hdf5 file exists", n5RootWritten.exists());
		else
			assertTrue("n5 or zarr root is not a directory:" + outputPath, n5RootWritten.isDirectory());

//		Thread.sleep(25);
		System.out.println("start N5Importer");
		final N5Importer reader = new N5Importer();
		reader.setShow( false );
		List< ImagePlus > impList = reader.process( n5PathAndDataset, false );
		System.out.println("N5Importer returned");

		if( impList == null ) {
			System.out.println("failed...trying again");
			// try again like some idiot
			impList = reader.process( n5PathAndDataset, false );
		}

		assertNotNull(String.format( "Failed to open image: %s %s ", outputPath, dataset ), impList);
		assertEquals( String.format( "%s %s one image opened ", outputPath, dataset ), 1, impList.size() );

		final double EPS = 1e-9;
		final ImagePlus impRead = impList.get( 0 );
		if( testMeta )
		{
			assertEquals( String.format( "%s resolutions x", dataset ), imp.getCalibration().pixelWidth, impRead.getCalibration().pixelWidth, EPS );
			assertEquals( String.format( "%s resolutions y", dataset ), imp.getCalibration().pixelHeight, impRead.getCalibration().pixelHeight, EPS );
			assertEquals( String.format( "%s resolutions z", dataset ), imp.getCalibration().pixelDepth, impRead.getCalibration().pixelDepth, EPS );

			final boolean unitsEqual = impRead.getCalibration().getUnit().equals( imp.getCalibration().getUnit() ); assertTrue( String.format( "%s units ", dataset ), unitsEqual );
		}

		if( testData )
		{
			boolean imagesEqual;
			if( imp.getType() == ImagePlus.COLOR_RGB )
			{
				imagesEqual = equalRGB( imp, impRead );
				assertEquals( String.format( "%s as rgb ", dataset ), ImagePlus.COLOR_RGB, impRead.getType() );
			}
			else
				imagesEqual = equal( imp, impRead );

			assertTrue( String.format( "%s data ", dataset ), imagesEqual );
		}

		impRead.close();
		deleteContainer(outputPath);
		System.out.println("#############################################");
		System.out.println("#############################################");
		System.out.println("");
	}

	@Test
	public void testRgb() throws InterruptedException
	{
		final ImagePlus imp = NewImage.createRGBImage("test", 8, 6, 4, NewImage.FILL_NOISE);
		final String metaType = N5Importer.MetadataImageJKey;

		final String n5RootPath = baseDir + "/test_rgb.n5";
		final String dataset = "/ij";
		final String blockSizeString = "16,16,16";
		final String compressionString = "raw";

		singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metaType, compressionString, false );
	}

	/**
	 * A test if we ever expand n5-viewer style metadata to be able
	 * to describe arrays of more than 3 dimensions.
	 *
	 */
	@Test
	public void testMultiChannel()
	{
		for( final String suffix : new String[] { ".h5", ".n5", ".zarr" })
		{
			try {
				testMultiChannelHelper(N5Importer.MetadataN5ViewerKey, suffix);
				testMultiChannelHelper(N5Importer.MetadataN5CosemKey, suffix);
				testMultiChannelHelper(N5Importer.MetadataOmeZarrKey, suffix);
				testMultiChannelHelper(N5Importer.MetadataImageJKey, suffix);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testOverwrite() {

		final String n5Root = baseDir + "/overwriteTest.n5";
		final String dataset = "dataset";
		final String blockSizeString = "16";
		final String compressionString = "raw";

		final String metadataType = N5ScalePyramidExporter.NONE;

		final long[] szBig = new long[]{8, 6, 4};
		final long[] szSmall = new long[]{6, 4, 2};
		final ImagePlus impBig = NewImage.createImage("test", (int)szBig[0], (int)szBig[1], (int)szBig[2], 8, NewImage.FILL_NOISE);
		final ImagePlus impSmall = NewImage.createImage("test", (int)szSmall[0], (int)szSmall[1], (int)szSmall[2], 8, NewImage.FILL_NOISE);

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions(impBig, n5Root, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
		writer.setOverwrite(true);
		writer.run();

		final N5Writer n5 = new N5FSWriter(n5Root);
		assertTrue(n5.datasetExists(dataset));

		assertArrayEquals("size orig", szBig, n5.getDatasetAttributes(dataset).getDimensions());

		final N5ScalePyramidExporter writerNoOverride = new N5ScalePyramidExporter();
		writerNoOverride.setOptions(impSmall, n5Root, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
		writerNoOverride.setOverwrite(false);
		writerNoOverride.run();

		assertArrayEquals("size after no overwrite", szBig, n5.getDatasetAttributes(dataset).getDimensions());

		final N5ScalePyramidExporter writerOverride = new N5ScalePyramidExporter();
		writerOverride.setOptions(impSmall, n5Root, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, false,
				N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
		writerOverride.setOverwrite(true);
		writerOverride.run();

		assertArrayEquals("size after overwrite", szSmall, n5.getDatasetAttributes(dataset).getDimensions());

		n5.remove();
		n5.close();
	}

	@Test
	public void testFormatOptions() {

		final String n5Root = baseDir + "/root_of_some_container";
		final String dataset = "dataset";
		final String blockSizeString = "16";
		final String compressionString = "raw";
		final String metadataType = N5ScalePyramidExporter.NONE;

		// get a writer from a string
		final HashMap<String, Function<String, N5Writer>> writerMap = new HashMap<>();
		writerMap.put(N5ScalePyramidExporter.HDF5_FORMAT, x -> new N5HDF5Writer(x));
		writerMap.put(N5ScalePyramidExporter.N5_FORMAT, x -> new N5FSWriter(x));
		writerMap.put(N5ScalePyramidExporter.ZARR_FORMAT, x -> new N5ZarrWriter(x));

		final HashMap<String, Function<String, N5Reader>> readerMap = new HashMap<>();
		readerMap.put(N5ScalePyramidExporter.HDF5_FORMAT, x -> new N5HDF5Reader(x));
		readerMap.put(N5ScalePyramidExporter.N5_FORMAT, x -> new N5FSReader(x));
		readerMap.put(N5ScalePyramidExporter.ZARR_FORMAT, x -> new N5ZarrReader(x));

		final long[] szBig = new long[]{8, 6, 4};
		final ImagePlus imp = NewImage.createImage("test", (int)szBig[0], (int)szBig[1], (int)szBig[2], 8, NewImage.FILL_NOISE);

		final String[] formats = new String[]{
				N5ScalePyramidExporter.HDF5_FORMAT,
				N5ScalePyramidExporter.N5_FORMAT,
				N5ScalePyramidExporter.ZARR_FORMAT
		};

		for (final String format : formats) {

			final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
			writer.setOptions(imp, n5Root, dataset, format, blockSizeString, false,
					N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
			writer.run();

			try {
				// ensure opening the correct container type works
				final N5Writer n5 = writerMap.get(format).apply(n5Root);

				/*
				 * eventually test that other formats fail to open, but is
				 * complicated by the fact that it is possible to open a zarr
				 * container with an n5 reader. So ignore this for now
				 */

				n5.remove();
				n5.close();
			} catch (final Exception e) {
				fail("option only: " + format);
			}
		}

		// repeat the above using uri format prefixes instead of the explicit option
		for (final String format : formats) {

			final String n5RootWithFormatPrefix = format.toLowerCase() + ":" + n5Root;
			final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
			writer.setOptions(imp, n5RootWithFormatPrefix, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, false,
					N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
			writer.run();

			try {
				// ensure opening the correct container type works
				final N5Writer n5 = writerMap.get(format).apply(n5Root);

				/*
				 * eventually test that other formats fail to open, but is
				 * complicated by the fact that it is possible to open a zarr
				 * container with an n5 reader. So ignore this for now
				 */

				n5.remove();
				n5.close();
			} catch (final Exception e) {
				fail("prefix only " + format);
			}
		}

		// repeat the above using uri format prefixes AND a the same explicit option
		for (final String format : formats) {

			final String n5RootWithFormatPrefix = format.toLowerCase() + ":" + n5Root;

			final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
			writer.setOptions(imp, n5RootWithFormatPrefix, dataset, format, blockSizeString, false,
					N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
			writer.run();

			try {
				// ensure opening the correct container type works
				final N5Writer n5 = writerMap.get(format).apply(n5Root);

				/*
				 * eventually test that other formats fail to open, but is
				 * complicated by the fact that it is possible to open a zarr
				 * container with an n5 reader. So ignore this for now
				 */

				n5.remove();
				n5.close();
			} catch (final Exception e) {
				fail("consistent prefix and option: " + format);
			}
		}

		/*
		 * the plugin will print errors for the below test, which will create noise
		 * in the test output. swallow the errors instead, and check that something
		 * was written
		 */
		final TriggerOutputStream trigger = new TriggerOutputStream();
		System.setOut(new PrintStream(trigger));

		// inconsistent options should fail
		for (final String format : formats) {

			for (final String otherFormat : formats) {

				if( format.equals(otherFormat))
					continue;

				final String n5RootWithFormatPrefix = otherFormat.toLowerCase() + ":" + n5Root;
				final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
				writer.setOptions(imp, n5RootWithFormatPrefix, dataset, format, blockSizeString, false,
						N5ScalePyramidExporter.DOWN_SAMPLE, metadataType, compressionString);
				writer.run();

				try {
					// the container should not exist
					final N5Reader n5 = readerMap.get(format).apply(n5Root);
					n5.close();
					fail("inconsistent prefix and option did not fail: " + format + " " + otherFormat);


				} catch (final Exception e) {
					// check that the plugin printed some error
					assertTrue(trigger.somethingWritten);
					trigger.reset(); // reset
				}

			}
		}

		System.setOut(System.out);
	}

	private static class TriggerOutputStream extends OutputStream {

		boolean somethingWritten = false;

		@Override
		public void write(final int b) throws IOException {

			somethingWritten = true;
		}

		public void reset() {

			somethingWritten = false;
		}

	}

	public void testMultiChannelHelper( final String metatype, final String suffix ) throws InterruptedException
	{
		final int bitDepth = 8;
		final String blockSizeString = "16";
		final String compressionString = "raw";

		// add zero to avoid eclipse making these variables final
		int nc = 3; nc += 0;
		int nz = 1; nz += 0;
		int nt = 1; nt += 0;

		for( nc = 1; nc <= 3; nc += 2)
		{
			for( nz = 1; nz <= 4; nz += 3)
			{
				for( nt = 1; nt <= 5; nt += 4)
				{
					final int N = nc * nz * nt;
					final ImagePlus imp = NewImage.createImage("test", 8, 6, N, bitDepth, NewImage.FILL_NOISE);
					imp.setDimensions( nc, nz, nt );
					imp.getCalibration().pixelWidth = 0.5;
					imp.getCalibration().pixelHeight = 0.6;

					if( nz > 1 )
						imp.getCalibration().pixelDepth = 0.7;

					final String dimCode = String.format("c%dz%dt%d", nc, nz, nt);
					final String n5RootPath = baseDir + "/test_" + metatype + "_" + dimCode + suffix;
					final String dataset = String.format("/%s", dimCode);
					singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metatype, compressionString, true, nc == 1 );
//					Thread.sleep(25);
				}
			}
		}
	}

	@Test
	public void testReadWriteParsePyramid()
	{
		final HashMap<String,String> typeToExtension = new HashMap<>();
		typeToExtension.put( "FILESYSTEM", "n5" );
		typeToExtension.put( "ZARR", "zarr" );
		typeToExtension.put( "HDF5", "h5" );

		final String blockSizeString = "2,2,2";
		final String compressionString = N5ScalePyramidExporter.GZIP_COMPRESSION;

		final String[] containerTypes = new String[] { "FILESYSTEM", "ZARR", "HDF5" };
		final String[] metadataTypes = new String[]{
				N5Importer.MetadataN5CosemKey,
				N5Importer.MetadataN5ViewerKey,
				N5Importer.MetadataOmeZarrKey
		};

		final String[] downsampleTypes = new String[]{
				N5ScalePyramidExporter.DOWN_AVERAGE,
				N5ScalePyramidExporter.DOWN_SAMPLE
		};

		final int bitDepth = 16;
		final ImagePlus imp = NewImage.createImage("test", 16, 16, 16, bitDepth, NewImage.FILL_NOISE);
		imp.setDimensions( 1, 4, 1 );
		for( final String containerType : containerTypes )
		{
			for( final String downsampleMethod : downsampleTypes )
			{
				for( final String metatype : metadataTypes )
				{
					final String n5RootPath = baseDir + "/test." + typeToExtension.get( containerType );
					final String datasetBase = "/test_"+metatype+"_"+downsampleMethod+"_"+bitDepth;
					final String dataset = datasetBase;

					pyramidReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, downsampleMethod, metatype, compressionString, true, true );
				}
			}
		}
	}

	public void pyramidReadWriteParseTest(
			final ImagePlus imp,
			final String outputPath,
			final String dataset,
			final String blockSizeString,
			final String downsampleMethod,
			final String metadataType,
			final String compressionType,
			final boolean testMeta,
			final boolean testData )
	{
		final double EPS = 1e-6;
		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions( imp, outputPath, dataset, N5ScalePyramidExporter.AUTO_FORMAT, blockSizeString, true, downsampleMethod, metadataType, compressionType);
		writer.run(); // run() closes the n5 writer

		final String readerDataset;
		if( metadataType.equals( N5Importer.MetadataN5ViewerKey ))
			readerDataset = dataset + "/c0";
		else
			readerDataset = dataset;

		final String n5PathAndDataset = outputPath + readerDataset;
		final N5Reader n5r = new N5Factory()
				.openReader(outputPath);
		String[] dsetList = n5r.list(readerDataset);
		for( String d : dsetList )
		{
			final String dsetPath = readerDataset + "/" + d;
			assertTrue("dataset does not exist at: " + d , n5r.datasetExists(readerDataset+ "/" + d));

			final int nd = n5r.getDatasetAttributes(dsetPath).getNumDimensions();
			final int level = Integer.parseInt( d.replaceFirst("^s", ""));
			final double factor = Math.pow(2, level);
			final double[] flatAffine = downsamplingAffineFlat( nd, factor, downsampleMethod );

			final N5MetadataParser<?> parser = getParser(metadataType);
			@SuppressWarnings("unchecked")
			final Optional<N5SpatialDatasetMetadata> meta = (Optional<N5SpatialDatasetMetadata>)parser.parseMetadata(n5r,dsetPath);
			assertTrue("metadata does not exist or not parsable", meta.isPresent());
			assertArrayEquals("affine", flatAffine, meta.get().spatialTransform().getRowPackedCopy(), EPS);
		}

		n5r.close();
		try {
			final N5Writer n5w = new N5Factory().openWriter(outputPath);
			n5w.remove();
			n5w.close();
		} catch (final N5Exception e) { }
	}

	private double[] downsamplingAffineFlat( final int nd, final double factor, final String downsampleMethod ) {

		final AffineTransform out = new AffineTransform(nd);
		for (int i = 0; i < nd; i++) {
			out.set(factor, i, i);
			if (downsampleMethod.equals(N5ScalePyramidExporter.DOWN_AVERAGE)) {
				out.set( 0.5 * factor - 0.5, i, nd);
			}
		}
		return out.getRowPackedCopy();
	}

	@SuppressWarnings("unchecked")
	private <T extends N5SpatialDatasetMetadata> N5MetadataParser<T> getParser(final String metadataType) {

		switch (metadataType) {
		case N5Importer.MetadataN5CosemKey:
			return (N5MetadataParser<T>)new N5CosemMetadataParser();
		case N5Importer.MetadataN5ViewerKey:
			return (N5MetadataParser<T>)new N5SingleScaleMetadataParser();
		case N5Importer.MetadataOmeZarrKey:
			return (N5MetadataParser<T>)new NgffSingleScaleMetadataParser();
		default:
			return null;
		}
	}

	public void testPyramidHelper( final String metatype, final String suffix )
	{
		final int bitDepth = 8;

		final String n5RootPath = baseDir + "/test_"+ metatype+"_dimCombos" + suffix;
		final String blockSizeString = "3";
		final String compressionString = "raw";
		final String downsamplingType = N5ScalePyramidExporter.DOWN_SAMPLE;

		int nc = 1;
		int nz = 1;
		int nt = 5;

		for( nc = 1; nc <= 3; nc += 2)
		{
			for( nz = 1; nz <= 4; nz += 3)
			{
				for( nt = 1; nt <= 5; nt += 4)
				{
					final int N = nc * nz * nt;
					final ImagePlus imp = NewImage.createImage("test", 8, 6, N, bitDepth, NewImage.FILL_NOISE);
					imp.setDimensions( nc, nz, nt );
					imp.getCalibration().pixelWidth = 0.5;
					imp.getCalibration().pixelHeight = 0.6;

					if( nz > 1 )
						imp.getCalibration().pixelDepth = 0.7;

					final String dataset = String.format("/c%dz%dt%d", nc, nz, nt);
					pyramidReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, downsamplingType,
							metatype, compressionString, true, nc == 1 );
				}
			}

		}
	}

}
