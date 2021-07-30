package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import net.imglib2.realtransform.AffineTransform3D;
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
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author John Bogovic
 */
public abstract class AbstractMetadataTemplateParser<T extends N5Metadata> implements N5MetadataParser<T> {
	
	private final String translation;
	
	private final Scope scope;
	
	private final Gson gson;

	private final ObjectMapper objMapper;

	public AbstractMetadataTemplateParser( final Gson gson, final String translation )
	{
		this.translation = translation;
		this.gson = gson;

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

	@Override
	public Optional<T> parseMetadata(N5Reader n5, N5TreeNode node) {

		HashMap<String, Object> attrs = N5TreeNode.getMetadataMap(n5, node.getPath());
		if( attrs.isEmpty() )
		{
			// TODO should i display this warning?
			System.err.println("could not parse attributes");
			return Optional.empty();
		}

		JsonNode in;
		try {
			in = objMapper.readTree( gson.toJson( attrs ));

			final List< JsonNode > out = new ArrayList<>();
			JsonQuery.compile( translation, Versions.JQ_1_6 ).apply( scope, in, out::add );	
			
			final StringBuffer stringOutput = new StringBuffer();
			for ( final JsonNode n : out )
				stringOutput.append( n.toString() + "\n" );

			final Type mapType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
			final HashMap<String, JsonElement> tmpmap = gson.fromJson(stringOutput.toString(), mapType);
			final HashMap<String, JsonElement> map = tmpmap == null ? new HashMap<>() : tmpmap;
			if( !map.containsKey("path" ))
				map.put("path", new JsonPrimitive( node.getPath() ));

			return parseFromMap( gson, map );

//			JsonElement elem = gson.fromJson(stringOutput.toString(), JsonElement.class );
//			JsonObject obj = elem.getAsJsonObject();
//			
//			if( !obj.has("path"))
//				obj.addProperty("path", node.getPath());
//
//			return parse( gson, elem );

		} catch ( Exception e) {
		}

		return Optional.empty();
	}

	public <R extends AbstractGsonReader> Optional<T> parseMetadataTree( final R n5, N5TreeNode node) {

		ContainerMetadataNode treeNode;
		try {
			treeNode = ContainerMetadataNode.build(n5, n5.getGson());
		} catch (Exception e) {
			return Optional.empty();
		}

		JsonNode in;
		try {
			in = objMapper.readTree( gson.toJson( treeNode ));

			final List< JsonNode > out = new ArrayList<>();
			JsonQuery.compile( translation, Versions.JQ_1_6 ).apply( scope, in, out::add );	
			
			final StringBuffer stringOutput = new StringBuffer();
			for ( final JsonNode n : out )
				stringOutput.append( n.toString() + "\n" );

			final Type mapType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
			final HashMap<String, JsonElement> tmpmap = gson.fromJson(stringOutput.toString(), mapType);
			final HashMap<String, JsonElement> map = tmpmap == null ? new HashMap<>() : tmpmap;
			
			if( !map.containsKey("path" ))
				map.put("path", new JsonPrimitive( node.getPath() ));

			return parseFromMap( gson, map );

//			JsonElement elem = gson.fromJson(stringOutput.toString(), JsonElement.class );
//			JsonObject obj = elem.getAsJsonObject();
//			
//			if( !obj.has("path"))
//				obj.addProperty("path", node.getPath());
//
//			return parse( gson, elem );

		} catch ( Exception e) {
		}

		return Optional.empty();
	}	

	public abstract Optional<T> parseFromMap( final Gson gson, HashMap<String, JsonElement> attributeMap );

//	public abstract Optional<T> parse( final Gson gson, JsonElement elem );

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
	
	public static Optional<DatasetAttributes> datasetAttributes(final Gson gson, HashMap<String, JsonElement> attributeMap) {

		try {

			final long[] dimensions = GsonAttributesParser.parseAttribute(attributeMap, "dimensions", long[].class, gson);
			if (dimensions == null)
				return Optional.empty();

			final DataType dataType = GsonAttributesParser.parseAttribute(attributeMap, "dataType", DataType.class, gson);
			if (dataType == null)
				return Optional.empty();

			int[] blockSize = GsonAttributesParser.parseAttribute(attributeMap, "blockSize", int[].class, gson);
			if (blockSize == null)
				blockSize = Arrays.stream(dimensions).mapToInt(a -> (int)a).toArray();

			Compression compression = GsonAttributesParser.parseAttribute(attributeMap, "compression", Compression.class, gson);

			/* version 0 */
			if (compression == null) {
				switch (GsonAttributesParser.parseAttribute(attributeMap, "compression", String.class, gson)) {
				case "raw":
					compression = new RawCompression();
					break;
				case "gzip":
					compression = new GzipCompression();
					break;
				case "bzip2":
					compression = new Bzip2Compression();
					break;
				case "lz4":
					compression = new Lz4Compression();
					break;
				case "xz":
					compression = new XzCompression();
					break;
				}
			}

			return Optional.of(new DatasetAttributes(dimensions, blockSize, dataType, compression));

		} catch (Exception e) {}

		return Optional.empty();
	}

	public static Optional<DatasetAttributes> datasetAttributes(final JsonDeserializationContext context, JsonElement elem ) {

		try {

			final long[] dimensions = context.deserialize( elem.getAsJsonObject().get("dimensions"), long[].class);
			if (dimensions == null)
				return Optional.empty();

			final DataType dataType = context.deserialize( elem.getAsJsonObject().get("dataType"), DataType.class);
			if (dataType == null)
				return Optional.empty();

			int[] blockSize = context.deserialize( elem.getAsJsonObject().get("blockSize"), int[].class );
			if (blockSize == null)
				blockSize = Arrays.stream(dimensions).mapToInt(a -> (int)a).toArray();

			Compression compression = context.deserialize( elem.getAsJsonObject().get("compression"), Compression.class );

			/* version 0 */
			if (compression == null) {
				switch ( compression.getType() ) {
				case "raw":
					compression = new RawCompression();
					break;
				case "gzip":
					compression = new GzipCompression();
					break;
				case "bzip2":
					compression = new Bzip2Compression();
					break;
				case "lz4":
					compression = new Lz4Compression();
					break;
				case "xz":
					compression = new XzCompression();
					break;
				}
			}

			return Optional.of(new DatasetAttributes(dimensions, blockSize, dataType, compression));

		} catch (Exception e) {}

		return Optional.empty();
	}
	
}
