/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

import se.sawano.java.text.AlphanumericComparator;

public class N5DatasetDiscoverer {

    @SuppressWarnings( "rawtypes" )
	private final N5MetadataParser[] metadataParsers;

	private final N5GroupParser<?>[] groupParsers;

    private final Comparator<? super String> comparator;

    private final Predicate< N5TreeNode > filter;

    private final ExecutorService executor;

    private N5TreeNode root;

	private String groupSeparator;

	private N5Reader n5;

    /**
     * Creates an N5 discoverer with alphanumeric sorting order of groups/datasets (such as, s9 goes before s10).
     * 
     * @param executor the executor
     * @param groupParsers group parsers
     * @param metadataParsers metadata parsers
     */
	@SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer( final ExecutorService executor, final N5GroupParser[] groupParsers, final N5MetadataParser... metadataParsers )
    {
        this( executor,
              Optional.of( new AlphanumericComparator(Collator.getInstance())),
              null,
              groupParsers,
              metadataParsers);
    }
	
	@SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer( 
			final N5Reader n5,
			final ExecutorService executor, final N5GroupParser[] groupParsers, final N5MetadataParser... metadataParsers )
    {
        this( n5,
        	  executor,
              Optional.of( new AlphanumericComparator(Collator.getInstance())),
              null,
              groupParsers,
              metadataParsers);
    }

	/**
     * Creates an N5 discoverer with alphanumeric sorting order of groups/datasets (such as, s9 goes before s10).
	 * 
	 * @param groupParsers group parsers
	 * @param metadataParsers metadata parsers
	 */
	@SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer( final N5GroupParser[] groupParsers, final N5MetadataParser... metadataParsers )
    {
        this( Executors.newSingleThreadExecutor(),
              Optional.of( new AlphanumericComparator(Collator.getInstance())),
              null,
              groupParsers,
              metadataParsers);
    }
	
	@SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer( final N5Reader n5,
			final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers )
    {
        this( n5,
        	  Executors.newSingleThreadExecutor(),
              Optional.of( new AlphanumericComparator(Collator.getInstance())),
              null,
              groupParsers,
              metadataParsers);
    }

	@SuppressWarnings("rawtypes")
	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Predicate< N5TreeNode > filter,
			final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers )
	{
		this( executor,
			  Optional.of( new AlphanumericComparator(Collator.getInstance())),
			  filter,
			  groupParsers,
			  metadataParsers );
	}
	
	@SuppressWarnings("rawtypes")
	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Predicate< N5TreeNode > filter,
			final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers )
	{
		this( n5,
				executor,
			  Optional.of( new AlphanumericComparator(Collator.getInstance())),
			  filter,
			  groupParsers,
			  metadataParsers );
	}

	@SuppressWarnings("rawtypes")
	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
            final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
		this( executor, comparator, null, groupParsers, metadataParsers);
    }
	
	@SuppressWarnings("rawtypes")
	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
            final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
		this( n5, executor, comparator, null, groupParsers, metadataParsers);
    }
	
	/**
     * Creates an N5 discoverer.
     *
     * If the optional parameter {@code comparator} is specified, the groups and datasets
     * will be listed in the order determined by this comparator.
	 * 
	 * @param executor the executor
	 * @param comparator optional string comparator 
	 * @param filter the dataset filter
	 * @param groupParsers group parsers
	 * @param metadataParsers metadata parsers
	 */
    @SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer(
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final Predicate<N5TreeNode> filter,
			final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
		this.executor = executor;
		this.comparator = comparator.orElseGet( null );
		this.filter = filter;
		this.groupParsers = groupParsers;
		this.metadataParsers = metadataParsers;
    }

	/**
     * Creates an N5 discoverer.
     *
     * If the optional parameter {@code comparator} is specified, the groups and datasets
     * will be listed in the order determined by this comparator.
	 * 
	 * @param executor the executor
	 * @param comparator optional string comparator 
	 * @param filter the dataset filter
	 * @param groupParsers group parsers
	 * @param metadataParsers metadata parsers
	 */
    @SuppressWarnings( "rawtypes" )
	public N5DatasetDiscoverer(
			final N5Reader n5,
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
			final Predicate<N5TreeNode> filter,
			final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
    	this.n5 = n5;
		this.executor = executor;
		this.comparator = comparator.orElseGet( null );
		this.filter = filter;
		this.groupParsers = groupParsers;
		this.metadataParsers = metadataParsers;
    }

    /**
     * Recursively discovers and parses metadata for datasets that are children 
     * of the given base path using {@link N5Reader#deepList}. Returns an {@link N5TreeNode}
     * that can be displayed as a JTree.
     * 
     * @param base the base path
     * @return the n5 tree node
     * @throws IOException the io exception
     */
	public N5TreeNode discoverAndParseRecursive( final String base ) throws IOException
    {
		groupSeparator = n5.getGroupSeparator();

		String[] datasetPaths;
		N5TreeNode root = null ;
		try
		{
			datasetPaths = n5.deepList( base, executor );
			root = N5TreeNode.fromFlatList( base, datasetPaths, groupSeparator );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}

		parseMetadataRecursive( root );
		parseGroupsRecursive( root );
		sortAndTrimRecursive( root );

		return root;
    }

	/**
	 * Returns the name of the dataset, removing the full path
	 * and leading groupSeparator.
	 * 
	 * @param fullPath
	 * @return dataset name
	 */
	private String normalDatasetName( final String fullPath )
	{
		return fullPath.replaceAll( "(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "" );
	}

	public N5TreeNode parse( final String dataset ) throws IOException
    {
		final N5TreeNode node = new N5TreeNode( dataset );
		return parse( node );
    }

	public N5TreeNode parse( final N5TreeNode node ) throws IOException
	{
		 // Go through all parsers to populate metadata
		for ( final N5MetadataParser< ? > parser : metadataParsers )
        {
			N5Metadata parsedMeta = null;
			try
			{
				parsedMeta = parser.parseMetadata( n5, node.getPath() );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}

			if ( parsedMeta != null )
			{
				node.setMetadata( parsedMeta );
				break;
			}
        }
		return node;
	}

	public void parseGroupsRecursive( final N5TreeNode node )
	{
		if ( groupParsers == null )
			return;

		// the group parser is responsible for
		// checking whether the node's metadata exist or not, 
		// and may more may not  run

		// this is not a dataset but may be a group (e.g. multiscale pyramid)
		// try to parse groups
		for ( final N5GroupParser< ? > gp : groupParsers )
		{
			final N5Metadata groupMeta = gp.parseMetadataGroup( node );
			if ( groupMeta != null )
				node.setMetadata( groupMeta );
		}

		for ( final N5TreeNode c : node.childrenList() )
			parseGroupsRecursive( c );
	}

	public void sortAndTrimRecursive( final N5TreeNode node )
	{
		trim( node );
		if ( comparator != null )
			sort( node, comparator );

		for ( final N5TreeNode c : node.childrenList() )
			sortAndTrimRecursive( c );
	}

	public void filterRecursive( final N5TreeNode node )
	{
		if( filter == null )
			return;

		if( !filter.test( node ))
			node.setMetadata( null );

		for ( final N5TreeNode c : node.childrenList() )
			filterRecursive( c );
	}

	public static void parseMetadata(final N5Reader n5, final N5TreeNode node,
			final N5MetadataParser< ? >[] metadataParsers )
	{
		parseMetadata( n5, node, metadataParsers, null );
	}

	// TODO needs testing
//	public static void parseMetadataRecursiveNew(final N5Reader n5, final N5TreeNode node,
//			final N5MetadataParser< ? >[] metadataParsers )
//	{
//		node.flatStream().forEach( n -> parseMetadata( n5, n, metadataParsers ));
//	}

	public static void parseMetadata(final N5Reader n5, final N5TreeNode node,
			final N5MetadataParser< ? >[] metadataParsers,
			final N5GroupParser< ? >[] groupParsers )
	{
        // Go through all parsers to populate metadata
		for ( final N5MetadataParser< ? > parser : metadataParsers )
        {
        	try
        	{
				final N5Metadata parsedMeta = parser.parseMetadata( n5, node.getPath() );

				if ( parsedMeta != null )
				{
					node.setMetadata( parsedMeta );
					break;
				}
			}
			catch ( final Exception e ) {
				e.printStackTrace();
			}
        }

        if( node.getMetadata() == null && groupParsers != null )
		{
			// this is not a dataset but may be a group (e.g. multiscale pyramid)
			// try to parse groups
			for ( final N5GroupParser< ? > gp : groupParsers )
			{
				final N5Metadata groupMeta = gp.parseMetadataGroup( node );
				if ( groupMeta != null )
				{
					node.setMetadata( groupMeta );
					break;
				}
			}
		}
	}

	public void parseMetadataRecursive( final N5TreeNode node ) throws IOException
	{
		parseMetadataRecursive( n5, node, metadataParsers );
	}

	public static void parseMetadataRecursive(final N5Reader n5, final N5TreeNode node,
			final N5MetadataParser<?>[] metadataParsers ) throws IOException
	{
		parseMetadata( n5, node, metadataParsers, null );

        // Recursively parse metadata for children nodes
		for ( final N5TreeNode childNode : node.childrenList() )
			parseMetadataRecursive( n5, childNode, metadataParsers );
    }

	public static void parseGroupsRecursive(
			final N5TreeNode node,
			final N5GroupParser<?>[] groupParsers )
	{
		// TODO make parallel

		if ( groupParsers == null )
			return;

		if ( node.getMetadata() == null )
		{
			// this is not a dataset but may be a group (e.g. multiscale pyramid)
			// try to parse groups
			for ( final N5GroupParser< ? > gp : groupParsers )
			{
				final N5Metadata groupMeta = gp.parseMetadataGroup( node );
				if ( groupMeta != null )
					node.setMetadata( groupMeta );
			}
		}

		for ( final N5TreeNode c : node.childrenList() )
			parseGroupsRecursive( c, groupParsers );
	}

    /**
     * Removes branches of the N5 container tree that do not contain any nodes that can be opened
     * (nodes with metadata).
     *
     * @param node the node
     * @return
     *      {@code true} if the branch contains a node that can be opened, {@code false} otherwise
     */
    private static boolean trim(final N5TreeNode node)
    {
		final List< N5TreeNode > children = node.childrenList();
		if ( children.isEmpty() )
		{
			return node.getMetadata() != null;
//			return node.isDataset();
		}

        boolean ret = false;
		for (final Iterator<N5TreeNode> it = children.iterator(); it.hasNext();)
		{
			final N5TreeNode childNode = it.next();
			if ( !trim( childNode ))
			{
				it.remove();
			}
			else
				ret = true;
		}

        return ret || node.getMetadata() != null;
    }

    private static void sort(final N5TreeNode node, final Comparator<? super String> comparator)
    {
		final List< N5TreeNode > children = node.childrenList();
		children.sort( Comparator.comparing( N5TreeNode::toString, comparator ) );

		for ( final N5TreeNode childNode : children )
			sort( childNode, comparator );
    }
}
