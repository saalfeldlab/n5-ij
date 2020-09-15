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

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface N5GsonMetadataParser < T extends N5Metadata > extends N5MetadataParser< T >
{

	/**
	 * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
	 * while discovering the N5 tree and filling the metadata for datasets or
	 * groups.
	 *
	 * The metadata parsing is done in the bottom-up fashion, so the children of
	 * the given {@code node} have already been processed and should already
	 * contain valid metadata (if any).
	 *
	 * @param map
	 * @return
	 * @throws Exception
	 */
	public default < R extends AbstractGsonReader > T parseMetadataGson( final R parser, final String dataset, final HashMap< String, JsonElement > map ) throws Exception
	{
		HashMap< String, Object > objMap = new HashMap< String, Object >();
		HashMap< String, Class< ? > > typeMap = keysToTypes();
		objMap.put( "dataset", dataset );
		for( String k : typeMap.keySet() )
		{
			objMap.put( k , parser.getGson().fromJson( map.get( k ), typeMap.get( k )));
		}
		return parseMetadata( objMap );
	}

	public default < R extends AbstractGsonReader > T parseMetadataGson( final R parser, final String dataset ) throws Exception
	{
		return parseMetadataGson( parser, dataset, parser.getAttributes( dataset ));
	}

	public default < R extends AbstractGsonReader > Map< String, Object > parseMetadataGson( 
			final R n5, final String dataset, final Map< String, Class<?> > keys )
	{
		HashMap< String, JsonElement > map;
		try
		{
			map = n5.getAttributes( dataset );
		}
		catch ( IOException e1 )
		{ 
			// empty map, or could be null?
			return new HashMap<>();
		}

		HashMap< String, Object > outmap = new HashMap<>();
		for( String k : keys.keySet() )
			outmap.put( k, map.get( k ));

		return outmap;
	}

	public default Map< String, Object > parseMetadata( 
			final N5Reader n5, final String dataset, final Map< String, Class<?> > keys )
	{
		HashMap< String, Object > map = new HashMap<>();
		for( String k : keys.keySet() )
		{
			if( !map.containsKey( k ))
				return null;

			try
			{
				map.put( k, n5.getAttribute( dataset, k, keys.get( k ) ) );
			}
			catch ( IOException e )
			{
				return null;
			}
		}

		return map;
	}

	@Override
	public default T parseMetadata( N5Reader n5, String dataset ) throws Exception
	{
		if( n5 instanceof AbstractGsonReader )
		{
			return parseMetadataGson( (AbstractGsonReader) n5, dataset );
		}
		else
		{
			Map< String, Object > keys = N5MetadataParser.parseMetadataStatic( n5, dataset, keysToTypes() );
			return parseMetadata( keys );
		}
	}

	@Override
	public default T parseMetadata( N5Reader n5, N5TreeNode... nodes ) throws Exception
	{
		if( n5 instanceof AbstractGsonReader )
		{
			return parseMetadataGson( (AbstractGsonReader) n5, nodes[0].path );
		}
		else
		{
			Map< String, Object > keys = N5MetadataParser.parseMetadataStatic( n5, nodes[0].path, keysToTypes() );
			return parseMetadata( keys );
		}
	}

}
