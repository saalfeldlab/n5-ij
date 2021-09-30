package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;

/**
 * A parser for metadata that are translated into the "canonical" dialect.
 * 
 * @author John Bogovic
 */
public class TranslatedTreeMetadataParser extends CanonicalMetadataParser {

	private final String translation;

	private final Scope scope;

	private final ObjectMapper objMapper;

	private ContainerMetadataNode translatedRoot;

	public TranslatedTreeMetadataParser(final String translation) {
		this(translation, x -> true);
	}

	public TranslatedTreeMetadataParser(final String translation,
			final Predicate<CanonicalMetadata> filter) {
		super( filter );
		this.translation = resolveImports( translation );

		scope = buildRootScope();
		objMapper = new ObjectMapper();
	}

	public static String resolveImports(String query) {
		if (query.startsWith("include"))
		{
			return new ImportedTranslations().getTranslation() + query.replaceFirst("^\\s*include\\s+\"n5\"\\s*;", "");
//			return FinalTranslations.IMPORTFUNS + query.replaceFirst("^\\s*include\\s+\"n5\"\\s*;", "");
		}
		else
			return query;
	}

	public TranslatedTreeMetadataParser( final N5Reader n5, final String n5Tree, final String translation) {

		this.translation = resolveImports( translation );

		scope = buildRootScope();
		objMapper = new ObjectMapper();	
	}

	public String transform( final String in ) throws JsonMappingException, JsonProcessingException
	{
		JsonNode inJsonNode = objMapper.readTree( in );

		final List< JsonNode > out = new ArrayList<>();
		JsonQuery.compile( translation, Versions.JQ_1_6 ).apply( scope, inJsonNode, out::add );	

		final StringBuffer stringOutput = new StringBuffer();
		for ( final JsonNode n : out )
			stringOutput.append( n.toString() + "\n" );

		return stringOutput.toString();
	}

	protected void setup( final N5Reader n5 ) {
//		if( gson == null )
			setGson( buildGson( n5 ));

//		if( root == null ) { 
			root = ContainerMetadataNode.build(n5, gson);
			root.addPathsRecursive();	
			
			try {
				translatedRoot = gson.fromJson( transform(gson.toJson(root)), ContainerMetadataNode.class);
				translatedRoot.addPathsRecursive();
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
//		}
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

	public static boolean testTranslation( String translationRaw )
	{
		String translation = resolveImports( translationRaw );
		try {
			JsonQuery.compile( translation, Versions.JQ_1_6 );
		} catch (JsonQueryException e) {
			return false;
		}
		return true;
	}

	public static Scope buildRootScope() {
		// First of all, you have to prepare a Scope which s a container of
		// built-in/user-defined functions and variables.
		final Scope rootScope = Scope.newEmptyScope();

		// Use BuiltinFunctionLoader to load built-in functions from the classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {
			@Override
			public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path,
					final PathOutput output, final Version version) throws JsonQueryException {
				args.get(0).apply(scope, in, (time) -> {
					output.emit(new TextNode(Strings.repeat(in.asText(), time.asInt())), null);
				});
			}
		});
		return rootScope;
	}

}
