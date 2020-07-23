package org.janelia.saalfeldlab.n5.metadata;

import java.util.ArrayList;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

public class AutoDetectMetadata implements N5MetadataParser< N5Metadata >
{
	private N5MetadataParser<?>[] parsers;

	public AutoDetectMetadata( N5MetadataParser<?>[] parsers )
	{
		this.parsers = parsers;
	}

	public AutoDetectMetadata()
	{
		this( new N5MetadataParser[]
		{
			new N5ImagePlusMetadata( "" ),
			new N5CosemMetadata( "", null, null ),
			new N5ViewerMetadataParser( false ),
			new DefaultMetadata( "", 1 )
		});
	}

	@Override
	public N5Metadata parseMetadata( N5Reader n5, N5TreeNode node ) throws Exception
	{
		ArrayList<Exception> elist = new ArrayList<>();
		for( N5MetadataParser< ? > p : parsers )
		{
			try 
			{
				N5Metadata meta = p.parseMetadata( n5, node );
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
			System.err.println( "" );
			e.printStackTrace();
		}
		return null;
	}

}
