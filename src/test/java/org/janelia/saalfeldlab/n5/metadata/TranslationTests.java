package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.transforms.ScaleOffsetSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;
import org.janelia.saalfeldlab.n5.metadata.translation.TranslatedMetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.translation.TranslatedTreeMetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.translation.InvertibleTreeTranslation;
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
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class TranslationTests {

	private File containerDir;
	private N5FSWriter n5;
	private ArrayImg<UnsignedByteType, ByteArray> img;

	private File containerDirTest;
	private N5FSWriter n5Test;

	@Before
	public void before()
	{
		URL configUrl = TransformTests.class.getResource( "/plugins.config" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "translations.n5" );
		
		final String n5TestRoot = "src/test/resources/test.n5";
		containerDirTest = new File(n5TestRoot);

		try {
			n5 = new N5FSWriter( containerDir.getCanonicalPath() );

			n5Test = new N5FSWriter( containerDirTest.getCanonicalPath(), JqUtils.gsonBuilder(null));

			int v = 0;
			img = ArrayImgs.unsignedBytes( 3, 4, 5);
			ArrayCursor<UnsignedByteType> c = img.cursor();
			while( c.hasNext())
				c.next().set( v++ );

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	@After
//	public void after() {
//		try {
//			n5.remove();
//		} catch (IOException e) {
//		}
//	}

	@Test
	public void testTransformSimple() {
		String str = "cow";
		A a = new A();
		a.a = str;

		JqFunction<A, B> tb = new JqFunction<>("{\"b\":.a }", new Gson(), B.class);
		B b = tb.apply(a);
		Assert.assertEquals(str, b.b);

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

	@Test
	public void testInvertibleTranslation() throws IOException {

		n5.createGroup("someGroup");
		final Gson gson = JqUtils.buildGson(n5);
		final ContainerMetadataNode grpNode = ContainerMetadataNode.build(n5Test, "someGroup", n5Test.getGson());

		String hasAttrs = "def hasAttributes: type == \"object\" and has(\"attributes\");";
		String defConvFwd = "def convFwd: .attributes |= (. + { \"translated\" : .orig? } | del( .orig ));";
		String defConvInv = "def convInv: .attributes |= (. + { \"orig\" : .translated? } | del( .translated ));";
		String fwd = hasAttrs + defConvFwd + " walk( if hasAttributes then convFwd else . end )";
		String inv = hasAttrs + defConvInv + " walk( if hasAttributes then convInv else . end )";
		InvertibleTreeTranslation translator = new InvertibleTreeTranslation(grpNode, gson, fwd, inv );

		translator.setAttribute("someGroup", "orig", "firstString");
		Assert.assertEquals("orig 1", "firstString", translator.getOrig().getAttribute("someGroup", "orig", String.class));
		Assert.assertEquals("translated 1", "firstString", translator.getTranslated().getAttribute("someGroup", "translated", String.class));

		translator.setTranslatedAttribute("someGroup", "translated", "secondString");
		Assert.assertEquals("translated 2", "secondString", translator.getTranslated().getAttribute("someGroup", "translated", String.class));
		Assert.assertEquals("orig 2", "secondString", translator.getOrig().getAttribute("someGroup", "orig", String.class));
	}
	
	@Test
	public void testInvertibleTranslationCosem() throws IOException {

		CosemTransform cosemTransform = new CosemTransform(
				new String[] { "z", "y", "x"}, 
				new double[] { 63, 64, 65 }, 
				new double[] { 5, 6, 7 }, 
				new String[]{"um", "um", "um"});
		
		n5.createGroup("cosem");
		n5.setAttribute("cosem", "transform", cosemTransform);
		
		n5.createGroup("cosem");
		n5.setAttribute("cosem", "transform", cosemTransform);

		final String hasAttrs = "def hasAttributes: type == \"object\" and has(\"attributes\");";
		final String defConvFwd = "def convFwd: .attributes |= cosemToTransform;";
		final String fwd = "include \"n5\"; " + hasAttrs + defConvFwd + " walk( if hasAttributes then convFwd else . end )";

//		final String defConvInv = "def convInv: .attributes |= . + { \"transform\" : scaleOffsetToCosem };";
//		final String inv = "include \"n5\"; " + hasAttrs +  defConvInv + " walk( if hasAttributes then convInv else . end )";

		final TranslatedTreeMetadataWriter canonicalToCosem = new TranslatedTreeMetadataWriter(n5,"cosem", fwd);
		canonicalToCosem.setAttribute("cosem", "transform", cosemTransform );
		canonicalToCosem.writeAllTranslatedAttributes("cosem");

		final CanonicalMetadataParser parser = new CanonicalMetadataParser();
		Optional<CanonicalMetadata> metaOpt = parser.parseMetadata(n5, "cosem");
		Assert.assertTrue( "did parse canonical", metaOpt.isPresent());
		Assert.assertTrue( "is CanonicalSpatialMetadata", metaOpt.get() instanceof CanonicalSpatialMetadata);
		SpatialMetadataCanonical spatialTransform = ((CanonicalSpatialMetadata)metaOpt.get()).getSpatialTransform();
		Assert.assertTrue( "is ScaleOffset", spatialTransform.spatialTransform() instanceof ScaleAndTranslation);
		
	}

	@Test
	public void testPathTranslation() {
		try {
			n5.createGroup("/pathXlation");
			n5.createDataset("/pathXlation/src", 
					new DatasetAttributes( new long[]{16,16}, new int[]{16,16}, DataType.UINT8, new RawCompression()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue("pathXlation src exists", n5.exists("/pathXlation/src"));

		final Gson gson = n5.getGson();
//		final ContainerMetadataNode grpNode = ContainerMetadataNode.build(n5, "pathXlation", gson );
		
		ContainerMetadataWriter treeWriter = new ContainerMetadataWriter( n5, "" );
		ContainerMetadataNode grpNode = treeWriter.getMetadataTree();
		
		String fwd = "include \"n5\"; moveSubTree( \"/pathXlation/src\"; \"/pathXlation/dst\" )";
		String inv = "include \"n5\"; moveSubTree( \"/pathXlation/dst\"; \"/pathXlation/src\" )";

		int code = 1010010001;
		InvertibleTreeTranslation translator = new InvertibleTreeTranslation(grpNode, gson, fwd, inv );
//		translator.setAttribute("pathXlation/src", "secretCode", new Integer( 1010010001 ));
		translator.setTranslatedAttribute( "pathXlation/dst", "secretCode", new Integer( code ));

		try {
			// write src
			treeWriter.setMetadataTree( translator.getOrig());
			treeWriter.writeAllAttributes();
			int parsedCode = n5.getAttribute( "pathXlation/src", "secretCode", Integer.class);
			Assert.assertEquals("parsed code src", code, parsedCode );

			// write dst
			treeWriter.setMetadataTree( translator.getTranslated());
			treeWriter.writeAllAttributes();
			parsedCode = n5.getAttribute( "pathXlation/dst", "secretCode", Integer.class);
			Assert.assertEquals("parsed code dst", code, parsedCode );

		} catch (IOException e) {
//			e.printStackTrace();
			Assert.fail( e.getMessage() );
		}

	}
	
	@Test
	public void testMetadataWriting() {
		try {
			n5.createGroup("/metaIo");
			n5.createDataset("/metaIo/writeCosem", 
					new DatasetAttributes( new long[]{16,16}, new int[]{16,16}, DataType.UINT8, new RawCompression()));
			n5.createDataset("/metaIo/writeCosemXlated", 
					new DatasetAttributes( new long[]{16,16}, new int[]{16,16}, DataType.UINT8, new RawCompression()));

			final CosemTransform cosemTransform = new CosemTransform(
					new String[] { "z", "y", "x"}, 
					new double[] { 63, 64, 65 }, 
					new double[] { 5, 6, 7 }, 
					new String[]{"um", "um", "um"});
			final N5CosemMetadata cosemMeta = new N5CosemMetadata("", cosemTransform , null);

			N5CosemMetadataParser rawParser = new N5CosemMetadataParser();
			rawParser.writeMetadata(cosemMeta, n5, "/metaIo/writeCosem");

			final String fwd = "walk( if type == \"object\" and has( \"transform\") then . +  {\"val\": 1234} else . end)";

//			// this writes to all the cosem data in the container
//			TranslatedMetadataWriter<N5CosemMetadata> xlatedWriter = new TranslatedMetadataWriter<N5CosemMetadata>( n5, fwd, rawParser );
//			xlatedWriter.writeMetadata(cosemMeta, n5, "metaIo/writeCosemXlated");

			// this writes only to the metaIo/writeCosemXlated dataset
			final String dataset = "metaIo/writeCosemXlated";
			final TranslatedMetadataWriter<N5CosemMetadata> xlatedWriter = new TranslatedMetadataWriter<N5CosemMetadata>( n5, dataset, fwd, rawParser );
			xlatedWriter.writeMetadata(cosemMeta, n5, dataset );
			
			Assert.assertNull("check writeCosem dataset not modified", n5.getAttribute("metaIo/writeCosem", "val", Integer.class));
			int val = n5.getAttribute("metaIo/writeCosemXlated", "val", Integer.class);
			Assert.assertEquals("check writeCosemXlated dataset modified", 1234, val ); 

		} catch (Exception e) {
			e.printStackTrace();
//			Assert.fail( e.getMessage());
		}

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
