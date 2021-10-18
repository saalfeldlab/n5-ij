package org.janelia.saalfeldlab.n5.metadata.translation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.google.gson.JsonElement;


public class TranslatedTreeMetadataWriter extends TreeTranslation{

	final N5Writer n5;

//	private ContainerMetadataNode root;

	public TranslatedTreeMetadataWriter(
			final N5Writer n5,
			final String dataset,
			final String translation) {
		super(	ContainerMetadataNode.build(n5, dataset, JqUtils.buildGson(n5) ), 
				JqUtils.buildGson(n5), 
				translation );

		this.n5 = n5;
//		this.root = super.getOrig();

//		translationFun = new ContainerTranslation( translation, gson);
//		root = ContainerMetadataNode.build(n5, gson);
//		treeTranslation = new TreeTranslation(root, gson, translation);
//		translated = translationFun.apply( root );
	}
	
	public TranslatedTreeMetadataWriter(
			final N5Writer n5,
			final String translation) {

		this( n5, "", translation );
	}

	public void writeAllTranslatedAttributes() throws IOException {

		Iterator<String> it = getTranslated().getChildPathsRecursive("").iterator();
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
	 * Writes all attributes stored in the node corresponding to the given pathName.
	 * 
	 * @param pathName
	 * @param node
	 * @throws IOException
	 */
	public void writeAllTranslatedAttributes(
			final String pathName ) throws IOException {

//		System.out.println( pathName );

		Optional<ContainerMetadataNode> nopt = getTranslated().getNode(pathName);
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

		Optional<ContainerMetadataNode> nopt = getTranslated().getChild(pathName, "/");
		if( !nopt.isPresent())
			return;

		HashMap<String, JsonElement> attrs = nopt.get().getAttributes();
		if( attrs.containsKey(key))
			n5.setAttribute(pathName, key, attrs.get(key));
	}

}
