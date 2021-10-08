package org.janelia.saalfeldlab.n5.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.ScaleOffsetSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.ScaleSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SequenceSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.TranslationSpatialTransform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;


public class TransformTests {

	private File containerDir;

	private N5Writer n5;

	private ArrayImg<UnsignedByteType, ByteArray> img;

	private double[] translation = new double[] { 100, 200, 300 };

	private double[] scale = new double[] { 1.1, 2.2, 3.3 };

	private double[] so = new double[] { 1.9, 0.1, 2.8, 0.2, 3.7, 0.3 };
	private double[] soScale = new double[] { 1.9, 2.8, 3.7 };
	private double[] soOffset = new double[] { 0.1, 0.2, 0.3 };

	private double[] affine = new double[] { 
			1.1, 0.1, 0.2, -10,
			0.3, 2.2, 0.4, -20,
			0.5, 0.6, 3.3, -30 };

	@Before
	public void before()
	{
		URL configUrl = TransformTests.class.getResource( "/plugins.config" );
		File baseDir = new File( configUrl.getFile() ).getParentFile();
		containerDir = new File( baseDir, "transforms.n5" );

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
	public void after()
	{
		try {
			n5.remove();
		} catch (IOException e) { }
	}

	@Test
	public void testParametrizedTransforms() throws IOException {

		// translation
		ArrayImg<DoubleType, DoubleArray> translationParams = ArrayImgs.doubles( translation, 3 );
		N5Utils.save( translationParams, n5, "translation", new int[]{3}, new GzipCompression());

		// scale
		ArrayImg<DoubleType, DoubleArray> scaleParams = ArrayImgs.doubles( scale, 3 );
		N5Utils.save( scaleParams, n5, "scale", new int[]{3}, new GzipCompression());

		// scale-offset
		ArrayImg<DoubleType, DoubleArray> scaleOffsetParams = ArrayImgs.doubles( so, 2, 3 );
		N5Utils.save( scaleOffsetParams, n5, "scale_offset", new int[]{3,3}, new GzipCompression());

		// affine
		ArrayImg<DoubleType, DoubleArray> affineParams = ArrayImgs.doubles( affine, 12 );
		N5Utils.save( affineParams, n5, "affine", new int[]{12}, new GzipCompression());

		final ScaleSpatialTransform scaleTransform = new ScaleSpatialTransform( "scale" );
		final TranslationSpatialTransform translationTransform = new TranslationSpatialTransform( "translation" );
		final AffineSpatialTransform affineTransform = new AffineSpatialTransform( "affine" );
		final ScaleOffsetSpatialTransform scaleOffsetTransform = new ScaleOffsetSpatialTransform( "scale_offset" );
		final SequenceSpatialTransform seq = new SequenceSpatialTransform( 
				new SpatialTransform[]{ affineTransform, scaleTransform, scaleOffsetTransform, translationTransform });

		// make an image
		N5Utils.save( img, n5, "imgParam", new int[] {5, 5, 5}, new GzipCompression());

		// set the transform metadata
		SpatialMetadataCanonical transform = new SpatialMetadataCanonical(null, seq, "pixel", null);
		n5.setAttribute("imgParam", "spatialTransform", transform);

		testParsedTransformSeq("/imgParam");
	}

	@Test
	public void testTransforms() throws IOException {

		final ScaleSpatialTransform scaleTransform = new ScaleSpatialTransform( scale );
		final TranslationSpatialTransform translationTransform = new TranslationSpatialTransform( translation );
		final ScaleOffsetSpatialTransform scaleOffsetTransform = new ScaleOffsetSpatialTransform(soScale, soOffset);
		final AffineSpatialTransform affineTransform = new AffineSpatialTransform( affine );
		final SequenceSpatialTransform seq = new SequenceSpatialTransform( 
				new SpatialTransform[]{ affineTransform, scaleTransform, scaleOffsetTransform, translationTransform });

		// make an image
		N5Utils.save( img, n5, "img", new int[] {5, 5, 5}, new GzipCompression());

		// set the transform metadata
		SpatialMetadataCanonical transform = new SpatialMetadataCanonical(null, seq, "pixel", null);
		n5.setAttribute("img", "spatialTransform", transform);

		testParsedTransformSeq("img");
	}

	private void testParsedTransformSeq( String dataset )
	{
		// canonical parser
//		final TranslatedTreeMetadataParser parser = new TranslatedTreeMetadataParser( n5, "." );
		final CanonicalMetadataParser parser = new CanonicalMetadataParser();
		Optional<CanonicalMetadata> metaOpt = parser.parseMetadata(n5, dataset);
		CanonicalMetadata meta = metaOpt.get();
		Assert.assertTrue("meta parsed as CanonicalSpatialDatasetMetadata", meta instanceof CanonicalSpatialDatasetMetadata );
		SpatialMetadataCanonical parsedXfm = ((CanonicalSpatialDatasetMetadata)meta).getSpatialTransform();

		Assert.assertTrue("parsed as sequence", parsedXfm.transform() instanceof SequenceSpatialTransform);
		SequenceSpatialTransform parsedSeq = (SequenceSpatialTransform)parsedXfm.transform();
		SpatialTransform transform0 = parsedSeq.getTransformations()[0];
		SpatialTransform transform1 = parsedSeq.getTransformations()[1];
		SpatialTransform transform2 = parsedSeq.getTransformations()[2];
		SpatialTransform transform3 = parsedSeq.getTransformations()[3];

		Assert.assertTrue("transform0 is affine", transform0 instanceof AffineSpatialTransform);
		Assert.assertArrayEquals( "parsed affine parameters", affine, ((AffineSpatialTransform)transform0).affine, 1e-9 );

		Assert.assertTrue("transform1 is scale", transform1 instanceof ScaleSpatialTransform);
		Assert.assertArrayEquals( "parsed scale parameters", scale, ((ScaleSpatialTransform)transform1).scale, 1e-9 );

		Assert.assertTrue("transform2 is scale", transform2 instanceof ScaleOffsetSpatialTransform);
		Assert.assertArrayEquals( "parsed scaleOffset scale parameters", soScale, ((ScaleOffsetSpatialTransform)transform2).scale, 1e-9 );
		Assert.assertArrayEquals( "parsed scaleOffset offset parameters", soOffset, ((ScaleOffsetSpatialTransform)transform2).offset, 1e-9 );

		Assert.assertTrue("transform3 is translation", transform3 instanceof TranslationSpatialTransform);
		Assert.assertArrayEquals( "parsed translation parameters", translation, ((TranslationSpatialTransform)transform3).translation, 1e-9 );
	}

}
