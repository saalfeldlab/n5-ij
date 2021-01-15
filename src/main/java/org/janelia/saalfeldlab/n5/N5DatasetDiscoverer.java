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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5GsonMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

import com.google.gson.JsonElement;

import se.sawano.java.text.AlphanumericComparator;

public class N5DatasetDiscoverer {

    @SuppressWarnings( "rawtypes" )
	private final N5MetadataParser[] metadataParsers;

	private final N5GroupParser<?>[] groupParsers;

    private final Comparator<? super String> comparator;

    private final Predicate< N5TreeNode > filter;

    private final ExecutorService executor;

    private N5TreeNode root;

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
			final ExecutorService executor,
			final Optional<Comparator<? super String>> comparator,
            final N5GroupParser[] groupParsers,
			final N5MetadataParser... metadataParsers)
    {
		this( executor, comparator, null, groupParsers, metadataParsers);
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

	public N5TreeNode discoverRecursive( final N5Reader n5, final String base ) throws IOException
    {
		root = new N5TreeNode( base, n5.datasetExists( base ));
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

	private LinkedBlockingQueue< Future< N5TreeNode > > parseJobFutures;

	public LinkedBlockingQueue< Future< N5TreeNode > > discoverThreads( final N5Reader n5, final N5TreeNode node ) throws IOException
	{
		parseJobFutures = new LinkedBlockingQueue<>();
		discoverThreadsHelper( n5, node );
		return parseJobFutures;
	}

	private N5TreeNode discoverThreadsHelper( final N5Reader n5, final N5TreeNode node ) throws IOException
	{
		parseJobFutures.add( executor.submit( new Callable< N5TreeNode >()
		{
			@Override
			public N5TreeNode call() throws Exception
			{
				parseMetadata( n5, node, metadataParsers, null );
				if ( node.isDataset() )
				{
					return node;
				}
				else
				{
					final String[] children = n5.list( node.path );
					for ( final String childGroup : children )
					{
						// add the node
						final String childPath = Paths.get( node.path, childGroup ).toString();
						final N5TreeNode childNode = new N5TreeNode( childPath, false );
						node.add( childNode );

						// parse recursively
						parseJobFutures.add( executor.submit( new Callable< N5TreeNode >()
						{
							@Override
							public N5TreeNode call() throws Exception
							{
								return discoverThreadsHelper( n5, childNode );
							}
						}));

					}
					return node;
				}
			}
		}));
		return node;
	}

	public void sortAndTrimRecursive( final N5TreeNode node )
	{
		trim( node );
		if ( comparator != null )
			sort( node, comparator );

		for ( final N5TreeNode c : node.childrenList() )
			sortAndTrimRecursive( c );
	}

	public void parseGroupsRecursive( final N5TreeNode node )
	{
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
			parseGroupsRecursive( c );
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
		HashMap< String, JsonElement > jsonMap = null;
		if ( n5 instanceof AbstractGsonReader )
		{
			jsonMap = ( ( AbstractGsonReader ) n5 ).getAttributes( node.path );
			if( jsonMap == null )
			{
				node.setIsDataset( false );
				return;
			}
		}

        // Go through all parsers to populate metadata
		for ( final N5MetadataParser< ? > parser : metadataParsers )
        {
        	try
        	{
				N5Metadata parsedMeta;
				if ( jsonMap != null && parser instanceof N5GsonMetadataParser )
				{
					parsedMeta = ( ( N5GsonMetadataParser< ? > ) parser ).parseMetadataGson( node.path, jsonMap );
				}
				else
					parsedMeta = parser.parseMetadata( n5, node );

				if ( parsedMeta != null )
				{
					node.setMetadata( parsedMeta );
					node.setIsDataset( true );

					break;
				}
			}
			catch ( final Exception e ) {}
        }

        if( node.getMetadata() == null && groupParsers != null )
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
     * @param node the node
     * @return
     *      {@code true} if the branch contains a node that can be opened, {@code false} otherwise
     */
    private static boolean trim(final N5TreeNode node)
    {
		final List< N5TreeNode > children = node.childrenList();
		if ( children.isEmpty() )
		{
			//return node.getMetadata() != null;
			return node.isDataset();
		}

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
		final List< N5TreeNode > children = node.childrenList();
		children.sort( Comparator.comparing( N5TreeNode::toString, comparator ) );

		// add back the children in order
		// necessary because the collection of children can't be sorted in place
		node.removeAllChildren();
		children.stream().forEach( x -> node.add( x ) );

		for ( final N5TreeNode childNode : children )
			sort( childNode, comparator );
    }
}
