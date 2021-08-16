package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class MetadataTests {

  N5FSReader n5;

  @Before
  public void setUp() throws IOException {

	final String n5Root = "src/test/resources/test.n5";
	File n5rootF = new File(n5Root);
	n5 = new N5FSReader(n5Root);
  }

  @Test
  public void testCosemMetadataMultiscale() {

	final double eps = 1e-6;
	final N5MetadataParser<?>[] parsers = new N5MetadataParser[]{new N5CosemMetadataParser()};
	final N5MetadataParser<?>[] grpparsers = new N5MetadataParser[]{new N5CosemMultiScaleMetadata.CosemMultiScaleParser()};

	final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(
			n5,
			Arrays.asList(parsers),
			Arrays.asList(grpparsers));

	try {
	  final N5TreeNode n5root = discoverer.discoverAndParseRecursive("/cosem_ms");

	  Assert.assertNotNull(n5root.getPath(), n5root.getMetadata());
	  Assert.assertTrue("is multiscale cosem", n5root.getMetadata() instanceof N5CosemMultiScaleMetadata);

	  N5CosemMultiScaleMetadata grpMeta = (N5CosemMultiScaleMetadata)n5root.getMetadata();
	  // check ordering of paths
	  Assert.assertEquals("cosem s0", "cosem_ms/s0", grpMeta.getPaths()[0]);
	  Assert.assertEquals("cosem s1", "cosem_ms/s1", grpMeta.getPaths()[1]);
	  Assert.assertEquals("cosem s2", "cosem_ms/s2", grpMeta.getPaths()[2]);

	  List<N5TreeNode> children = n5root.childrenList();
	  Assert.assertEquals("discovery node count", 3, children.size());

	  children.stream().forEach(n -> {
		final String dname = n.getPath();

		Assert.assertNotNull(dname, n.getMetadata());
		Assert.assertTrue("is cosem", n.getMetadata() instanceof N5CosemMetadata);
	  });
	} catch (IOException e) {
	  fail("Discovery failed");
	  e.printStackTrace();
	}
  }

  @Test
  public void testCosemMetadata() {

	final double eps = 1e-6;

	final List<N5MetadataParser<?>>
			parsers = Collections.singletonList(new N5CosemMetadataParser());

	final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(
			n5, parsers, new ArrayList<>());

	try {
	  final N5TreeNode n5root = discoverer.discoverAndParseRecursive("/");

	  List<N5TreeNode> children = n5root.childrenList();
	  Assert.assertEquals("discovery node count", 3, children.size());

	  children.stream().filter(x -> x.getPath().equals("/cosem")).forEach(n -> {
		String dname = n.getPath();

		Assert.assertNotNull(dname, n.getMetadata());

		Assert.assertTrue("is cosem", n.getMetadata() instanceof N5CosemMetadata);

		N5CosemMetadata m = (N5CosemMetadata)n.getMetadata();
		AffineTransform3D xfm = m.spatialTransform3d();
		final double s = xfm.get(0, 0); // scale
		final double t = xfm.get(0, 3); // translation / offset

		Assert.assertEquals("cosem scale", 64, s, eps);
		Assert.assertEquals("cosem offset", 28, t, eps);
	  });
	} catch (IOException e) {
	  fail("Discovery failed");
	  e.printStackTrace();
	}
  }

  @Test
  public void testN5ViewerMetadata() {

	final double eps = 1e-6;

	final List<N5MetadataParser<?>> parsers = Collections.singletonList(new N5SingleScaleMetadataParser());

	final String[] datasetList = new String[]{"n5v_ds", "n5v_pr", "n5v_pra", "n5v_pra-ds", "n5v_pr-ds"};
	final Set<String> datasetSet = Stream.of(datasetList).collect(Collectors.toSet());

	final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, parsers, null);
	try {
	  final N5TreeNode n5root = discoverer.discoverAndParseRecursive("/");

	  List<N5TreeNode> childrenWithMetadata = n5root.childrenList().stream()
			  .filter(x -> Objects.nonNull(x.getMetadata()))
			  .collect(Collectors.toList());
	  long childrenNoMetadataCount = n5root.childrenList().stream()
			  .filter(x -> Objects.isNull(x.getMetadata()))
			  .count();
	  Assert.assertEquals("discovery node count with single scale metadata", 6, childrenWithMetadata.size());
	  Assert.assertEquals("discovery node count without single scale metadata", 3, childrenNoMetadataCount);

	  childrenWithMetadata.stream().filter(x -> datasetSet.contains(x.getPath())).forEach(n -> {

		final String dname = n.getPath();
		Assert.assertNotNull(dname, n.getMetadata());

		SpatialMetadata m = (SpatialMetadata)n.getMetadata();
		AffineTransform3D xfm = m.spatialTransform3d();
		double s = xfm.get(0, 0); // scale
		double t = xfm.get(0, 3); // translation / offset

		if (dname.contains("ds")) {
		  if (dname.contains("pr")) {
			Assert.assertEquals(dname + " scale", 3.0, s, eps);
			Assert.assertEquals(dname + " offset", 0.75, t, eps);
		  } else {
			Assert.assertEquals(dname + " scale", 2.0, s, eps);
			Assert.assertEquals(dname + " offset", 0.5, t, eps);

		  }
		} else {
		  Assert.assertEquals(dname + " scale", 1.5, s, eps);
		  Assert.assertEquals(dname + " offset", 0.0, t, eps);
		}
	  });
	} catch (IOException e) {
	  fail("Discovery failed");
	  e.printStackTrace();
	}

  }

  @Test
  public void testGenericMetadata() {

	final double eps = 1e-6;

	final N5DatasetDiscoverer discoverer= new N5DatasetDiscoverer(n5, 
			Collections.singletonList( 
					N5GenericSingleScaleMetadataParser.builder().resolution("pixelResolution")
					.build()),
			null);

	final N5DatasetDiscoverer discovererDf= new N5DatasetDiscoverer(n5, 
			Collections.singletonList( 
					N5GenericSingleScaleMetadataParser.builder().resolution("pixelResolution")
					.downsamplingFactors("downsamplingFactors").build()),
			null);

	final N5DatasetDiscoverer discovererRes= new N5DatasetDiscoverer(n5, 
			Collections.singletonList( 
					N5GenericSingleScaleMetadataParser.builder().resolution("res").build()),
			null);
	final N5DatasetDiscoverer discovererResOff = new N5DatasetDiscoverer(n5, 
			Collections.singletonList( 
					N5GenericSingleScaleMetadataParser.builder().offset("off").resolution("res").build()),
			null);

	try {
	  final N5TreeNode n5pra = discoverer.discoverAndParseRecursive("n5v_pra");
	  Assert.assertNotNull("n5v_pra metadata", n5pra.getMetadata());
	  SpatialMetadata m = (SpatialMetadata)n5pra.getMetadata();
	  AffineTransform3D xfm = m.spatialTransform3d();
	  Assert.assertEquals("n5v_pra generic scale", 1.5, xfm.get(0, 0), eps);
	  Assert.assertEquals("n5v_pra generic offset", 0.0, xfm.get(0, 3), eps);

	  final N5TreeNode n5prads = discovererDf.discoverAndParseRecursive("n5v_pra-ds");
	  Assert.assertNotNull("n5v_pra_ds metadata", n5prads.getMetadata());
	  SpatialMetadata mds = (SpatialMetadata)n5prads.getMetadata();
	  AffineTransform3D xfmds = mds.spatialTransform3d();
	  Assert.assertEquals("n5v_pra_ds generic scale", 3.0, xfmds.get(0, 0), eps);
	  Assert.assertEquals("n5v_pra_ds generic offset", 0.75, xfmds.get(0, 3), eps);

	  final N5TreeNode nodeRes = discovererRes.discoverAndParseRecursive("others/res");
	  Assert.assertNotNull("res metadata", nodeRes.getMetadata());
	  SpatialMetadata metaRes = (SpatialMetadata)nodeRes.getMetadata();
	  AffineTransform3D xfmRes = metaRes.spatialTransform3d();
	  Assert.assertEquals("res generic scale", 1.5, xfmRes.get(0, 0), eps);
	  Assert.assertEquals("res generic offset", 0.0, xfmRes.get(0, 3), eps);

	  final N5TreeNode nodeResOff = discovererResOff.discoverAndParseRecursive("others/resOff");
	  Assert.assertNotNull("res metadata", nodeResOff.getMetadata());
	  SpatialMetadata metaResOff = (SpatialMetadata)nodeResOff.getMetadata();
	  AffineTransform3D xfmResOff = metaResOff.spatialTransform3d();
	  Assert.assertEquals("resOff generic scale", 1.5, xfmResOff.get(0, 0), eps);
	  Assert.assertEquals("resOff generic offset", 12.3, xfmResOff.get(0, 3), eps);

	} catch (IOException e) {
	  fail("Discovery failed");
	  e.printStackTrace();
	}

  }

}
