/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.ij.N5IJUtils;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ViewerToImagePlus;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ij.ImagePlus;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class MetadataIoTests
{
	static private String testDirPath = createTestDirPath("n5-test");
	private static String createTestDirPath(String dirName) {
		try {
			return Files.createTempDirectory(dirName).toString();
		} catch (IOException exc) {
			return System.getProperty("user.home") + "/tmp/" + dirName;
		}
	}
	static private String testBaseDatasetName = "/test/data";

	private ImagePlus imp2d;
	private ImagePlus imp3d;
	private ImagePlus imp4d;
	private ImagePlus imp5d;

	@Before
	public void setUp() throws IOException
	{
		final ArrayImgFactory< UnsignedByteType > factory = new ArrayImgFactory<>( new UnsignedByteType() );

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
		final ImagePlusMetadataTemplate meta = new ImagePlusMetadataTemplate( "testpath", imp3d );
		final MetadataTemplateMapper metaMapper = new MetadataTemplateMapper( MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );

		N5FSWriter n5 = null;
		try
		{
			n5 = new N5FSWriter( testDirPath+ ".n5" );
			n5.createDataset( "/testpath",
				new long[]{2,3,4}, new int[] {2,3,4}, DataType.UINT8 , new RawCompression() );

			metaMapper.writeMetadata( meta, n5, "/testpath" );
		}
		catch ( final IOException e )
		{
			Assert.fail( "Metamapper - could not create writer or dataset." );
		}
		catch ( final Exception e )
		{
			Assert.fail( "Metamapper - could not write metadata");
		}

		try
		{
			final double[] res = n5.getAttribute( "/testpath", "resolution", double[].class );
			Assert.assertArrayEquals( "metamapper resolution",
					new double[]{ 3.3, 2.2, 1.1 }, res, 1e-6 );
		}
		catch ( final N5Exception e )
		{
			Assert.fail( "Metamapper - could not read metadata");
		}
	}

	@Test
	public void testN5Viewer()
	{
		final File f = new File("src/test/resources/test.n5");
		N5FSReader n5 = null;
		try
		{
			n5 = new N5FSReader( f.getAbsolutePath() );
		}
		catch ( final N5Exception e )
		{
			Assert.fail("N5V meta - could not read n5");
		}


		final N5SingleScaleMetadataParser p = new N5SingleScaleMetadataParser();
		N5SingleScaleMetadata metaS0 = null;
		N5SingleScaleMetadata metaS1 = null;
		try
		{
			metaS0 = p.parseMetadata( n5, "/c0/s0" ).get();
			metaS1 = p.parseMetadata( n5, "/c0/s1" ).get();
			final double[] expectXfm = new double[] {
					1.5, 0, 0, 0,
					0, 1.5, 0, 0,
					0, 0, 1.5, 0 };

			Assert.assertEquals( "n5v units", "mm", metaS0.unit() );
			Assert.assertArrayEquals( "n5v transform",
					metaS0.spatialTransform3d().getRowPackedCopy(),
					expectXfm,
					1e-6 );
		}
		catch ( final Exception e )
		{
			Assert.fail("N5V meta - could not read metadata");
		}

		// multiscale
		final N5TreeNode s0node = new N5TreeNode( "/c0/s0" );
		s0node.setMetadata( metaS0 );

		final N5TreeNode s1node = new N5TreeNode( "/c0/s1" );
		s1node.setMetadata( metaS1 );

		final N5TreeNode root = new N5TreeNode( "/c0" );
		root.add( s0node );
		root.add( s1node );

		final N5ViewerMultiscaleMetadataParser n5vMultiIo = new N5ViewerMultiscaleMetadataParser();

		final N5MultiScaleMetadata grp = n5vMultiIo.apply( n5, root ).get();
		final AffineTransform3D[] transforms = grp.spatialTransforms3d();
		Assert.assertEquals( "n5v multiscale count", 2, transforms.length );
		Assert.assertEquals( "n5v s0 transform x", 1.5, transforms[0].get( 0, 0 ), 1e-6 );
		Assert.assertEquals( "n5v s1 transform x", 3.0, transforms[1].get( 0, 0 ), 1e-6 );
	}

	@Test
	public void testCosem()
	{
		final File f = new File("src/test/resources/test.n5");
		N5FSReader n5 = null;
		try
		{
			n5 = new N5FSReader( f.getAbsolutePath() );
		}
		catch ( final N5Exception e )
		{
			Assert.fail("Cosem meta - could not read n5");
		}
		final N5CosemMetadataParser cosemMetaIo = new N5CosemMetadataParser();
		try
		{
			final Optional<N5CosemMetadata> metaS0Opt = cosemMetaIo.parseMetadata( n5, "/c0/s0" );
			assertTrue( "has cosem metadata", metaS0Opt.isPresent());

			final N5CosemMetadata metaS0 = metaS0Opt.get();

			Assert.assertArrayEquals( "cosem translate",
					new double[]{2, 3, 5}, metaS0.getCosemTransform().translate, 1e-6 );

			Assert.assertArrayEquals( "cosem scales",
					new double[]{7, 11, 13}, metaS0.getCosemTransform().scale, 1e-6 );

			Assert.assertArrayEquals( "cosem units",
					new String[] { "mm", "mm", "mm" }, metaS0.getCosemTransform().units );

			Assert.assertArrayEquals( "cosem axes",
					new String[] { "x", "y", "z" }, metaS0.getCosemTransform().axes );

		}
		catch ( final Exception e )
		{
			Assert.fail("Cosem meta - could not read metadata");
		}
	}

	@Test
	public void testH5()
	{
		final N5HDF5Writer n5 = new N5HDF5Writer( testDirPath + ".h5", 32, 32, 32, 32, 32 );
		testAllMetadataTypes( n5 );
	}

	@Test
	public void testZarr()
	{
		try
		{
			final N5ZarrWriter n5 = new N5ZarrWriter( testDirPath+ ".zarr" );
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
		catch ( final N5Exception e )
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
		catch ( final N5Exception e )
		{
			Assert.fail("could not build n5 writer");
		}

	}

	private void testAllMetadataTypes( final N5Writer n5 )
	{
		final ImagePlusLegacyMetadataParser metaWriter = new ImagePlusLegacyMetadataParser();

		final N5CosemMetadataParser cosemMeta = new N5CosemMetadataParser();
		final CosemToImagePlus cosemIpw = new CosemToImagePlus();

		final N5SingleScaleMetadataParser n5vMeta = new N5SingleScaleMetadataParser();
		final N5ViewerToImagePlus n5vIpw = new N5ViewerToImagePlus();

		testReadWriteMetadata(imp2d, n5, testBaseDatasetName + "/imp2d", metaWriter, metaWriter);
		testReadWriteMetadata(imp2d, n5, testBaseDatasetName + "/imp2d_cosem", cosemMeta, cosemIpw);
		testReadWriteMetadata(imp2d, n5, testBaseDatasetName + "/imp2d_n5v", n5vMeta, n5vIpw);

		testReadWriteMetadata(imp3d, n5, testBaseDatasetName + "/imp3d", metaWriter, metaWriter);
		testReadWriteMetadata( imp3d, n5, testBaseDatasetName + "/imp3d_cosem", cosemMeta, cosemIpw );
		testReadWriteMetadata(imp3d, n5, testBaseDatasetName + "/imp3d_n5v", n5vMeta, n5vIpw);

		testReadWriteMetadata(imp4d, n5, testBaseDatasetName + "/imp4d", metaWriter, metaWriter);
		testReadWriteMetadata(imp5d, n5, testBaseDatasetName + "/imp5d", metaWriter, metaWriter);
	}

	private < T extends N5DatasetMetadata, W extends N5MetadataWriter< T > & N5MetadataParser< T >, I extends ImageplusMetadata< T > >
		void testReadWriteMetadata(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final W metaWriter,
			final I ipWriter )
	{
		final GzipCompression compression = new GzipCompression();
		final int nd = Arrays.stream( imp.getDimensions() )
					.map( x -> x > 1 ? 1 : 0 )
					.sum();

		final int[] blockSize = IntStream.generate( () -> 20 ).limit( nd ).toArray();
		try
		{
			N5IJUtils.save( imp, n5, datasetName, blockSize, compression, metaWriter, ipWriter );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			Assert.fail( datasetName + " writing failed" );
		}

		ImagePlus impRead;
		try
		{
			impRead = N5IJUtils.load( n5, datasetName, metaWriter, ipWriter );
			Assert.assertTrue( datasetName + " metadata match", doImagePlusMetadataMatch( imp, impRead ));
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			Assert.fail( datasetName + " reading failed" );
		}
	}

	private boolean doImagePlusMetadataMatch( final ImagePlus a, final ImagePlus b )
	{
		final boolean dimsMatch = Arrays.equals( a.getDimensions(), b.getDimensions() );
		if( !dimsMatch )
		{
			return false;
		}

		final int nd = Arrays.stream( a.getDimensions() )
				.map( x -> x > 1 ? 1 : 0 )
				.sum();

		final boolean wMatch = a.getCalibration().pixelWidth == b.getCalibration().pixelWidth;
		final boolean hMatch = a.getCalibration().pixelHeight == b.getCalibration().pixelHeight;
		final boolean unitsMatch = a.getCalibration().getUnit().equals( b.getCalibration().getUnit() );
		boolean dMatch = true;
		if( nd > 2 )
			dMatch = a.getCalibration().pixelDepth == b.getCalibration().pixelDepth;

		return wMatch && hMatch && dMatch && unitsMatch;
	}

}
