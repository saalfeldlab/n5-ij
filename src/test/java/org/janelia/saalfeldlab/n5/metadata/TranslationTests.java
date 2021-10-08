package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.TranslatedTreeMetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.transforms.ScaleOffsetSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;
import org.janelia.saalfeldlab.n5.metadata.translation.JqFunction;

import org.junit.Test;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.google.gson.Gson;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class TranslationTests {

	private File containerDir;
	private N5FSWriter n5;
	private ArrayImg<UnsignedByteType, ByteArray> img;

	@Before
	public void before()
	{
		URL configUrl = TransformTests.class.getResource( "/plugins.config" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "translations.n5" );

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath() );

			int v = 0;
			img = ArrayImgs.unsignedBytes( 3, 4, 5);
			ArrayCursor<UnsignedByteType> c = img.cursor();
			while( c.hasNext())
				c.next().set( v++ );

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@After
	public void after() {
		try {
			n5.remove();
		} catch (IOException e) {
		}
	}

	@Test
	public void testTransformSimple() {
		A a = new A();
		a.a = "cow";

		JqFunction<A, B> tb = new JqFunction<>("{\"b\":.a }", new Gson(), B.class);
		B b = tb.apply(a);
		Assert.assertEquals("cow", b.b);

		final double[] arr = new double[] { 0, 1, 2, 3, 4 };
		JqFunction<double[], Double> tv = new JqFunction<>(".[2]", new Gson(), Double.class);
		double val = tv.apply(arr);
		Assert.assertEquals(2, val, 1e-9);
	}

	@Test
	public void testIntensity() {
		final double[] res = new double[] { 1, 2, 3 };
		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( "um", res );

		N5vMetaObj n5vMetaObj = new N5vMetaObj();
		n5vMetaObj.pixelResolution = voxdims;

		N5vMetaArr n5vMetaArr = new N5vMetaArr();
		n5vMetaArr.pixelResolution = res;

		JqFunction<N5vMetaObj,CanonicalSpatialDatasetMetadata > f = new JqFunction<>(
				"include \"n5\"; . + { \"intensityLimits\": { "
				+ " \"min\":2, \"max\" : 222 }} ",
				JqUtils.buildGson(null),
				CanonicalSpatialDatasetMetadata.class);	

		CanonicalSpatialDatasetMetadata metaOut = f.apply(n5vMetaObj);
		Assert.assertEquals(2, metaOut.minIntensity(), 1e-9);
		Assert.assertEquals(222, metaOut.maxIntensity(), 1e-9);
	}

	@Test
	public void testN5v() {
		final double[] res = new double[] { 1, 2, 3 };
		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( "um", res );

		N5vMetaObj n5vMetaObj = new N5vMetaObj();
		n5vMetaObj.pixelResolution = voxdims;

		N5vMetaArr n5vMetaArr = new N5vMetaArr();
		n5vMetaArr.pixelResolution = res;

		JqFunction<N5vMetaObj,CanonicalSpatialDatasetMetadata > f = new JqFunction<>(
				"include \"n5\"; n5vToCanonicalScaleOffset",
				JqUtils.buildGson(null),
				CanonicalSpatialDatasetMetadata.class);	
		CanonicalSpatialDatasetMetadata metaOut = f.apply(n5vMetaObj);

		SpatialTransform spatialTransform = metaOut.getSpatialTransform().transform();
		Assert.assertTrue( spatialTransform instanceof ScaleOffsetSpatialTransform);

		ScaleOffsetSpatialTransform scaleOffset = (ScaleOffsetSpatialTransform)spatialTransform; 
		Assert.assertArrayEquals(new double[]{1, 2, 3}, scaleOffset.scale, 1e-9);
		Assert.assertArrayEquals(new double[]{0, 0, 0}, scaleOffset.offset, 1e-9);
	}

	@Test
	public void testCosem() {
		final CosemTransform ct = new CosemTransform(
				new String[]{"z", "y", "x"},
				new double[]{3, 2, 1 },
				new double[]{0.3, 0.2, 0.1 },
				new String[]{"um", "um", "um"} );

		CosemMeta metaIn = new CosemMeta();
		metaIn.transform = ct;

		JqFunction<CosemMeta,CanonicalSpatialDatasetMetadata > f = new JqFunction<>(
				"include \"n5\"; cosemToTransform",
				JqUtils.buildGson(null),
				CanonicalSpatialDatasetMetadata.class);	
		CanonicalSpatialDatasetMetadata metaOut = f.apply(metaIn);

		SpatialTransform spatialTransform = metaOut.getSpatialTransform().transform();
		Assert.assertTrue( spatialTransform instanceof ScaleOffsetSpatialTransform);

		ScaleOffsetSpatialTransform scaleOffset = (ScaleOffsetSpatialTransform)spatialTransform; 
		Assert.assertArrayEquals(new double[]{1, 2, 3}, scaleOffset.scale, 1e-9);
		Assert.assertArrayEquals(new double[]{0.1, 0.2, 0.3}, scaleOffset.offset, 1e-9);
	}

	@Test
	public void testTranslationAttributeWriting() throws IOException {

		N5Utils.save( img, n5, "attr", new int[] {5, 5, 5}, new GzipCompression());
		N5Utils.save( img, n5, "a/attr2", new int[] {5, 5, 5}, new GzipCompression());

		final String translation = "include \"n5\"; walk( if isAttributes then . + {\"myint\": 12345 } else . end)";
		TranslatedTreeMetadataWriter attrWriter = new TranslatedTreeMetadataWriter( n5, translation );
		attrWriter.writeAllTranslatedAttributes("attr");

		int myint = n5.getAttribute("attr", "myint", Integer.class);
		Assert.assertEquals(12345, myint );

		// "clear" myint attribute
		n5.setAttribute("attr", "myint", "null" );

		final String translation2 = "include \"n5\"; walk( if isAttributes then . + {\"myint\": 54321 } else . end)";
		TranslatedTreeMetadataWriter attrWriter2 = new TranslatedTreeMetadataWriter( n5, translation2 );
		attrWriter2.writeTranslatedAttribute("attr","myint");

		myint = n5.getAttribute("attr", "myint", Integer.class);
		Assert.assertEquals(54321, myint);

		final String translationAll = "include \"n5\"; walk( if isAttributes then . + {\"addedP\": .path } else . end)";
		TranslatedTreeMetadataWriter attrWriterAll = new TranslatedTreeMetadataWriter( n5, translationAll );
		attrWriterAll.writeAllTranslatedAttributes();
		Assert.assertEquals("/a/attr2", n5.getAttribute("a/attr2", "addedP", String.class));
	}

	private static class A {
		public String a;
	}

	private static class B {
		public String b;
	}

	private static class CosemMeta {
		public CosemTransform transform;
	}

	private static class N5vMetaObj {
		public FinalVoxelDimensions pixelResolution;
	}

	private static class N5vMetaArr {
		public double[] pixelResolution;
	}
}
