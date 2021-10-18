package org.janelia.saalfeldlab.n5.metadata.container;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;

import com.google.gson.JsonElement;


public class ContainerMetadataWriter {

	private N5Writer n5;

	private ContainerMetadataNode metadataTree;

	public ContainerMetadataWriter(
			final N5Writer n5,
			final String dataset) {

		this( n5, ContainerMetadataNode.build(n5, dataset, JqUtils.buildGson(n5)) );
	}
	
	public ContainerMetadataWriter(
			final N5Writer n5) {
		this( n5, "" );
	}
	
	public ContainerMetadataWriter(
			final N5Writer n5,
			final ContainerMetadataNode metadataTree) {

		this.n5 = n5;
		this.metadataTree = metadataTree;
	}
	
	public ContainerMetadataNode getMetadataTree() {
		return metadataTree;
	}

	public void setMetadataTree( ContainerMetadataNode metadataTree ) {
		this.metadataTree = metadataTree;
	}
	
	public void setN5Writer( N5Writer n5 ) {
		this.n5 = n5;
	}

	public void writeAllAttributes() throws IOException {

		Iterator<String> it = metadataTree.getChildPathsRecursive("").iterator();
		while( it.hasNext() )
			writeAllAttributes( it.next() );
	}
	
	/**
	 * Writes all attributes stored in the tree 
	 * 
	 * @param pathName
	 * @param node
	 * @throws IOException
	 */
	public void writeAllAttributes(
			final String pathName ) throws IOException {

		if( !n5.exists(pathName))
			n5.createGroup(pathName);

		Optional<ContainerMetadataNode> nopt = metadataTree.getChild(pathName, "/");
		if( !nopt.isPresent())
			return;

		String path = nopt.get().getPath();
		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
		for( String key : attrs.keySet())
		if( attrs.containsKey(key))
			n5.setAttribute(path, key, attrs.get(key));
	}

	public void writeAttribute(
			final String pathName,
			final String key) throws IOException {

		Optional<ContainerMetadataNode> nopt = metadataTree.getChild(pathName, "/");
		if( !nopt.isPresent())
			return;

		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
		if( attrs.containsKey(key))
			n5.setAttribute(pathName, key, attrs.get(key));
	}

}
