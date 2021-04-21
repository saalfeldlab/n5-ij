package org.janelia.saalfeldlab.n5.metadata;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.realtransform.AffineTransform3D;

public class MetadataTests
{
	N5FSReader n5;

	@Before
	public void setUp() throws IOException 
	{
		final String n5Root = "src/test/resources/test.n5";
//		File n5rootF = new File( n5Root );
//		System.out.println( n5rootF.exists() + " " + n5rootF.isDirectory() );
		n5 = new N5FSReader( n5Root );
	}
	
	@Test
	public void testCosemMetadataMultiscale()
	{
		final double eps = 1e-6;
		final N5MetadataParser<?>[] parsers = new N5MetadataParser[] { new N5CosemMetadata() };

		final N5GroupParser<?>[] groupParsers = new N5GroupParser[]{
				new N5CosemMultiScaleMetadata(),
		};

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, groupParsers, parsers );

		try
		{
			N5TreeNode n5root = discoverer.discoverAndParseRecursive( "/cosem_ms" );

			Assert.assertNotNull( n5root );
			Assert.assertNotNull( n5root.getPath(), n5root.getMetadata() );
			Assert.assertTrue( "is multiscale cosem", n5root.getMetadata() instanceof N5CosemMultiScaleMetadata );

			N5CosemMultiScaleMetadata grpMeta = (N5CosemMultiScaleMetadata)n5root.getMetadata() ;
			// check ordering of paths
			Assert.assertEquals( "cosem s0", "cosem_ms/s0", grpMeta.getPaths()[0] );
			Assert.assertEquals( "cosem s1", "cosem_ms/s1", grpMeta.getPaths()[1] );
			Assert.assertEquals( "cosem s2", "cosem_ms/s2", grpMeta.getPaths()[2] );

			List< N5TreeNode > children = n5root.childrenList();
			Assert.assertEquals("discovery node count", 3, children.size());

			children.stream().forEach( n -> {
				final String dname = n.getPath();
				System.out.println( dname );

				Assert.assertNotNull( dname, n.getMetadata() );
				Assert.assertTrue( "is cosem", n.getMetadata() instanceof N5CosemMetadata );
			});
		}
		catch ( IOException e )
		{
			fail("Discovery failed");
			e.printStackTrace();
		}
	}

	@Test
	public void testCosemMetadata()
	{
		final double eps = 1e-6;
		final N5MetadataParser<?>[] parsers = new N5MetadataParser[] { new N5CosemMetadata() };

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, null, parsers );
		try
		{
			N5TreeNode n5root = discoverer.discoverAndParseRecursive( "" );

			List< N5TreeNode > children = n5root.childrenList();
			Assert.assertEquals("discovery node count", 3, children.size());

			children.stream().filter( x -> x.getPath().equals("/cosem" )).forEach( n -> {
				String dname = n.getPath();
//				try
//				{
//					N5DatasetDiscoverer.parseMetadata( n5, n, parsers, null );
//				}
//				catch ( IOException e )
//				{
//					fail("cosem parse failed");
//				}

				Assert.assertNotNull( dname, n.getMetadata() );

				Assert.assertTrue( "is cosem", n.getMetadata() instanceof N5CosemMetadata );

				N5CosemMetadata m = ( N5CosemMetadata ) n.getMetadata();
				AffineTransform3D xfm = m.physicalTransform3d();
				final double s = xfm.get( 0, 0 ); // scale 
				final double t = xfm.get( 0, 3 ); // translation / offset

				Assert.assertEquals( "cosem scale", 64, s, eps );
				Assert.assertEquals( "cosem offset", 28, t, eps );
			});
		}
		catch ( IOException e )
		{
			fail("Discovery failed");
			e.printStackTrace();
		}

	}

	@Test
	public void testN5ViewerMetadata()
	{
		final double eps = 1e-6;

		final N5MetadataParser<?>[] parsers = new N5MetadataParser[] {
//				new N5SingleScaleLegacyMetadata(),
				new N5SingleScaleMetadata() };
		
		String[] datasetList = new String[] {
				"n5v_ds", "n5v_pr", "n5v_pra",  "n5v_pra-ds",  "n5v_pr-ds" };

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( n5, null, parsers );
		N5TreeNode n5root = null;
		try
		{
			n5root = discoverer.discoverAndParseRecursive( "/" );

			List< N5TreeNode > children = n5root.childrenList();
//			Assert.assertEquals("discovery node count", 6, children.size());

			for( String dname : datasetList)
			{
				N5TreeNode n = new N5TreeNode( dname );
				N5DatasetDiscoverer.parseMetadata( n5, n, parsers, null );
				Assert.assertNotNull( dname, n.getMetadata() );

				PhysicalMetadata m = ( PhysicalMetadata ) n.getMetadata();
				AffineTransform3D xfm = m.physicalTransform3d();
				double s = xfm.get( 0, 0 ); // scale 
				double t = xfm.get( 0, 3 ); // translation / offset
				
				if( dname.contains("ds"))
				{
					if( dname.contains("pr"))
					{
						Assert.assertEquals( dname + " scale", 3.0, s, eps );
						Assert.assertEquals( dname + " offset", 0.75, t, eps );
					}
					else
					{
						Assert.assertEquals( dname + " scale", 2.0, s, eps );
						Assert.assertEquals( dname + " offset", 0.5, t, eps );
					}
				}
				else
				{
					Assert.assertEquals( dname + " scale", 1.5, s, eps );
					Assert.assertEquals( dname + " offset", 0.0, t, eps );
				}
			}
		}
		catch ( IOException e )
		{
			fail("Discovery failed");
			e.printStackTrace();
		}

	}

}
