package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imagej.ImageJ;

@Plugin(type = Command.class,
	menuPath = "Plugins>N5>Export N5 Simple" )
public class N5WriterSimple implements Command {
	
	
	@Parameter
	private LogService log;
	
	@Parameter
	private ImagePlus imp;
	
	@Parameter( style = "directory" ) 
	private File n5BasePath;

	@Parameter 
	private String n5Dataset;

	@Parameter 
	private String blockSizeString = "X,Y,C,Z,T";

	@Override
	public void run() {
	
		// make sure there is one
		if ( imp == null )
		{
			log.error( "Please open an image first." );
			return;
		}
		
		// check the image type
		DataType type;
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
			type = DataType.UINT8;
			break;
		case ImagePlus.GRAY16:
			type = DataType.UINT16;
			break;
		case ImagePlus.GRAY32:
			type = DataType.FLOAT32;
			break;
		default:
			log.error( "Only 8, 16, 32-bit images are supported currently!" );
			return;
		}

		// parse block size
		int[] blockSize = 
			Arrays.stream(blockSizeString.split(",")).mapToInt( Integer::parseInt ).toArray();

		try {
			N5FSWriter n5writer = new N5FSWriter( n5BasePath.getAbsolutePath() );
	
			// image data
			N5IJUtils.save(
				    imp,
				    n5writer,
				    n5Dataset,
				    blockSize,
				    new GzipCompression());

			// metadata
			N5ImagePlusMetadata.writeMetadata( n5writer, n5Dataset, imp);

		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public static void main(String[] args) throws IOException {
		
		final ImageJ ij = new ImageJ();
		ij.launch(args);
		
		String imagePath = "/Users/bogovicj/tmp/confocal-series.tif";
		Object dataset = ij.io().open( imagePath );
		System.out.println( "dataset: " + dataset );

		ij.ui().show(dataset);
		System.out.println( ij.dataset().getDatasets().get(0) );

		ij.command().run( N5WriterSimple.class,  true );

	}

}
