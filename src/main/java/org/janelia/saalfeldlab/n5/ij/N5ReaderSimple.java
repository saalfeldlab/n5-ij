package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import net.imagej.ImageJ;

@Plugin(type = Command.class, menuPath = "Plugins>N5>Read N5 Simple")
public class N5ReaderSimple implements Command {

	@Parameter
	private LogService log;
	
	@Parameter
	private UIService ui;

	@Parameter(style = "directory")
	private File n5BasePath;

	@Parameter
	private String n5Dataset;

	@Override
	public void run() {
		try {
			N5FSReader n5reader = new N5FSReader(n5BasePath.getAbsolutePath());

			// image data
			ImagePlus imp = N5IJUtils.load(n5reader, n5Dataset);

			// metadata
			N5ImagePlusMetadata.readMetadata(n5reader, n5Dataset, imp);
			
			ui.show( imp );

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {

		final ImageJ ij = new ImageJ();
		ij.launch(args);

		ij.command().run(N5ReaderSimple.class, true );
	}

}
