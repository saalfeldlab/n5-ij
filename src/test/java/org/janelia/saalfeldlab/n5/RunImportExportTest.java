package org.janelia.saalfeldlab.n5;

import java.util.HashMap;
import java.util.List;

import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class RunImportExportTest
{
	final ImagePlus imp;

	final String outputPath;

	final String dataset;

	private String metadataType;

	private String compressionType;

	private String blockSizeString;
	
	/*
	 * "Outputs"
	 */
	private boolean singletonListOut;

	private boolean resEqual;

	private boolean unitsEqual;

	private boolean imagesEqual;
	
	public RunImportExportTest(
			final ImagePlus imp,
			final String outputPath,
			final String dataset,
			final String metadataType,
			final String compressionType,
			final String blockSizeString )
	{
		this.imp = imp;
		this.outputPath = outputPath;
		this.dataset = dataset;

		this.metadataType = metadataType;
		this.compressionType = compressionType;
		this.blockSizeString = blockSizeString;
	}
	
	public void run()
	{
		N5Exporter writer = new N5Exporter();
		writer.setOptions( imp, outputPath, dataset,
				blockSizeString, metadataType, compressionType );
		writer.run();

		final String n5PathAndDataset = outputPath +  dataset;
		N5Importer reader = new N5Importer();
		List< ImagePlus > impList = reader.process( n5PathAndDataset, false );

		singletonListOut = impList.size() == 1;

		ImagePlus impRead = impList.get( 0 );

		resEqual = impRead.getCalibration().pixelWidth == imp.getCalibration().pixelWidth &&
			impRead.getCalibration().pixelHeight == imp.getCalibration().pixelHeight &&
			impRead.getCalibration().pixelDepth == imp.getCalibration().pixelDepth;

		unitsEqual = impRead.getCalibration().getUnit().equals( imp.getCalibration().getUnit() );
		
		imagesEqual = equal( imp, impRead );

		impRead.close();
	}

	public static void main( String[] args )
	{
		ImagePlus imp = IJ.openImage( "/home/john/tmp/blobs.tif" );

		String[] metadataTypes = new String[]{ 
				N5Importer.MetadataImageJKey,
				N5Importer.MetadataN5CosemKey,
				N5Importer.MetadataN5ViewerKey
		};

		//String[] containerTypes = new String[] { "FILESYSTEM", "ZARR" };
		String[] containerTypes = new String[] { "HDF5" };

		HashMap<String,String> typeToExtension = new HashMap<>();
		typeToExtension.put( "FILESYSTEM", "n5" );
		typeToExtension.put( "ZARR", "zarr" );
		typeToExtension.put( "HDF5", "h5" );

		for( String containerType : containerTypes )
		{
			for( String metatype : metadataTypes )
			{
				String n5RootPath = "/home/john/tmp/test." + typeToExtension.get( containerType );
				RunImportExportTest testRunner = new RunImportExportTest(
						imp, n5RootPath, "/blobs", 
						metatype, "gzip", "32,32" );

				testRunner.run();
				System.out.println( "metadata type: " + metatype );
				boolean allPassed = testRunner.singletonListOut &&
						testRunner.resEqual && 
						testRunner.unitsEqual  && 
						testRunner.imagesEqual ;
				System.out.println( " " + allPassed );
				if( ! allPassed )
				{
					System.out.println( "   " +
						testRunner.singletonListOut + " " +
						testRunner.resEqual + " " +
						testRunner.unitsEqual  + " " +
						testRunner.imagesEqual );
				}
				System.out.println( " " );
			}
		}
	}
	
	public static < T extends RealType< T > & NativeType< T > > boolean equal( final ImagePlus a, final ImagePlus b )
	{
		try {
			Img<T> imgA = ImageJFunctions.wrapRealNative( a );
			Img<T> imgB = ImageJFunctions.wrapRealNative( a );
			
			Cursor< T > c = imgA.cursor();
			RandomAccess< T > r = imgB.randomAccess();

			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().getRealDouble() != r.get().getRealDouble() )
					return false;

			}

			return true;

		}catch( Exception e )
		{
			return false;
		}
	}

}
