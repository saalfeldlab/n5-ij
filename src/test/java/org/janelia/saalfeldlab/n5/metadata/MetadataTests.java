package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.TestRunners;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetadataTests {

	private static N5FSReader n5;

	@BeforeClass
	public static void setUp() throws IOException {

		final String n5Root = "src/test/resources/test.n5";
		n5 = new N5FSReader(n5Root);
	}

	@Test
	public void testCosemMetadataMultiscale() {

		final N5MetadataParser<?>[] parsers = new N5MetadataParser[]{new N5CosemMetadataParser()};
		final N5MetadataParser<?>[] grpparsers = new N5MetadataParser[]{new N5CosemMultiScaleMetadata.CosemMultiScaleParser()};

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(
				n5,
				Arrays.asList(parsers),
				Arrays.asList(grpparsers));

		TestRunners.tryWaitRepeat( () -> {
			try {
				return discoverer.discoverAndParseRecursive("/cosem_ms");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent( n5root -> {

			assertNotNull(n5root.getPath(), n5root.getMetadata());
			assertTrue("is multiscale cosem", n5root.getMetadata() instanceof N5CosemMultiScaleMetadata);

			N5CosemMultiScaleMetadata grpMeta = (N5CosemMultiScaleMetadata)n5root.getMetadata();
			// check ordering of paths
			assertEquals("cosem s0", "cosem_ms/s0", grpMeta.getPaths()[0]);
			assertEquals("cosem s1", "cosem_ms/s1", grpMeta.getPaths()[1]);
			assertEquals("cosem s2", "cosem_ms/s2", grpMeta.getPaths()[2]);

			final List<N5TreeNode> children = n5root.childrenList();
			Assert.assertEquals("discovery node count", 3, children.size());

			children.stream().forEach(n -> {
				final String dname = n.getPath();

				assertNotNull(dname, n.getMetadata());
				assertTrue("is cosem", n.getMetadata() instanceof N5CosemMetadata);
			});
			
		});
	}

	@Test
	public void testCosemMetadata() {

		final double eps = 1e-6;

		final List<N5MetadataParser<?>> parsers = Collections.singletonList(new N5CosemMetadataParser());

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(
				n5, parsers, new ArrayList<>());

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discoverer.discoverAndParseRecursive("/");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(n5root -> {

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

		});
	}

	@Test
	public void testN5ViewerMetadata() {

		final double eps = 1e-6;

		final List<N5MetadataParser<?>> parsers = Collections.singletonList(new N5SingleScaleMetadataParser());

		final String[] datasetList = new String[]{"n5v_ds", "n5v_pr", "n5v_pra", "n5v_pra-ds", "n5v_pr-ds"};
		final Set<String> datasetSet = Stream.of(datasetList).collect(Collectors.toSet());

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, parsers, null);

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discoverer.discoverAndParseRecursive("/");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(n5root -> {

			List<N5TreeNode> childrenWithMetadata = n5root.childrenList().stream()
					.filter(x -> Objects.nonNull(x.getMetadata()))
					.collect(Collectors.toList());
			long childrenNoMetadataCount = n5root.childrenList().stream()
					.filter(x -> Objects.isNull(x.getMetadata()))
					.count();
			Assert.assertEquals("discovery node count with single scale metadata", 4, childrenWithMetadata.size());
			Assert.assertEquals("discovery node count without single scale metadata", 1, childrenNoMetadataCount);

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

		});
	}

	@Test
	public void testGenericMetadata() {

		final double eps = 1e-6;

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5,
				Collections.singletonList(
						N5GenericSingleScaleMetadataParser.builder().resolution("pixelResolution")
								.build()),
				null);

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discoverer.discoverAndParseRecursive("n5v_pra");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(n5pra -> {

			assertNotNull("n5v_pra metadata", n5pra.getMetadata());
			SpatialMetadata m = (SpatialMetadata)n5pra.getMetadata();
			AffineTransform3D xfm = m.spatialTransform3d();
			assertEquals("n5v_pra generic scale", 1.5, xfm.get(0, 0), eps);
			assertEquals("n5v_pra generic offset", 0.0, xfm.get(0, 3), eps);

		});

		final N5DatasetDiscoverer discovererDf = new N5DatasetDiscoverer(n5,
				Collections.singletonList(
						N5GenericSingleScaleMetadataParser.builder().resolution("pixelResolution")
								.downsamplingFactors("downsamplingFactors").build()),
				null);

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discovererDf.discoverAndParseRecursive("n5v_pra-ds");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(n5prads -> {

			assertNotNull("n5v_pra_ds metadata", n5prads.getMetadata());
			SpatialMetadata mds = (SpatialMetadata)n5prads.getMetadata();
			AffineTransform3D xfmds = mds.spatialTransform3d();
			assertEquals("n5v_pra_ds generic scale", 3.0, xfmds.get(0, 0), eps);
			assertEquals("n5v_pra_ds generic offset", 0.75, xfmds.get(0, 3), eps);

		});
		
		
		final N5DatasetDiscoverer discovererRes = new N5DatasetDiscoverer(n5,
				Collections.singletonList(
						N5GenericSingleScaleMetadataParser.builder().resolution("res").build()),
				null);

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discovererRes.discoverAndParseRecursive("others/res");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(nodeRes -> {
			assertNotNull("res metadata", nodeRes.getMetadata());
			SpatialMetadata metaRes = (SpatialMetadata)nodeRes.getMetadata();
			AffineTransform3D xfmRes = metaRes.spatialTransform3d();
			assertEquals("res generic scale", 1.5, xfmRes.get(0, 0), eps);
			assertEquals("res generic offset", 0.0, xfmRes.get(0, 3), eps);
		});
		
		final N5DatasetDiscoverer discovererResOff = new N5DatasetDiscoverer(n5,
				Collections.singletonList(
						N5GenericSingleScaleMetadataParser.builder().offset("off").resolution("res").build()),
				null);

		TestRunners.tryWaitRepeat(() -> {
			try {
				return discovererResOff.discoverAndParseRecursive("others/resOff");
			} catch (IOException e) {
				return null; // so that the runner tries again
			}
		}).ifPresent(nodeResOff -> {
			assertNotNull("res metadata", nodeResOff.getMetadata());
			SpatialMetadata metaResOff = (SpatialMetadata)nodeResOff.getMetadata();
			AffineTransform3D xfmResOff = metaResOff.spatialTransform3d();
			assertEquals("resOff generic scale", 1.5, xfmResOff.get(0, 0), eps);
			assertEquals("resOff generic offset", 12.3, xfmResOff.get(0, 3), eps);
		});

	}

}
