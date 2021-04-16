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
	public void testDiscoveryAndMetadata()
	{
		
		final double eps = 1e-6;

		final N5MetadataParser<?>[] parsers = new N5MetadataParser[] {
				new N5SingleScaleLegacyMetadata(),
				new N5SingleScaleMetadata() };
		
		String[] datasetList = new String[] {
				"n5v_ds", "n5v_pr", "n5v_pra",  "n5v_pra-ds",  "n5v_pr-ds" };

		final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( null, parsers );
		N5TreeNode n5root = null;
		try
		{
			n5root = discoverer.discoverRecursive( n5, "/" );
			N5DatasetDiscoverer.parseMetadata( n5, n5root, parsers, null );

			List< N5TreeNode > children = n5root.childrenList();
			Assert.assertEquals("discovery node count", 6, children.size());


			for( String dname : datasetList)
			{
				N5TreeNode n = new N5TreeNode( dname, false );
				N5DatasetDiscoverer.parseMetadata( n5, n, parsers, null );
				Assert.assertNotNull( dname, n.getMetadata() );

				System.out.println( dname );
				PhysicalMetadata m = ( PhysicalMetadata ) n.getMetadata();
				AffineTransform3D xfm = m.physicalTransform3d();
				double s = xfm.get( 0, 0 ); // scale 
				double t = xfm.get( 0, 3 ); // translation / offset
//				System.out.println( m.physicalTransform3d());
//				System.out.println( " " );
				
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
