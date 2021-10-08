package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;
import org.janelia.saalfeldlab.n5.metadata.translation.JqFunction;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


public class TranslatedTreeMetadataWriter {

	private JqFunction<ContainerMetadataNode,ContainerMetadataNode> translationFun;
	
	final N5Writer n5;

	private ContainerMetadataNode root;

	private ContainerMetadataNode translated;

	public TranslatedTreeMetadataWriter(
			final N5Writer n5,
			final String translation) {
		
		this.n5 = n5;
		Gson gson = JqUtils.buildGson(n5);
		translationFun = new JqFunction<>( translation,
				gson,
				ContainerMetadataNode.class);

		root = ContainerMetadataNode.build(n5, gson);
		root.addPathsRecursive();
		translated = translationFun.apply( root );
	}
	
	public void writeAllTranslatedAttributes() throws IOException {

		Iterator<String> it = translated.getChildPathsRecursive("").iterator();
		while( it.hasNext() )
			writeAllTranslatedAttributes( it.next() );

//		Optional<ContainerMetadataNode> nopt = translated.getChild(pathName, "/");
//		if( !nopt.isPresent())
//			return;
//
//		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
//		for( String key : attrs.keySet())
//		if( attrs.containsKey(key))
//			n5.setAttribute(pathName, key, attrs.get(key));
	}
	
	/**
	 * Writes all attributes stored in the tree 
	 * 
	 * @param pathName
	 * @param node
	 * @throws IOException
	 */
	public void writeAllTranslatedAttributes(
			final String pathName ) throws IOException {

//		System.out.println( pathName );

		Optional<ContainerMetadataNode> nopt = translated.getChild(pathName, "/");
		if( !nopt.isPresent())
			return;

		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
		for( String key : attrs.keySet())
		if( attrs.containsKey(key))
			n5.setAttribute(pathName, key, attrs.get(key));
	}

	public void writeTranslatedAttribute(
			final String pathName,
			final String key) throws IOException {

		Optional<ContainerMetadataNode> nopt = translated.getChild(pathName, "/");
		if( !nopt.isPresent())
			return;

		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
		if( attrs.containsKey(key))
			n5.setAttribute(pathName, key, attrs.get(key));
	}

}
