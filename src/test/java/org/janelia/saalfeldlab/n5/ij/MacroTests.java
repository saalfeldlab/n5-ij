package org.janelia.saalfeldlab.n5.ij;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.TestExportImports;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImageJVirtualStack;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class MacroTests {

	private File containerDir;

	private URI n5rootF;

	private String dataset;

	private ImagePlus imp;

	@Before
	public void before() {
		System.setProperty("java.awt.headless", "true");

		containerDir = new File(tempN5PathName());

		n5rootF = Paths.get("src", "test", "resources", "test.n5").toUri();
		dataset = "dataset";

		imp = NewImage.createImage("test", 8, 7, 9, 16, NewImage.FILL_NOISE);
		final String format = N5ScalePyramidExporter.N5_FORMAT;

		final N5ScalePyramidExporter writer = new N5ScalePyramidExporter();
		writer.setOptions( imp, containerDir.getAbsolutePath(), dataset, format, "16,16,16", false,
				N5ScalePyramidExporter.NONE, N5ScalePyramidExporter.DOWN_SAMPLE, N5ScalePyramidExporter.RAW_COMPRESSION);
		writer.run(); // run() closes the n5 writer
	}

	@After
	public void after() {

		N5Writer n5;
		try {
			n5 = new N5Factory().openWriter(containerDir.getCanonicalPath());
			n5.remove();
		} catch (IOException e) {}
	}

	private static String tempN5PathName() {

		try {
			final File tmpFile = Files.createTempDirectory("n5-ij-macro-test-").toFile();
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
	public void testMacroContentPath() throws IOException {
		System.out.println("testMacroContent Path style");
		testMacroContentHelper("url=%s/%s");
	}

	@Test
	public void testMacroContentUri() throws IOException {
		System.out.println("testMacroContent URI style skip windows");
		final String os = System.getProperty("os.name").toLowerCase();
		if( !os.startsWith("windows"))
			testMacroContentHelper("url=%s?%s");
	}

	public void testMacroContentHelper( String urlFormat ) throws IOException {

		System.out.println(urlFormat);
		System.out.println(containerDir.getCanonicalPath());

		// URL
		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format( urlFormat + " hide", containerDir.getCanonicalPath(), dataset ));

		final List<ImagePlus> res = plugin.getResult();
		final ImagePlus imgImported = res.get(0);
		assertTrue( "equal content", TestExportImports.equal(imp, imgImported));

		final N5Importer pluginCrop = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format( urlFormat + " hide min=0,1,2 max=5,5,5",
						containerDir.getAbsolutePath(), "dataset" ));
		final List<ImagePlus> resCrop = pluginCrop.getResult();
		final ImagePlus imgImportedCrop = resCrop.get(0);

		final IntervalView<UnsignedShortType> imgCrop = Views.zeroMin( Views.interval(
				ImageJFunctions.wrapShort(imp),
				Intervals.createMinMax( 0, 1, 2, 5, 5, 5 )));

		final ImagePlus impCrop = ImageJFunctions.wrap(imgCrop, "imgCrop");
		impCrop.setDimensions(1, 4, 1);

		assertEquals("cont crop w", impCrop.getWidth(), imgImportedCrop.getWidth());
		assertEquals("cont crop h", impCrop.getHeight(), imgImportedCrop.getHeight());
		assertEquals("cont crop d", impCrop.getNSlices(), imgImportedCrop.getNSlices());
		assertTrue("equal content crop", TestExportImports.equal(impCrop, imgImportedCrop));
	}

	@Test
	public void testMacro() {

		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("url=%s" + File.separator + "%s hide", n5rootF.toString(), "cosem" ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals("crop num", 1, res.size());
		final ImagePlus img = res.get(0);

		assertEquals("crop w", 256, img.getWidth());
		assertEquals("crop h", 256, img.getHeight());
		assertEquals("crop d", 129, img.getNSlices());
	}

	@Test
	public void testMacroVirtual() {

		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("url=%s" + File.separator + "%s hide virtual", n5rootF.toString(), "cosem" ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals("crop num", 1, res.size());
		final ImagePlus img = res.get(0);
		assertTrue( "is virtual", (img.getStack() instanceof ImageJVirtualStack) );
	}

	@Test
	public void testMacroCrop() {

		final String minString = "100,100,50";
		final String maxString = "250,250,120";
		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("url=%s" + File.separator + "%s hide min=%s max=%s",
						n5rootF.toString(), "cosem", minString, maxString ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals(" crop num", 1, res.size());

		final ImagePlus img = res.get(0);
		assertEquals("crop w", 151, img.getWidth());
		assertEquals("crop h", 151, img.getHeight());
		assertEquals("crop d",  71, img.getNSlices() );
	}

}
