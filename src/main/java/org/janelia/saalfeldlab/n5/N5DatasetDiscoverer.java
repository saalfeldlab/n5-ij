/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import se.sawano.java.text.AlphanumericComparator;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class N5DatasetDiscoverer {

    @SuppressWarnings( "rawtypes" )
	private final N5MetadataParser[] metadataParsers;

	private final N5GroupParser<?>[] groupParsers;

    private final Comparator<? super String> comparator;

    /**
     * Creates an N5 discoverer with alphanumeric sorting order of groups/datasets (such as, s9 goes before s10).
     *
     * @param metadataParsers
     */
	@SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer( final N5GroupParser[] groupParsers, final N5MetadataParser... metadataParsers )
    {
        this(
                Optional.of( new AlphanumericComparator(Collator.getInstance())),
                groupParsers,
                metadataParsers);
    }

    /**
     * Creates an N5 discoverer.
     *
     * If the optional parameter {@code comparator} is specified, the groups and datasets
     * will be listed in the order determined by this comparator.
     *
     * @param comparator
     * @param metadataParsers
     */
    @SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer(
            final Optional<Comparator<? super String>> comparator,
            final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
		this.comparator = comparator.orElseGet( null );
		this.groupParsers = groupParsers;
        this.metadataParsers = metadataParsers;
    }

	public N5TreeNode discoverRecursive( final N5Reader n5, final String base ) throws IOException
    {
		final N5TreeNode root = new N5TreeNode( base, n5.datasetExists( base ));
		discover( n5, root );
		parseMetadataRecursive( n5, root, metadataParsers, groupParsers );
		trim( root );
		return root;
    }

	public N5TreeNode parse( final N5Reader n5, final String dataset ) throws IOException
    {
		final N5TreeNode node = new N5TreeNode( dataset, n5.datasetExists( dataset ));
		parseMetadata( n5, node, metadataParsers, null );
		return node;
    }

    private void discover(final N5Reader n5, final N5TreeNode node) throws IOException
    {
        if( !node.isDataset() )
        {
			for ( final String childGroup : n5.list( node.path ) )
			{
				final String childPath = Paths.get( node.path, childGroup ).toString();
				final N5TreeNode childNode = new N5TreeNode( childPath, n5.datasetExists( childPath ) );
				node.add( childNode );
				discover( n5, childNode );
            }
			if ( comparator != null )
				sort( node, comparator );
        }
    }

	public static void parseMetadata(final N5Reader n5, final N5TreeNode node, 
			final N5MetadataParser< ? >[] metadataParsers,
			final N5GroupParser< ? >[] groupParsers ) throws IOException
	{
        // Go through all parsers to populate metadata
        for (final N5MetadataParser< ? > parser : metadataParsers)
        {
        	try
        	{
				node.setMetadata(parser.parseMetadata( n5, node )); 
				if (node.getMetadata() != null)
					break;
        	}
        	catch( Exception e ) {}
        }

        // If there is no matching metadata but it is a dataset, we should still be able to open it.
        // Create a single-scale metadata entry with an identity transform.
        if (node.getMetadata() == null && node.isDataset())
        {
			System.out.println( "Warning: using default metadata for " + node.path );
			// could be made more efficient if metadata store dataset attributes?
			int nd = n5.getDatasetAttributes( node.path ).getNumDimensions();
			node.setMetadata( new DefaultMetadata( node.path, nd ) );
        }
		else if ( groupParsers != null )
		{
			// this is not a dataset but may be a group (e.g. multiscale pyramid)
			// try to parse groups
			for ( N5GroupParser< ? > gp : groupParsers )
			{
				N5Metadata groupMeta = gp.parseMetadataGroup( node );
				if ( groupMeta != null )
					node.setMetadata( groupMeta );
			}
		}
	}

	public static void parseMetadataRecursive(final N5Reader n5, final N5TreeNode node, 
			final N5MetadataParser<?>[] metadataParsers,
			final N5GroupParser<?>[] groupParsers ) throws IOException
	{
        // Recursively parse metadata for children nodes
		for ( final N5TreeNode childNode : node.childrenList() )
			parseMetadataRecursive( n5, childNode, metadataParsers, groupParsers );

        parseMetadata( n5, node, metadataParsers, groupParsers );
    }

    /**
     * Removes branches of the N5 container tree that do not contain any nodes that can be opened
     * (nodes with metadata).
     *
     * @param node
     * @return
     *      {@code true} if the branch contains a node that can be opened, {@code false} otherwise
     */
    private static boolean trim(final N5TreeNode node)
    {
		List< N5TreeNode > children = node.childrenList();
		if ( children.isEmpty() )
			return node.getMetadata() != null;

        boolean ret = false;
        for (final Iterator<N5TreeNode> it = children.iterator(); it.hasNext();)
        {
            final N5TreeNode childNode = it.next();
			if ( !trim( childNode ))
			{
				node.remove( childNode );
			}
            else
                ret = true;
        }
        return ret || node.getMetadata() != null;
    }

    private static void sort(final N5TreeNode node, final Comparator<? super String> comparator)
    {
		List< N5TreeNode > children = node.childrenList();
		children.sort( Comparator.comparing( N5TreeNode::toString, comparator ) );

		// add back the children in order
		// necessary because the collection of children can't be sorted in place
		node.removeAllChildren();
		children.stream().forEach( x -> node.add( x ) );

		for ( final N5TreeNode childNode : children )
			sort( childNode, comparator );
    }
}
