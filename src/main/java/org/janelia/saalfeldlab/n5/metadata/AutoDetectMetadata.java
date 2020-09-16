package org.janelia.saalfeldlab.n5.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AutoDetectMetadata implements N5GsonMetadataParser< N5Metadata >
{
	private N5MetadataParser<?>[] parsers;
	private HashMap<String,Class<?>> keysToTypes;

	public AutoDetectMetadata( N5MetadataParser<?>[] parsers )
	{
		this.parsers = parsers;
		keysToTypes = new HashMap<>();
	}

	public AutoDetectMetadata()
	{
		this( new N5MetadataParser[]
		{
			new N5ImagePlusMetadata( "" ),
			new N5CosemMetadata(),
			new N5ViewerSingleMetadataParser( ),
			new DefaultMetadata( "", -1 )
		});
	}

	@Override
	public HashMap<String,Class<?>> keysToTypes()
	{
		return keysToTypes;
	}

	@Override
	public N5Metadata parseMetadata( Map< String, Object > metaMap ) throws Exception
	{
		ArrayList<Exception> elist = new ArrayList<>();
		for( N5MetadataParser< ? > p : parsers )
		{
			try 
			{
				N5Metadata meta = p.parseMetadata( metaMap );
				if( meta != null )
					return meta;
			}
			catch( Exception e )
			{
				elist.add( e );
			}
		}

		for( Exception e : elist )
		{
			e.printStackTrace();
		}
		return null;
	}

}
