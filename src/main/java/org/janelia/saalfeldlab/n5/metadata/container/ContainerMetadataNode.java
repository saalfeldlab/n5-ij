package org.janelia.saalfeldlab.n5.metadata.container;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

/**
 * 
 * @author John Bogovic
 *
 */
public class ContainerMetadataNode {
	
	private final HashMap<String, JsonElement> attributes;
	private final Map<String, ContainerMetadataNode > children;
	//private final List<ContainerMetadataNode> children;

	public ContainerMetadataNode( final HashMap<String, JsonElement> attributes,
		final Map<String, ContainerMetadataNode > children )
//		final List<ContainerMetadataNode> children )
	{
		this.attributes = attributes;
		this.children = children;
	}
	
	
	public HashMap<String, JsonElement> getAttributes()
	{
		return attributes;
	}
	
	public Map<String, ContainerMetadataNode > getChildren()
	{
		return children;
	}
	
	public Optional<ContainerMetadataNode> getChild( final String relativePath, final String groupSeparator )
	{
		if( relativePath.isEmpty() )
			return Optional.of(this);

		final String normPath = relativePath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final int i = normPath.indexOf(groupSeparator);

		final String cpath; 
		final String relToChild;
		if( i < 0 )
		{
			cpath = relativePath;
			relToChild = "";
		}
		else
		{
			final String[] pathSplit = normPath.split( groupSeparator );
			final String[] relToChildList = new String[ pathSplit.length - 1 ]; 

			cpath = pathSplit[ 0 ];
			System.arraycopy(pathSplit, 1, relToChildList, 0, relToChildList.length);
			relToChild = Arrays.stream( relToChildList ).collect( Collectors.joining("/"));
		}


		final ContainerMetadataNode c = children.get(cpath);
		if( c == null )
			return Optional.empty();
		else 
			return c.getChild(relToChild, groupSeparator);

	}
	
}
