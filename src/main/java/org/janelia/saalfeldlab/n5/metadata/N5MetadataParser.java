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
package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

import com.google.gson.JsonElement;

public interface N5MetadataParser < T extends N5Metadata > //R extends AbstractGsonReader & N5Reader >
{

	/**
	 * Returns a map of keys to class types needed for parsing. 
	 * 
	 * Optional in general, but used by default implementations.
	 * @return
	 */
	public HashMap<String,Class<?>> keysToTypes();

	public default boolean check( final Map< String, Object > map ) throws Exception
	{
		for( String k : keysToTypes().keySet() )
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
     * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
     * while discovering the N5 tree and filling the metadata for datasets or groups.
     *
     * The metadata parsing is done in the bottom-up fashion, so the children of the given {@code node}
     * have already been processed and should already contain valid metadata (if any).
     *
     * @param n5
     * @param node
     * @return
     * @throws Exception
     */
	public default T parseMetadata( final N5Reader n5, final N5TreeNode... nodes ) throws Exception
	{
		return parseMetadata( n5, nodes[ 0 ].path );
	}

	public default T parseMetadata( final N5Reader n5, final String dataset ) throws Exception
	{
		Map< String, Object > keys = N5MetadataParser.parseMetadataStatic( n5, dataset, keysToTypes() );
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
		HashMap< String, Object > map = new HashMap<>();
		map.put( "dataset", dataset ); // TODO doc this

		for( String k : keys.keySet() )
		{
			try
			{
				map.put( k, n5.getAttribute( dataset, k, keys.get( k ) ) );
			}
			catch ( IOException e )
			{ 
				return null;
			}
		}

		try
		{
			DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
			map.put( "dimensions", attrs.getDimensions() );
			map.put( "blockSize", attrs.getBlockSize() );
			map.put( "dataType", attrs.getDataType().toString() );
		}
		catch ( IOException e ) { }

		return map;
	}

	public static boolean hasRequiredKeys(
			final Map<String,Class<?>> keysToTypes,
			final Map<String,?> metaMap )
	{
		for( String k : keysToTypes.keySet() )
		{
			if( !metaMap.containsKey( k ))
				return false;
		}
		return true;
	}

}
