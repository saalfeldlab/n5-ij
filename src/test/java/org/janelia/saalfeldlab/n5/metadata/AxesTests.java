package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RunImportExportTest;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.metadata.axes.DefaultDatasetAxisMetadata;
import org.janelia.saalfeldlab.n5.metadata.axes.DefaultDatasetAxisMetadataParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;

public class AxesTests {

	File containerDir;

	N5Writer n5;

	@Before
	public void before()
	{
		URL configUrl = RunImportExportTest.class.getResource( "/plugins.config" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "axis.n5" );

		int v = 0;
		ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs.unsignedBytes(2, 3, 4, 5);
		ArrayCursor<UnsignedByteType> c = img.cursor();
		while( c.hasNext())
			c.next().set( v++ );

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath() );

			N5Utils.save( img, n5, "xycz", new int[] {5,5,5,5}, new GzipCompression());
			n5.setAttribute( "xycz", "axes", new String[]{"x", "y", "c", "z"});

			// xycz
			N5Utils.save( img, n5, "xyzc", new int[] {5,5,5,5}, new GzipCompression());
			n5.setAttribute( "xyzc", "axes", new String[]{"x", "y", "z", "c" });

			// yxzc
			N5Utils.save( img, n5, "yxzc", new int[] {5,5,5,5}, new GzipCompression());
			n5.setAttribute( "yxzc", "axes", new String[]{"y", "x", "z", "c" });

			// xytz
			N5Utils.save( img, n5, "xytz", new int[] {5,5,5,5}, new GzipCompression());
			n5.setAttribute( "xytz", "axes", new String[]{"x", "y", "t", "z" });

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after()
	{
		try {
			n5.remove();
		} catch (IOException e) { }
	}

	@Test
	public void testAxisPermutations() throws IOException {

		final DefaultDatasetAxisMetadataParser parser = new DefaultDatasetAxisMetadataParser();

		Optional<DefaultDatasetAxisMetadata> yxzcMetaOpt = parser.parseMetadata(n5, "/yxzc");
		Assert.assertTrue("yxzc axes parsed", yxzcMetaOpt.isPresent());
		int[] p = AxisUtils.findImagePlusPermutation(yxzcMetaOpt.get());
		Assert.assertArrayEquals("yxzc permutation",  new int[]{ 1, 0, 3, 2, -1 }, p);
		CachedCellImg<?, ?> img = N5Utils.open(n5, "/yxzc");
		long[] pDims = Intervals.dimensionsAsLongArray( AxisUtils.permuteForImagePlus( img, yxzcMetaOpt.get() ));
		Assert.assertArrayEquals("yxzc size", new long[]{3, 2, 5, 4, 1}, pDims );

		Optional<DefaultDatasetAxisMetadata> xyzcMetaOpt = parser.parseMetadata(n5, "/xyzc");
		Assert.assertTrue("xyzc axes parsed", xyzcMetaOpt.isPresent());
		p = AxisUtils.findImagePlusPermutation(xyzcMetaOpt.get());
		Assert.assertArrayEquals("xyzc permutation",  new int[]{ 0, 1, 3, 2, -1 }, p);
		img = N5Utils.open(n5, "/xyzc");
		pDims = Intervals.dimensionsAsLongArray( AxisUtils.permuteForImagePlus( img, xyzcMetaOpt.get() ));
		Assert.assertArrayEquals("xyzc size", new long[]{2, 3, 5, 4, 1}, pDims );

		Optional<DefaultDatasetAxisMetadata> xyczMetaOpt = parser.parseMetadata(n5, "/xycz");
		Assert.assertTrue("xycz axes parsed", xyczMetaOpt.isPresent());
		p = AxisUtils.findImagePlusPermutation(xyczMetaOpt.get());
		Assert.assertArrayEquals("xycz permutation",  new int[]{ 0, 1, 2, 3, -1 }, p);
		img = N5Utils.open(n5, "/xycz");
		pDims = Intervals.dimensionsAsLongArray( AxisUtils.permuteForImagePlus( img, xyczMetaOpt.get() ));
		Assert.assertArrayEquals("xycz size", new long[]{2, 3, 4, 5, 1}, pDims );

		Optional<DefaultDatasetAxisMetadata> xytzMetaOpt = parser.parseMetadata(n5, "/xytz");
		Assert.assertTrue("xytz axes parsed", xytzMetaOpt.isPresent());
		p = AxisUtils.findImagePlusPermutation(xytzMetaOpt.get());
		Assert.assertArrayEquals("xytz permutation",  new int[]{ 0, 1, -1, 3, 2 }, p);
		img = N5Utils.open(n5, "/xytz");
		pDims = Intervals.dimensionsAsLongArray( AxisUtils.permuteForImagePlus( img, xytzMetaOpt.get() ));
		Assert.assertArrayEquals("xytz size", new long[]{2, 3, 1, 5, 4}, pDims );
	}

}
