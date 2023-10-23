package org.janelia.saalfeldlab.n5.ij;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RunImportExportTest;
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

	private File n5rootF;

	private File containerDir;

	private ImagePlus imp;

	@Before
	public void before() {
		System.setProperty("java.awt.headless", "true");

		final String n5Root = "src/test/resources/test.n5";
		n5rootF = new File(n5Root);

		final URL configUrl = RunImportExportTest.class.getResource( "/plugins.config" );
		final File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "macrotest.n5" );
		System.out.println( containerDir.getAbsolutePath() );

		imp = NewImage.createImage("test", 8, 7, 9, 16, NewImage.FILL_NOISE);

		final N5Exporter writer = new N5Exporter();
		writer.setOptions( imp, containerDir.getAbsolutePath(), "dataset", "16,16,16",
				N5Exporter.NONE,
				N5Exporter.RAW_COMPRESSION,
				N5Exporter.OVERWRITE, "");
		writer.run();
	}

	@After
	public void after() {
		final N5Writer n5 = new N5Factory().openWriter( containerDir.getAbsolutePath());
		n5.remove();
	}

	@Test
	public void testMacroContent() {
		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s hide", containerDir.getAbsolutePath(), "dataset" ));

		final List<ImagePlus> res = plugin.getResult();
		final ImagePlus imgImported = res.get(0);
		assertTrue( "equal content", TestExportImports.equal(imp, imgImported));

		final N5Importer pluginCrop = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s hide min=0,1,2 max=5,5,5",
						containerDir.getAbsolutePath(), "dataset" ));
		final List<ImagePlus> resCrop = pluginCrop.getResult();
		final ImagePlus imgImportedCrop = resCrop.get(0);

		final IntervalView<UnsignedShortType> imgCrop = Views.zeroMin( Views.interval(
				ImageJFunctions.wrapShort(imp),
				Intervals.createMinMax( 0, 1, 2, 5, 5, 5 )));

		final ImagePlus impCrop = ImageJFunctions.wrap(imgCrop, "imgCrop");
		impCrop.setDimensions(1, 4, 1);

		assertEquals( "  cont crop w", impCrop.getWidth(), imgImportedCrop.getWidth());
		assertEquals( "  cont crop h", impCrop.getHeight(), imgImportedCrop.getHeight());
		assertEquals( "  cont crop d", impCrop.getNSlices(), imgImportedCrop.getNSlices());
		assertTrue( "equal content crop", TestExportImports.equal(impCrop, imgImportedCrop));
	}

	@Test
	public void testMacro() {

		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s hide", n5rootF.getAbsolutePath(), "cosem" ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals(" crop num", 1, res.size());
		final ImagePlus img = res.get(0);

		assertEquals(" crop w", 256, img.getWidth());
		assertEquals(" crop h", 256, img.getHeight());
		assertEquals(" crop d", 129, img.getNSlices() );
	}

	@Test
	public void testMacroVirtual() {
		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s hide virtual", n5rootF.getAbsolutePath(), "cosem" ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals(" crop num", 1, res.size());
		final ImagePlus img = res.get(0);
		assertTrue( " is virtual", (img.getStack() instanceof ImageJVirtualStack) );
	}

	@Test
	public void testMacroCrop() {
		final String minString = "100,100,50";
		final String maxString = "250,250,120";

		final N5Importer plugin = (N5Importer)IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s hide min=%s max=%s",
				n5rootF.getAbsolutePath(), "cosem", minString, maxString ));

		final List<ImagePlus> res = plugin.getResult();
		assertEquals(" crop num", 1, res.size());

		final ImagePlus img = res.get(0);
		assertEquals(" crop w", 151, img.getWidth());
		assertEquals(" crop h", 151, img.getHeight());
		assertEquals(" crop d",  71, img.getNSlices() );
	}

}
