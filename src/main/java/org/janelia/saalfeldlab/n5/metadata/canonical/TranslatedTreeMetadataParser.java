package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.janelia.saalfeldlab.n5.Bzip2Compression;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.CompressionAdapter;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.container.ContainerMetadataNode;
import org.janelia.saalfeldlab.n5.metadata.transforms.LinearSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransformAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

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
public class TranslatedTreeMetadataParser implements N5MetadataParser<CanonicalMetadata> {

	private final String translation;

	private final Scope scope;

	private final Gson gson;

	private final ObjectMapper objMapper;

	private final ContainerMetadataNode root;

	private ContainerMetadataNode translatedRoot;

	private Predicate<CanonicalMetadata> filter;

	public TranslatedTreeMetadataParser(final N5Reader n5, final String translation) {
		this(n5, translation, x -> true);
	}

	public TranslatedTreeMetadataParser(final N5Reader n5, final String translation,
			final Predicate<CanonicalMetadata> filter) {

		this.translation = resolveImports( translation );
		this.filter = filter;

		scope = buildRootScope();
		objMapper = new ObjectMapper();
		gson = buildGson();

		root = ContainerMetadataNode.build(n5, gson);
		try {
			translatedRoot = gson.fromJson( transform(gson.toJson(root)), ContainerMetadataNode.class);
			addPaths();
		} catch (Exception e) {
			e.printStackTrace();
			translatedRoot = null;
		}
	}

	public static Gson buildGson() {
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter());
		gsonBuilder.registerTypeAdapter(LinearSpatialTransform.class, new SpatialTransformAdapter());
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadata.CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.disableHtmlEscaping();
		return gsonBuilder.create();
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

	public void setFilter(final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	public TranslatedTreeMetadataParser(final String n5Tree, final String translation) {

		this.translation = resolveImports( translation );

		scope = buildRootScope();
		objMapper = new ObjectMapper();	

		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( SpatialTransform.class, new SpatialTransformAdapter() );
		gsonBuilder.registerTypeAdapter( LinearSpatialTransform.class, new SpatialTransformAdapter() );
		gsonBuilder.registerTypeAdapter( CanonicalMetadata.class, new CanonicalMetadata.CanonicalMetadataAdapter() );
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.disableHtmlEscaping();
		gson = gsonBuilder.create();

		root = gson.fromJson( n5Tree, ContainerMetadataNode.class );
		try {
			translatedRoot = gson.fromJson( transform(gson.toJson(root)), ContainerMetadataNode.class );
			addPaths();
		} catch (Exception e) {
			e.printStackTrace();
			translatedRoot = null;
		}
	}

	private void addPaths() throws Exception {
		JsonNode inJsonNode = objMapper.readTree(gson.toJson(translatedRoot));

		final List<JsonNode> out = new ArrayList<>();
		JsonQuery.compile( FinalTranslations.PATHFUNS + " addPaths", Versions.JQ_1_6).apply(scope, inJsonNode, out::add);

		final StringBuffer stringOutput = new StringBuffer();
		for (final JsonNode n : out)
			stringOutput.append(n.toString() + "\n");

		translatedRoot = gson.fromJson(stringOutput.toString(), ContainerMetadataNode.class);
	}

	public Gson getGson()
	{
		return gson;
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
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		if( translatedRoot == null ) {
			return Optional.empty();
		}

		return translatedRoot.getChild( node.getPath(), n5.getGroupSeparator() )
			.map( ContainerMetadataNode::getAttributes )
			.map( this::canonicalMetadata )
			.filter(filter);
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(final N5Reader n5, final String dataset) {
		return parseMetadata( new N5TreeNode( dataset ), n5.getGroupSeparator() );
	}

	public Optional<CanonicalMetadata> parseMetadata(final String dataset, final String groupSep ) {
		return parseMetadata( new N5TreeNode( dataset ), groupSep );
	}

	public Optional<CanonicalMetadata> parseMetadata(N5TreeNode node, String groupSep) {

		if (translatedRoot == null)
			return Optional.empty();

		return translatedRoot.getChild(node.getPath(), groupSep).map(ContainerMetadataNode::getAttributes)
				.map(this::canonicalMetadata);
	}

	public CanonicalMetadata canonicalMetadata(final HashMap<String, JsonElement> attrMap) {
		return gson.fromJson(gson.toJson(attrMap), CanonicalMetadata.class);
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
	
	public static Optional<DatasetAttributes> datasetAttributes(final Gson gson,
			HashMap<String, JsonElement> attributeMap) {

		try {

			final long[] dimensions = GsonAttributesParser.parseAttribute(attributeMap, "dimensions", long[].class,
					gson);
			if (dimensions == null)
				return Optional.empty();

			final DataType dataType = GsonAttributesParser.parseAttribute(attributeMap, "dataType", DataType.class,
					gson);
			if (dataType == null)
				return Optional.empty();

			int[] blockSize = GsonAttributesParser.parseAttribute(attributeMap, "blockSize", int[].class, gson);
			if (blockSize == null)
				blockSize = Arrays.stream(dimensions).mapToInt(a -> (int) a).toArray();

			Compression compression = GsonAttributesParser.parseAttribute(attributeMap, "compression",
					Compression.class, gson);

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

		} catch (Exception e) {
		}

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
				final String compressionString = context.deserialize( elem.getAsJsonObject().get("compression"), String.class );
				switch ( compressionString ) {
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
