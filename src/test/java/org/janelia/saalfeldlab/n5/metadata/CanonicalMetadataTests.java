package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.metadata.canonical.Axis;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.imagej.CanonicalMetadataToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import ij.ImagePlus;
import ij.ImageStack;

public class CanonicalMetadataTests {

	private ImagePlus imp3d, imp4d, imp5d;

	private Axis xAxis, yAxis, zAxis, cAxis, tAxis;

	private long[] dims3d, dims4d, dims5d;

	private int[] blkSz3d, blkSz4d, blkSz5d;

	File n5rootF;

	private N5FSReader n5;

	@Before
	public void before() {

		final String n5Root = "src/test/resources/test.n5";
		n5rootF = new File(n5Root);

		try {
			n5 = new N5FSReader( n5rootF.getCanonicalPath() );
		}catch( IOException e ) {
			e.printStackTrace();
		}

		imp3d = new ImagePlus("imp3d", ImageStack.create(1, 1, 24, 8));
		imp4d = new ImagePlus("imp4d", ImageStack.create(1, 1, 24, 8));
		imp5d = new ImagePlus("imp5d", ImageStack.create(1, 1, 24, 8));

		xAxis = new Axis("space", "x", "nm");
		yAxis = new Axis("space", "y", "nm");
		zAxis = new Axis("space", "z", "nm");
		cAxis = new Axis("channel", "c", "null");
		tAxis = new Axis("time", "t", "ms");

		dims3d = new long[] { 1, 1, 24 };
		dims4d = new long[] { 1, 1, 4, 6 };
		dims5d = new long[] { 1, 1, 2, 3, 4 };

		blkSz3d = new int[] { 8, 8, 8 };
		blkSz4d = new int[] { 8, 8, 8, 8 };
		blkSz5d = new int[] { 8, 8, 8, 8, 8 };
	}

	private double[] identity(int nd) {
		final double[] affine = new double[nd * (nd + 1)];
		int i = 0;
		for (int d = 0; d < nd; d++) {
			affine[i] = 1.0;
			i += nd + 2;
		}

		return affine;
	}

	@Test
	public void testAxesToImagePlus() throws IOException {

		CanonicalMetadataToImagePlus ipMeta = new CanonicalMetadataToImagePlus();

		// 3d tests
		final DatasetAttributes attrs3d = new DatasetAttributes(dims3d, blkSz3d, DataType.UINT8, new GzipCompression());
		double[] affineParams3d = identity( 3 );
		CanonicalDatasetMetadata xyz = makeMeta( affineParams3d, attrs3d, new Axis[] { xAxis, yAxis, zAxis } );
		CanonicalDatasetMetadata xyc = makeMeta( affineParams3d, attrs3d, new Axis[] { xAxis, yAxis, cAxis } );
		CanonicalDatasetMetadata xyt = makeMeta( affineParams3d, attrs3d, new Axis[] { xAxis, yAxis, tAxis } );

		ipMeta.writeMetadata(xyc, imp3d);
		assertEquals("xyc nc", 24, imp3d.getNChannels());
		assertEquals("xyc nz", 1, imp3d.getNSlices());
		assertEquals("xyc nt", 1, imp3d.getNFrames());

		ipMeta.writeMetadata(xyz, imp3d);
		assertEquals("xyz nc", 1, imp3d.getNChannels());
		assertEquals("xyz nz", 24, imp3d.getNSlices());
		assertEquals("xyz nt", 1, imp3d.getNFrames());

		ipMeta.writeMetadata(xyt, imp3d);
		assertEquals("xyt nc", 1, imp3d.getNChannels());
		assertEquals("xyt nz", 1, imp3d.getNSlices());
		assertEquals("xyt nt", 24, imp3d.getNFrames());

		// 4d tests
		final DatasetAttributes attrs4d = new DatasetAttributes(dims4d, blkSz4d, DataType.UINT8, new GzipCompression());
		double[] affineParams4d = identity( 4 );
		CanonicalDatasetMetadata xycz = makeMeta( affineParams4d, attrs4d, new Axis[] { xAxis, yAxis, cAxis, zAxis } );
		CanonicalDatasetMetadata xyct = makeMeta( affineParams4d, attrs4d, new Axis[] { xAxis, yAxis, cAxis, tAxis } );
		CanonicalDatasetMetadata xyzt = makeMeta( affineParams4d, attrs4d, new Axis[] { xAxis, yAxis, zAxis, tAxis } );

		ipMeta.writeMetadata(xycz, imp4d);
		assertEquals("xycz nc", 4, imp4d.getNChannels());
		assertEquals("xycz nz", 6, imp4d.getNSlices());
		assertEquals("xycz nt", 1, imp4d.getNFrames());

		ipMeta.writeMetadata(xyct, imp4d);
		assertEquals("xyct nc", 4, imp4d.getNChannels());
		assertEquals("xyct nz", 1, imp4d.getNSlices());
		assertEquals("xyct nt", 6, imp4d.getNFrames());

		ipMeta.writeMetadata(xyzt, imp4d);
		assertEquals("xyzt nc", 1, imp4d.getNChannels());
		assertEquals("xyzt nz", 4, imp4d.getNSlices());
		assertEquals("xyzt nt", 6, imp4d.getNFrames());

		// 5d tests
		final DatasetAttributes attrs5d = new DatasetAttributes(dims5d, blkSz5d, DataType.UINT8, new GzipCompression());
		double[] affineParams5d = identity( 5 );
		CanonicalDatasetMetadata xyczt = makeMeta( affineParams5d, attrs5d, new Axis[] { xAxis, yAxis, cAxis, zAxis, tAxis } );
		ipMeta.writeMetadata(xyczt, imp5d);
		assertEquals("xyczt nc", 2, imp5d.getNChannels());
		assertEquals("xyczt nz", 3, imp5d.getNSlices());
		assertEquals("xyczt nt", 4, imp5d.getNFrames());
	}

	@Test
	public void testMetadataTree()
	{
		ContainerMetadataNode rootNode = ContainerMetadataNode.build(n5, n5.getGson());
		Assert.assertNotNull("rootNode not null", rootNode);
		Assert.assertEquals( "num children of root", 9, rootNode.getChildren().keySet().size());

		Optional<ContainerMetadataNode> cosemMsNodeOpt = rootNode.getChild("cosem_ms", "/");
		Assert.assertTrue("has cosem_ms", cosemMsNodeOpt.isPresent());
		ContainerMetadataNode cosemMsNode = cosemMsNodeOpt.get();
		Assert.assertEquals( "num children of cosem_ms", 3, cosemMsNode.getChildren().keySet().size());

		Optional<ContainerMetadataNode> s0Opt = cosemMsNode.getChild("s0", "/");
		Assert.assertTrue("has s0", s0Opt.isPresent());
		ContainerMetadataNode s0 = s0Opt.get();
		Assert.assertTrue("s0 has transform attribute", s0.getAttributes().keySet().contains("transform"));
	}

	private void printDims(ImagePlus imp) {
		System.out.println(imp.getNChannels());
		System.out.println(imp.getNSlices());
		System.out.println(imp.getNFrames());
	}

	private CanonicalDatasetMetadata makeMeta(double[] affine, DatasetAttributes attrs, Axis[] axes) {
		return new CanonicalDatasetMetadata("",
				new SpatialMetadataCanonical("", new AffineSpatialTransform(affine), "mm", axes), attrs);
	}

//	public static void oneOffAxisTest() {
////		DatasetAttributes attributes = new DatasetAttributes(new long[] { 0, 0, 0, 0, 0 }, 5,
////				DataType.FLOAT32, new GzipCompression());
//		DatasetAttributes attributes = new DatasetAttributes(new long[] { 0, 0, 0 }, new int[] { 5, 5, 5 },
//				DataType.FLOAT32, new GzipCompression());
//
//		Axis[] axes = new Axis[3];
//		axes[0] = new Axis("space", "x", "mm");
//		axes[1] = new Axis("space", "y", "mm");
//		axes[2] = new Axis("space", "z", "mm");
//
//		AffineSpatialTransform affine = new AffineSpatialTransform(
//				new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 });
//		SpatialMetadataCanonical spatial = new SpatialMetadataCanonical("", affine, "mm", axes);
//
//		CanonicalDatasetMetadata meta = new CanonicalDatasetMetadata("", spatial, attributes);
//		Gson gson = TranslatedTreeMetadataParser.buildGson();
//
//		System.out.println(gson.toJson(meta));
//		System.out.println("");
//		System.out.println(gson.toJson(spatial));
//		System.out.println("");
//		System.out.println(gson.toJson(affine));
//
//	}
//
//	public static void main(String[] args) {
//
//		oneOffAxisTest();
//
//	}

}
