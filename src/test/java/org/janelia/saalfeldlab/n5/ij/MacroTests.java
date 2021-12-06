package org.janelia.saalfeldlab.n5.ij;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.img.display.imagej.ImageJVirtualStack;

public class MacroTests {

	private File n5rootF;

	private ImageJ ij;

	@Before
	public void before() {
		ij = new ImageJ( ImageJ.NO_SHOW );
		final String n5Root = "src/test/resources/test.n5";
		n5rootF = new File(n5Root);
	}

	@Test
	public void testMacro() {
		IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s", n5rootF.getAbsolutePath(), "cosem" ));

		final ImagePlus img = IJ.getImage();
		assertEquals(" crop w", 256, img.getWidth());
		assertEquals(" crop h", 256, img.getHeight());
		assertEquals(" crop d", 129, img.getNSlices() );
	}

	@Test
	public void testMacroVirtual() {
		IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer",
				String.format("n5=%s/%s virtual", n5rootF.getAbsolutePath(), "cosem" ));

		final ImagePlus img = IJ.getImage();
		assertTrue( " is virtual", (img.getStack() instanceof ImageJVirtualStack) );
	}

	@Test
	public void testMacroCrop() {
		String minString = "100,100,50";
		String maxString = "250,250,120";	

		IJ.runPlugIn("org.janelia.saalfeldlab.n5.ij.N5Importer", String.format("n5=%s/%s min=%s max=%s", 
				n5rootF.getAbsolutePath(), "cosem", minString, maxString ));

		final ImagePlus img = IJ.getImage();
		assertEquals(" crop w", 151, img.getWidth());
		assertEquals(" crop h", 151, img.getHeight());
		assertEquals(" crop d",  71, img.getNSlices() );

	}

}
