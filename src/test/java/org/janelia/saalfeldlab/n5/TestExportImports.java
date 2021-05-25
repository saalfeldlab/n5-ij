package org.janelia.saalfeldlab.n5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class TestExportImports
{
	private File baseDir;

	@Before
	public void before()
	{
		URL configUrl = RunImportExportTest.class.getResource( "/plugins.config" );
		baseDir = new File( configUrl.getFile()).getParentFile();
	}

	@Test
	public void testEmptyMeta()
	{
		final ImagePlus imp = NewImage.createImage("test", 8, 6, 1, 16, NewImage.FILL_NOISE);
		String metaType = N5Importer.MetadataDefaultKey;

		final String n5RootPath = baseDir + "/test_none.n5";
		final String dataset = "/test";
		final String blockSizeString = "32,32";
		final String compressionString = "raw";
		singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metaType, compressionString, false );
	}
	
	@Test
	public void testReadWriteParse()
	{
		final HashMap<String,String> typeToExtension = new HashMap<>();
		typeToExtension.put( "FILESYSTEM", "n5" );
		typeToExtension.put( "ZARR", "zarr" );
		typeToExtension.put( "HDF5", "h5" );

		final String blockSizeString = "16,16,16";
		final String compressionString = "gzip";
		String[] containerTypes = new String[] { "FILESYSTEM", "ZARR", "HDF5" };

		final String[] metadataTypes = new String[]{
				N5Importer.MetadataImageJKey,
				N5Importer.MetadataN5CosemKey,
				N5Importer.MetadataN5ViewerKey
		};

		for( int bitDepth : new int[]{ 8, 16, 32 })
		{
			final ImagePlus imp = NewImage.createImage("test", 8, 6, 4, bitDepth, NewImage.FILL_NOISE);
			for( final String containerType : containerTypes )
			{
				for( final String metatype : metadataTypes )
				{
					final String n5RootPath = baseDir + "/test." + typeToExtension.get( containerType );
					final String dataset = "/test_"+metatype+"_"+bitDepth;

					singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metatype, compressionString, true );
				}
			}
		}
	}

	private static < T extends RealType< T > & NativeType< T > > boolean equal( final ImagePlus a, final ImagePlus b )
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
			boolean testMeta )
	{
		final N5Exporter writer = new N5Exporter();
		writer.setOptions( imp, outputPath, dataset, blockSizeString, metadataType, compressionType,
				N5Exporter.OVERWRITE, "");
		writer.run();

		final String readerDataset = metadataType.equals( N5Importer.MetadataN5ViewerKey ) ? dataset + "/c0/s0" : dataset;
		final String n5PathAndDataset = outputPath + readerDataset;
		final N5Importer reader = new N5Importer();
		reader.setShow( false );
		final List< ImagePlus > impList = reader.process( n5PathAndDataset, false );

		assertEquals( String.format( "%s %s one image opened ", outputPath, dataset ), 1, impList.size() );

		final ImagePlus impRead = impList.get( 0 );

		if( testMeta )
		{
			boolean resEqual = impRead.getCalibration().pixelWidth == imp.getCalibration().pixelWidth && 
					impRead.getCalibration().pixelHeight == imp.getCalibration().pixelHeight
					&& impRead.getCalibration().pixelDepth == imp.getCalibration().pixelDepth;
			
			assertTrue( String.format( "%s resolutions ", dataset ), resEqual );

			boolean unitsEqual = impRead.getCalibration().getUnit().equals( imp.getCalibration().getUnit() );
			assertTrue( String.format( "%s units ", dataset ), unitsEqual );
		}

		boolean imagesEqual;
		if( imp.getType() == ImagePlus.COLOR_RGB )
		{
			imagesEqual = equalRGB( imp, impRead );
			assertEquals( String.format( "%s as rgb ", dataset ), ImagePlus.COLOR_RGB, impRead.getType() );
		}
		else
			imagesEqual = equal( imp, impRead );

		assertTrue( String.format( "%s data ", dataset ), imagesEqual );

		impRead.close();

	}

	@Test
	public void testRgb()
	{
		final ImagePlus imp = NewImage.createRGBImage("test", 8, 6, 4, NewImage.FILL_NOISE);
		String metaType = N5Importer.MetadataImageJKey;

		final String n5RootPath = baseDir + "/test_rgb.n5";
		final String dataset = "/ij";
		final String blockSizeString = "16,16,16";
		final String compressionString = "raw";

		singleReadWriteParseTest( imp, n5RootPath, dataset, blockSizeString, metaType, compressionString, false );
	}
}
