package org.janelia.saalfeldlab.n5;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.metadata.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class MetadataIoTests
{
	static private String testDirPath = System.getProperty("user.home") + "/tmp/n5-test";
	static private String testBaseDatasetName = "/test/data";

	private ImagePlus imp2d;
	private ImagePlus imp3d;
	private ImagePlus imp4d;
	private ImagePlus imp5d;

	@Before
	public void setUp() throws IOException
	{
		ArrayImgFactory< UnsignedByteType > factory = new ArrayImgFactory<>( new UnsignedByteType() );

		imp2d = ImageJFunctions.wrapUnsignedByte( factory.create( 20, 15  ), "imp2d" );
		imp2d.getCalibration().pixelWidth = 3.3;
		imp2d.getCalibration().pixelHeight = 2.2;

		imp3d = ImageJFunctions.wrapUnsignedByte( factory.create( 20, 15, 10 ), "imp3d" );
		imp3d.setDimensions( 1, 10, 1 );
		imp3d.getCalibration().pixelWidth = 3.3;
		imp3d.getCalibration().pixelHeight = 2.2;
		imp3d.getCalibration().pixelDepth = 1.1;

		imp4d = ImageJFunctions.wrapUnsignedByte( factory.create( 20, 15, 10, 8 ), "imp4d" );
		imp4d.setDimensions( 8, 10, 1 );
		imp4d.getCalibration().pixelWidth = 3.3;
		imp4d.getCalibration().pixelHeight = 2.2;
		imp4d.getCalibration().pixelDepth = 1.1;

		imp5d = ImageJFunctions.wrapUnsignedByte( factory.create( 20, 15, 10, 7, 4 ), "imp5d" );
		imp5d.setDimensions( 7, 10, 4 );
		imp5d.getCalibration().pixelWidth = 3.3;
		imp5d.getCalibration().pixelHeight = 2.2;
		imp5d.getCalibration().pixelDepth = 1.1;
	}

	@Test
	public void testCustomMetadata()
	{
		ImagePlusMetadataTemplate meta = new ImagePlusMetadataTemplate( "testpath", imp3d );
		MetadataTemplateMapper metaMapper = new MetadataTemplateMapper( MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );

		N5FSWriter n5 = null;
		try
		{
			n5 = new N5FSWriter( testDirPath+ ".n5" );
			n5.createDataset( "/testpath", 
				new long[]{2,3,4}, new int[] {2,3,4}, DataType.UINT8 , new RawCompression() );

			metaMapper.writeMetadata( meta, n5, "/testpath" );
		}
		catch ( IOException e )
		{
			Assert.fail( "Metamapper - could not create writer or dataset." );
		}
		catch ( Exception e )
		{
			Assert.fail( "Metamapper - could not write metadata");
		}

		try
		{
			double[] res = n5.getAttribute( "/testpath", "resolution", double[].class );
			Assert.assertArrayEquals( "metamapper resolution",
					new double[]{ 3.3, 2.2, 1.1 }, res, 1e-6 );
		}
		catch ( IOException e )
		{
			Assert.fail( "Metamapper - could not read metadata");
		}
	}

	@Test
	public void testN5Viewer()
	{
		File f = new File("src/test/resources/test.n5");
		N5FSReader n5 = null;
		try
		{
			n5 = new N5FSReader( f.getAbsolutePath() );
		}
		catch ( IOException e )
		{
			Assert.fail("N5V meta - could not read n5");
		}

		N5SingleScaleMetadata n5vMetaIo = new N5SingleScaleMetadata();

		N5SingleScaleMetadata metaS0 = null;
		N5SingleScaleMetadata metaS1 = null;
		try
		{
			metaS0 = n5vMetaIo.parseMetadata( n5, "/c0/s0" );
			metaS1 = n5vMetaIo.parseMetadata( n5, "/c0/s1" );
			double[] expectXfm = new double[] {
					1.5, 0, 0, 0, 
					0, 1.5, 0, 0,
					0, 0, 1.5, 0 };

			Assert.assertEquals( "n5v units", "mm", metaS0.unit );
			Assert.assertArrayEquals( "n5v transform", 
					metaS0.transform.getRowPackedCopy(),
					expectXfm,
					1e-6 );
		}
		catch ( Exception e )
		{
			Assert.fail("N5V meta - could not read metadata");
		}

		// multiscale 
		N5TreeNode s0node = new N5TreeNode( "/c0/s0", true );
		s0node.setMetadata( metaS0 );

		N5TreeNode s1node = new N5TreeNode( "/c0/s1", true );
		s1node.setMetadata( metaS1 );

		N5TreeNode root = new N5TreeNode( "/c0", false );
		root.add( s0node );
		root.add( s1node );

		N5ViewerMultiscaleMetadataParser n5vMultiIo = new N5ViewerMultiscaleMetadataParser();
		N5MultiScaleMetadata grp = n5vMultiIo.parseMetadataGroup( root );
		Assert.assertEquals( "n5v multiscale count", 2, grp.transforms.length );
		Assert.assertEquals( "n5v s0 transform x", 1.5, grp.transforms[0].get( 0, 0 ), 1e-6 );
		Assert.assertEquals( "n5v s1 transform x", 3.0, grp.transforms[1].get( 0, 0 ), 1e-6 );
	}

	@Test
	public void testCosem()
	{
		File f = new File("src/test/resources/test.n5");
		N5FSReader n5 = null;
		try
		{
			n5 = new N5FSReader( f.getAbsolutePath() );
		}
		catch ( IOException e )
		{
			Assert.fail("Cosem meta - could not read n5");
		}
		N5CosemMetadata cosemMetaIo = new N5CosemMetadata();
		try
		{
			N5CosemMetadata metaS0 = cosemMetaIo.parseMetadata( n5, "/c0/s0" );

			Assert.assertArrayEquals( "cosem translate", 
					new double[]{2, 3, 5}, metaS0.getTransform().translate, 1e-6 );

			Assert.assertArrayEquals( "cosem scales", 
					new double[]{7, 11, 13}, metaS0.getTransform().scale, 1e-6 );

			Assert.assertArrayEquals( "cosem units", 
					new String[] { "mm", "mm", "mm" }, metaS0.getTransform().units );

			Assert.assertArrayEquals( "cosem axes", 
					new String[] { "x", "y", "z" }, metaS0.getTransform().axes );

		}
		catch ( Exception e )
		{
			Assert.fail("Cosem meta - could not read metadata");
		}
	}

	@Test
	public void testH5()
	{
		try
		{
			N5HDF5Writer n5 = new N5HDF5Writer( testDirPath + ".h5", 32, 32, 32, 32, 32 );
			testAllMetadataTypes( n5 );
		}
		catch ( IOException e )
		{
			Assert.fail("could not build n5 writer");
		}
	}

	@Test
	public void testZarr()
	{
		try
		{
			N5ZarrWriter n5 = new N5ZarrWriter( testDirPath+ ".zarr" );
			testAllMetadataTypes( n5 );

			n5.remove(  testBaseDatasetName + "/imp2d" );
			n5.remove(  testBaseDatasetName + "/imp2_cosemd" );
			n5.remove(  testBaseDatasetName + "/imp2_n5v" );

			n5.remove(  testBaseDatasetName + "/imp3d" );
			n5.remove(  testBaseDatasetName + "/imp3d_cosem" );
			n5.remove(  testBaseDatasetName + "/imp3d_n5v" );

			n5.remove(  testBaseDatasetName + "/imp4d" );
			n5.remove(  testBaseDatasetName + "/imp5d" );
		}
		catch ( IOException e )
		{
			Assert.fail("could not build n5 writer");
		}

	}
	
	@Test
	public void testN5FileSystem()
	{
		N5FSWriter n5;
		try
		{
			n5 = new N5FSWriter( testDirPath+ ".n5" );
			testAllMetadataTypes( n5 );

			n5.remove(  testBaseDatasetName + "/imp2d" );
			n5.remove(  testBaseDatasetName + "/imp2_cosemd" );
			n5.remove(  testBaseDatasetName + "/imp2_n5v" );

			n5.remove(  testBaseDatasetName + "/imp3d" );
			n5.remove(  testBaseDatasetName + "/imp3d_cosem" );
			n5.remove(  testBaseDatasetName + "/imp3d_n5v" );

			n5.remove(  testBaseDatasetName + "/imp4d" );
			n5.remove(  testBaseDatasetName + "/imp5d" );
		}
		catch ( IOException e )
		{
			Assert.fail("could not build n5 writer");
		}

	}
	
	private void testAllMetadataTypes( final N5Writer n5 )
	{
		N5ImagePlusMetadata metaWriter = new N5ImagePlusMetadata( "" );
		N5CosemMetadata cosemMeta = new N5CosemMetadata();
		N5SingleScaleMetadata n5vMeta = new N5SingleScaleMetadata();

		testReadWriteMetadata( imp2d, n5, testBaseDatasetName + "/imp2d", metaWriter );
		testReadWriteMetadata( imp2d, n5, testBaseDatasetName + "/imp2d_cosem", cosemMeta );
		testReadWriteMetadata( imp2d, n5, testBaseDatasetName + "/imp2d_n5v", n5vMeta );

		testReadWriteMetadata( imp3d, n5, testBaseDatasetName + "/imp3d", metaWriter );
		testReadWriteMetadata( imp3d, n5, testBaseDatasetName + "/imp3d_cosem", cosemMeta );
		testReadWriteMetadata( imp3d, n5, testBaseDatasetName + "/imp3d_n5v", n5vMeta );

		testReadWriteMetadata( imp4d, n5, testBaseDatasetName + "/imp4d", metaWriter );
		testReadWriteMetadata( imp5d, n5, testBaseDatasetName + "/imp5d", metaWriter );
	}
	
	private < T extends N5Metadata, W extends N5MetadataWriter< T > & N5MetadataParser< T > & ImageplusMetadata< T > > 
		void testReadWriteMetadata(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final W metaWriter )
	{
		GzipCompression compression = new GzipCompression();
		int nd = Arrays.stream( imp.getDimensions() )
					.map( x -> x > 1 ? 1 : 0 )
					.sum();

		int[] blockSize = IntStream.generate( () -> 20 ).limit( nd ).toArray();
		try
		{
			N5IJUtils.save( imp, n5, datasetName, blockSize, compression, metaWriter );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			Assert.fail( datasetName + " writing failed" );
		}

		ImagePlus impRead;
		try
		{
			impRead = N5IJUtils.load( n5, datasetName, metaWriter );
			Assert.assertTrue( datasetName + " metadata match", doImagePlusMetadataMatch( imp, impRead ));
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			Assert.fail( datasetName + " reading failed" );
		}
	}
	
	private boolean doImagePlusMetadataMatch( final ImagePlus a, final ImagePlus b )
	{
		boolean dimsMatch = Arrays.equals( a.getDimensions(), b.getDimensions() );
		if( !dimsMatch )
		{
			return false;
		}
		
		int nd = Arrays.stream( a.getDimensions() )
				.map( x -> x > 1 ? 1 : 0 )
				.sum();

		boolean wMatch = a.getCalibration().pixelWidth == b.getCalibration().pixelWidth;
		boolean hMatch = a.getCalibration().pixelHeight == b.getCalibration().pixelHeight;
		boolean unitsMatch = a.getCalibration().getUnit().equals( b.getCalibration().getUnit() );
		boolean dMatch = true;
		if( nd > 2 )
			dMatch = a.getCalibration().pixelDepth == b.getCalibration().pixelDepth;

		return wMatch && hMatch && dMatch && unitsMatch;
	}

}
