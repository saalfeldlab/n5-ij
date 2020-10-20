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

import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

/**
 * Abstract class for single-scale or multi-scale N5 metadata.
 */
public abstract class AbstractN5Metadata implements N5Metadata
{
	private DatasetAttributes attributes;
	
	private String path;
	
	public AbstractN5Metadata( final String path, final DatasetAttributes attributes )
	{
		this.path = path;
		this.attributes = attributes;
	}

	public AbstractN5Metadata( final String path )
	{
		this( path, null );
	}

	public AbstractN5Metadata( final DatasetAttributes attributes )
	{
		this( "", attributes );
	}

	@Override
    public String getPath()
    {
    	return path;
    }

	@Override
	public DatasetAttributes getAttributes()
	{
		return attributes;
	}

	public static Map< String, Class<?> > datasetAtttributeKeys()
	{
		Map< String, Class<?>> defaultMap =  new HashMap< String, Class<?>>();
		addDatasetAttributeKeys( defaultMap );
		return defaultMap;
	}
	
	public static void addDatasetAttributeKeys( final Map< String, Class<?>> keysToTypes )
	{
		keysToTypes.put( "dimensions", long[].class );
		keysToTypes.put( "blockSize", int[].class );
		keysToTypes.put( "dataType", String.class );
	}

}
