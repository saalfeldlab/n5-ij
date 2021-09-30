package org.janelia.saalfeldlab.n5.metadata.canonical;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;

/**
 * A parser for the "canonical" metadata dialect.
 * 
 * @author John Bogovic
 */
public class CanonicalMetadataParser implements N5MetadataParser<CanonicalMetadata> {

	protected Gson gson;

	protected ContainerMetadataNode root;

	protected Predicate<CanonicalMetadata> filter;

	public CanonicalMetadataParser() {
		this(x -> true);
	}

	public CanonicalMetadataParser( final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;

	}

	public static Gson buildGson( final N5Reader n5 ) {
		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(SpatialTransform.class, new SpatialTransformAdapter( n5 ));
//		gsonBuilder.registerTypeAdapter(LinearSpatialTransform.class, new SpatialTransformAdapter( n5 ));
		gsonBuilder.registerTypeAdapter(CanonicalMetadata.class, new CanonicalMetadata.CanonicalMetadataAdapter());
		gsonBuilder.registerTypeAdapter(DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeHierarchyAdapter(Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.disableHtmlEscaping();
		return gsonBuilder.create();
	}

	public void setFilter(final Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

	public CanonicalMetadataParser( final N5Reader n5, final String n5Tree, final String translation) {

		final GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter( SpatialTransform.class, new SpatialTransformAdapter( n5 ) );
		gsonBuilder.registerTypeAdapter( LinearSpatialTransform.class, new SpatialTransformAdapter( n5 ) );
		gsonBuilder.registerTypeAdapter( CanonicalMetadata.class, new CanonicalMetadata.CanonicalMetadataAdapter() );
		gsonBuilder.registerTypeAdapter( DataType.class, new DataType.JsonAdapter());
		gsonBuilder.registerTypeHierarchyAdapter( Compression.class, CompressionAdapter.getJsonAdapter());
		gsonBuilder.disableHtmlEscaping();
		gson = gsonBuilder.create();

		root = gson.fromJson( n5Tree, ContainerMetadataNode.class );
	}

	public Gson getGson() {
		return gson;
	}

	public void setGson( Gson gson ) {
		this.gson = gson;
	}
	
	protected void setup( final N5Reader n5 ) {
		// TODO rebuilding gson and root is the safest thing to do, but possibly inefficient

//		if( gson == null )
			setGson( buildGson( n5 ));

//		if( root == null ) { 
			root = ContainerMetadataNode.build(n5, gson);
			root.addPathsRecursive();	
//		}
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {
		setup( n5 );
		return parseMetadata( node, n5.getGroupSeparator());
//		if( root == null ) {
//			return Optional.empty();
//		}
//
//		return root.getChild( node.getPath(), n5.getGroupSeparator() )
//			.map( ContainerMetadataNode::getAttributes )
//			.map( this::canonicalMetadata )
//			.filter(filter);
	}

	@Override
	public Optional<CanonicalMetadata> parseMetadata(final N5Reader n5, final String dataset) {
		return parseMetadata( n5, new N5TreeNode( dataset ));
	}

	public Optional<CanonicalMetadata> parseMetadata(final String dataset, final String groupSep ) {
		return parseMetadata( new N5TreeNode( dataset ), groupSep );
	}

	public Optional<CanonicalMetadata> parseMetadata(N5TreeNode node, String groupSep) {
		if (root == null)
			return Optional.empty();

		return root.getChild(node.getPath(), groupSep)
				.map(ContainerMetadataNode::getAttributes)
				.map(this::canonicalMetadata)
				.filter(filter);
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
