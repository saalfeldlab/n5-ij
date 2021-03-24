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
package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

public interface N5MetadataParser < T extends N5Metadata > //R extends AbstractGsonReader & N5Reader >
{

	/**
	 * Returns a map of keys to class types needed for parsing.
	 *
	 * Optional in general, but used by default implementations.
	 * @return the map
	 */
	public HashMap<String,Class<?>> keysToTypes();

	public default boolean check( final Map< String, Object > map ) throws Exception
	{
		for( final String k : keysToTypes().keySet() )
		{
			if ( !map.containsKey( k ) )
				return false;
			else if( map.get( k ) == null )
				return false;
		}
		return true;
	}

	public T parseMetadata( final Map< String, Object > map ) throws Exception;

	/**
	 * 
     * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
     * while discovering the N5 tree and filling the metadata for datasets or groups.
     *
     * The metadata parsing is done in the bottom-up fashion, so the children of the given {@code node}
     * have already been processed and should already contain valid metadata (if any).
     * 
	 * @param n5 the reader
	 * @param nodes list of tree nodes
	 * @return the metadata
	 * @throws Exception parsing exception
	 */
	public default T parseMetadata( final N5Reader n5, final N5TreeNode... nodes ) throws Exception
	{
		return parseMetadata( n5, nodes[ 0 ].getPath() );
	}

	public default T parseMetadata( final N5Reader n5, final String dataset ) throws Exception
	{
		final Map< String, Object > keys = N5MetadataParser.parseMetadataStatic( n5, dataset, keysToTypes() );
		return parseMetadata( keys );
	}

	public static DatasetAttributes parseAttributes( final Map< String, Object > map  )
	{
		final int[] blockSize = ( int[] ) map.get( "blockSize" );
		final long[] dimensions = ( long[] ) map.get( "dimensions" );
		final DataType dataType = DataType.fromString( ( String ) map.get( "dataType" ) );

		if( dimensions != null && dataType != null && dataType != null )
			return new DatasetAttributes( dimensions, blockSize, dataType, null );

		return null;
	}

	public static Map< String, Object > parseMetadataStatic(
			final N5Reader n5, final String dataset, final Map< String, Class<?> > keys )
	{
		final HashMap< String, Object > map = new HashMap<>();
		map.put( "dataset", dataset ); // TODO doc this

		for( final String k : keys.keySet() )
		{
			try
			{
				map.put( k, n5.getAttribute( dataset, k, keys.get( k ) ) );
			}
			catch ( final IOException e )
			{
				return null;
			}
		}

		try
		{
			final DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
			map.put( "dimensions", attrs.getDimensions() );
			map.put( "blockSize", attrs.getBlockSize() );
			map.put( "dataType", attrs.getDataType().toString() );
		}
		catch ( final IOException e ) { }

		return map;
	}

	public static boolean hasRequiredKeys(
			final Map<String,Class<?>> keysToTypes,
			final Map<String,?> metaMap )
	{
		for( final String k : keysToTypes.keySet() )
		{
			if( !metaMap.containsKey( k ))
				return false;
		}
		return true;
	}

}
