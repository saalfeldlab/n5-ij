package org.janelia.saalfeldlab.n5.metadata.imagej;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;

public class MetadataTemplateMapper implements N5MetadataWriter<ImagePlusMetadataTemplate> {

	public ImagePlusMetadataTemplate template;

	private final Scope scope;

	private final String query;

	private Gson gson;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ObjectMapper objMapper;

	public MetadataTemplateMapper(final Scope scope, final String query) {

		this.scope = scope;
		this.query = query;
		gson = new GsonBuilder().create();
		objMapper = MAPPER;
	}

	public MetadataTemplateMapper( final String query )
	{
		this( buildRootScope(), query );
	}
	
	public void setObjectMapper( final ObjectMapper objMapper )
	{
		this.objMapper = objMapper;
	}
	
	public JsonQuery getQuery() throws JsonQueryException
	{
		return JsonQuery.compile( query, Versions.JQ_1_6 );
	}
	
	public Scope getScope()
	{
		return scope;
	}

	public String mapToJson( final ImagePlusMetadataTemplate metadata ) throws IOException
	{
		return map( gson.toJson( metadata ));
	}

	public List<JsonNode> map( final JsonNode in ) throws JsonQueryException
	{
		final List< JsonNode > out = new ArrayList<>();
		getQuery().apply( scope, in, out::add );	
		return out;
	}

	public String map( final String input ) throws IOException
	{
		final List<JsonNode> out = map( objMapper.readTree( input ));
		final StringBuffer stringOutput = new StringBuffer();
		for ( final JsonNode node : out )
			stringOutput.append( node.toString() + "\n" );

		return stringOutput.toString();
	}

	public Object computeToMap( final String json ) throws IOException
	{
		return gson.fromJson( map( json ), Object.class );
	}

	public JsonElement computeToJson( final String input ) throws IOException
	{
		return new JsonParser().parse( map( input ));
	}

	public static Scope buildRootScope()
	{
		// First of all, you have to prepare a Scope which s a container of built-in/user-defined functions and variables.
		final Scope rootScope = Scope.newEmptyScope();

		// Use BuiltinFunctionLoader to load built-in functions from the classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {
			@Override
			public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output, final Version version) throws JsonQueryException {
				args.get(0).apply(scope, in, (time) -> {
					output.emit(new TextNode(Strings.repeat(in.asText(), time.asInt())), null);
				});
			}
		});
		return rootScope;
	}

	public static final String RESOLUTION_ONLY_MAPPER = "{\n\"resolution\" : [.xResolution, .yResolution, .zResolution ]\n}";

	public static final String COSEM_MAPPER = "{\n\t\"transform\":\n" +
			"\t{\n" +
			"\t\"scale\": [.zResolution, .yResolution, .xResolution],\n" +
			"\t\"translate\": [.zOrigin, .yOrigin, .xOrigin],\n" +
			"\t\"axes\": [\"z\", \"y\", \"x\"],\n" +
			"\t\"units\": [.globalUnit, .globalUnit, .globalUnit]\n" +
			"\t}\n" +
			"}";

	@Override
	public void writeMetadata(final ImagePlusMetadataTemplate t, final N5Writer n5, final String group) throws Exception {

		final Map<String, ?> map = (Map<String, ?>)computeToMap(gson.toJson(t));
		for (String key : map.keySet())
			n5.setAttribute(group, key, map.get(key));
	}

  	public String toJsonString( final ImagePlusMetadataTemplate t ) throws Exception
  	{
  		return computeToJson( gson.toJson( t )).toString();
  	}

}
