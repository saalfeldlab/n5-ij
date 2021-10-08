package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;
import org.janelia.saalfeldlab.n5.metadata.translation.ImportedTranslations;
import org.janelia.saalfeldlab.n5.metadata.translation.JqFunction;

import com.google.gson.JsonElement;

/**
 * A parser for metadata that are translated into the "canonical" dialect.
 * 
 * @author John Bogovic
 */
public class TranslatedTreeMetadataParser extends CanonicalMetadataParser {

	private ContainerMetadataNode translatedRoot;

	private JqFunction<ContainerMetadataNode,ContainerMetadataNode> translationFun;

	public TranslatedTreeMetadataParser(final String translation) {
		this(translation, x -> true);
	}

	public TranslatedTreeMetadataParser(final String translation,
			final Predicate<CanonicalMetadata> filter) {
		super( filter );
		translationFun = new JqFunction<>( translation,
				JqUtils.buildGson(null),
				ContainerMetadataNode.class);
	}

	public boolean validTranslation() {
		return translationFun != null;
	}

	public static String resolveImports(String query) {
		if (query.startsWith("include"))
			return new ImportedTranslations().getTranslation() + query.replaceFirst("^\\s*include\\s+\"n5\"\\s*;", "");
		else
			return query;
	}

	public TranslatedTreeMetadataParser( final N5Reader n5, final String n5Tree, final String translation) {
		super( null );
		translationFun = new JqFunction<>( translation,
				JqUtils.buildGson(n5),
				ContainerMetadataNode.class);
	}

	protected void setup( final N5Reader n5 ) {
//		if( gson == null )
			setGson( buildGson( n5 ));

		root = ContainerMetadataNode.build(n5, gson);
		root.addPathsRecursive();
		translatedRoot = translationFun.apply( root );
		if( translatedRoot != null )
			translatedRoot.addPathsRecursive();
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {
		setup( n5 );
		return parseMetadata( node, n5.getGroupSeparator());
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(final N5Reader n5, final String dataset) {
		return parseMetadata( n5, new N5TreeNode( dataset ) );
	}

	public Optional<CanonicalMetadata> parseMetadata(final String dataset, final String groupSep ) {
		return parseMetadata( new N5TreeNode( dataset ), groupSep );
	}

	public Optional<CanonicalMetadata> parseMetadata(N5TreeNode node, String groupSep) {

		if (translatedRoot == null)
			return Optional.empty();

		return translatedRoot.getChild( node.getPath(), groupSep )
				.map( ContainerMetadataNode::getAttributes )
				.map( this::canonicalMetadata )
				.filter(filter);
	}

	public CanonicalMetadata canonicalMetadata(final HashMap<String, JsonElement> attrMap) {
		return gson.fromJson(gson.toJson(attrMap), CanonicalMetadata.class);
	}

}
