package org.janelia.saalfeldlab.n5.metadata.translation;

import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataAdapter;
import org.janelia.saalfeldlab.n5.metadata.canonical.ImportedTranslations;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransformAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

public class JqUtils {

	public static String resolveImports(String query) {
		if (query.startsWith("include")) {
			return new ImportedTranslations().getTranslation() + query.replaceFirst("^\\s*include\\s+\"n5\"\\s*;", "");
		}
		else
			return query;
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

	public static Gson buildGson( final N5Reader n5 ) {
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter( n5 ));
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.disableHtmlEscaping();
		return gsonBuilder.create();
	}

	public String transform( final String in, final String translation, final ObjectMapper objMapper, final Scope scope ) throws JsonMappingException, JsonProcessingException 
	{
		JsonNode inJsonNode = objMapper.readTree( in );

		final List< JsonNode > out = new ArrayList<>();
		JsonQuery.compile( translation, Versions.JQ_1_6 ).apply( scope, inJsonNode, out::add );	

		final StringBuffer stringOutput = new StringBuffer();
		for ( final JsonNode n : out )
			stringOutput.append( n.toString() + "\n" );

		return stringOutput.toString();
	}

}
