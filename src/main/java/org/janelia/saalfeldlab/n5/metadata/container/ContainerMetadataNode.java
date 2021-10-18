package org.janelia.saalfeldlab.n5.metadata.container;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.metadata.translation.JqUtils;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * @author John Bogovic
 */
public class ContainerMetadataNode implements N5Writer {

	protected HashMap<String, JsonElement> attributes;
	protected Map<String, ContainerMetadataNode> children;
	protected final transient Gson gson;

	public ContainerMetadataNode() {
		gson = JqUtils.buildGson(null);
		attributes = new HashMap<String, JsonElement>();
		children = new HashMap<String, ContainerMetadataNode >();
		addPathsRecursive();
	}

	public ContainerMetadataNode(final HashMap<String, JsonElement> attributes,
			final Map<String, ContainerMetadataNode> children, final Gson gson) {
		this.attributes = attributes;
		this.children = children;
		this.gson = gson;
	}

	public ContainerMetadataNode( ContainerMetadataNode other) {
		gson = other.gson;
		attributes = other.attributes;
		children = other.children;
	}

	public HashMap<String, JsonElement> getAttributes() {
		return attributes;
	}

	public Map<String, ContainerMetadataNode> getChildren() {
		return children;
	}

	public Stream<ContainerMetadataNode> getChildrenStream() {
		return children.entrySet().stream().map( e -> e.getValue() );
	}

	public Stream<ContainerMetadataNode> flatten() {
		return Stream.concat(Stream.of(this), getChildrenStream().flatMap( ContainerMetadataNode::flatten ));
	}

	public void addChild(String relativepath, ContainerMetadataNode child) {
		children.put(relativepath, child);
	}

	/**
	 * 
	 * @param path full or relative path
	 * @return
	 */
	public Stream<ContainerMetadataNode> pathToChild(final String path) {
		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");

		final String thisNodePath = getPath();
		String relativePath = normPath;
		if( !thisNodePath.isEmpty() && normPath.startsWith( thisNodePath )) {
			relativePath = normPath.replaceFirst( thisNodePath, "");
		}

		return Stream.concat(Stream.of(this), children.get(relativePath).pathToChild(relativePath));
	}

	/**
	 * @return the path to this node from the root.
	 */
	public String getPath() {
		if( attributes.containsKey("path"))
			return attributes.get("path").getAsString();
		else
			return "";
	}

	public Stream<String> getChildPathsRecursive( String thisPath ) {
		return Streams.concat( Stream.of( thisPath ),
			this.children.keySet().stream().flatMap( k -> 
				this.children.get(k).getChildPathsRecursive( thisPath + "/" + k )));
	}

	/**
	 * Adds path attributes to this node and recursively to its children.
	 */
	public void addPathsRecursive() {
		addPathsRecursive("");
	}

	/**
	 * Adds path attributes to this node and recursively to its children.
	 * 
	 * @param thisPath
	 */
	public void addPathsRecursive( String thisPath ) {
		attributes.put("path", new JsonPrimitive( thisPath ));
		for ( String childPath : children.keySet() )
			children.get(childPath).addPathsRecursive( thisPath + "/" + childPath );
	}

	public Optional<ContainerMetadataNode> getNode( final String path ) {

		final String groupSeparator = "/";
		final String normPath = path.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final String thisNodePath = getPath();

		if( normPath.startsWith( thisNodePath )) {
			return getChild( normPath.replaceFirst( thisNodePath, ""));
		}

		return Optional.empty();
	}

	public Optional<ContainerMetadataNode> getChild(final String relativePath ) {
		return getChild(relativePath, "/");
	}

	public ContainerMetadataNode childRelative(final String normRelativePath) {
		String childName = normRelativePath.substring( 0, normRelativePath.indexOf('/'));
		if( children.containsKey(childName) )
			return children.get(childName);
		else
			return null;
	}

	public Optional<ContainerMetadataNode> getChild(final String relativePath, final String groupSeparator) {
		if (relativePath.isEmpty())
			return Optional.of(this);

		final String normPath = relativePath.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final int i = normPath.indexOf(groupSeparator);

		final String cpath;
		final String relToChild;
		if (i < 0) {
			cpath = normPath;
			relToChild = "";
		} else {
			final String[] pathSplit = normPath.split(groupSeparator);
			final String[] relToChildList = new String[pathSplit.length - 1];

			cpath = pathSplit[0];
			System.arraycopy(pathSplit, 1, relToChildList, 0, relToChildList.length);
			relToChild = Arrays.stream(relToChildList).collect(Collectors.joining("/"));
		}

		final ContainerMetadataNode c = children.get(cpath);
		if (c == null)
			return Optional.empty();
		else
			return c.getChild(relToChild, groupSeparator);
	}

	@Override
	public <T> T getAttribute( String path, String key, Class<T> clazz ) {
		return getNode( path ).map( x -> gson.fromJson( x.getAttributes().get(key), clazz))
			.orElseGet( () -> null );
	}

//	@Override
//	public <T> T getAttribute(String pathName, String key, Class<T> clazz) {
//
//		Optional<ContainerMetadataNode> nodeOpt = getNode(pathName);
//		if( nodeOpt.isPresent() ) { 
//			return gson.fromJson( nodeOpt.get().getAttributes().get(key), clazz );
//		}
//		return null;
//	}

	@Override
	public <T> T getAttribute(String pathName, String key, Type type) throws IOException {
		Optional<ContainerMetadataNode> nodeOpt = getNode(pathName);
		if( nodeOpt.isPresent() ) { 
			return gson.fromJson( nodeOpt.get().getAttributes().get(key), type );
		}
		return null;
	}

	@Override
	public DatasetAttributes getDatasetAttributes(String pathName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(String pathName) {
		return getNode( pathName ).isPresent();
	}

	@Override
	public String[] list(String pathName) throws IOException {
		return getNode(pathName).map( ContainerMetadataNode::getChildren )
			.map( m -> m.keySet().toArray( new String[1]))
			.orElse(new String[] {} );
	}

	@Override
	public Map<String, Class<?>> listAttributes(String pathName) throws IOException {
		return null;
	}

	@Override
	public <T> void setAttribute( final String pathName, final String key, final T attribute) {
		setAttributes(pathName, Collections.singletonMap(key, attribute));
	}

	@Override
	public void setAttributes(String pathName, Map<String, ?> attributes) {
		final Type mapType = new TypeToken<HashMap<String, JsonElement>>(){}.getType();
		JsonElement json = gson.toJsonTree(attributes);
		HashMap<String, JsonElement> map = gson.fromJson(json, mapType);
		getNode( pathName ).ifPresent( x -> x.attributes.putAll(map) );
	}

	@Override
	public void createGroup(String pathName) {
		final String groupSeparator = "/";
		final String normPath = pathName.replaceAll("^(" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
		final String thisNodePath = getPath();

		final String relativePath;
		if( normPath.startsWith( thisNodePath ))
			relativePath = normPath.replaceFirst( thisNodePath, "" );
		else
			relativePath = normPath;

		String[] parts = relativePath.split(groupSeparator);
		createGroupHelper( this, parts, 0 );
	}

	private static void createGroupHelper( ContainerMetadataNode node, String[] parts, int i )
	{
		if( i >= parts.length )
			return;

		String childRelpath = parts[i];
		ContainerMetadataNode child;
		if( !node.children.containsKey( childRelpath )) {
			child = new ContainerMetadataNode();
			node.addChild(childRelpath, child);
		}
		else {
			child = node.children.get(childRelpath);
		}

		createGroupHelper( child, parts, i+1 )	;
	}

	@Override
	public boolean remove(String pathName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> void writeBlock(String pathName, DatasetAttributes datasetAttributes, DataBlock<T> dataBlock)
			throws IOException {
	}

	@Override
	public boolean deleteBlock(String pathName, long... gridPosition) throws IOException {
		return false;
	}

	@Override
	public DataBlock<?> readBlock(String pathName, DatasetAttributes datasetAttributes, long... gridPosition)
			throws IOException {
		return null;
	}



	@SuppressWarnings("unchecked")
	public static  <N extends GsonAttributesParser & N5Reader > ContainerMetadataNode build(
			final N5Reader n5, final String dataset, final Gson gson ) {
		if (n5 instanceof GsonAttributesParser) {
			try {
				return buildGson((N)n5, dataset, gson );
			} catch (Exception e) {
			}
		}
		else {
			try {
				return buildN5( n5, dataset, gson );
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static ContainerMetadataNode build(final N5Reader n5, final Gson gson ) {
		return build( n5, "", gson );
	}

	public static <N extends GsonAttributesParser & N5Reader > ContainerMetadataNode buildGson(
			final N n5, final String dataset, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {
			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
			final ContainerMetadataNode containerRoot = buildHelper(n5, root );
			containerRoot.addPathsRecursive(dataset);
			return containerRoot;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <N extends GsonAttributesParser & N5Reader> ContainerMetadataNode buildHelper(final N n5, N5TreeNode baseNode ) {

		HashMap<String, JsonElement> attrs = null;
		try {
			attrs = n5.getAttributes(baseNode.getPath());
		} catch (IOException e) { }

		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelper(n5, child));

		if ( attrs != null )
			return new ContainerMetadataNode(attrs, childMap, n5.getGson());
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap, n5.getGson());
	}

	public static <T extends N5Reader> ContainerMetadataNode buildN5(final T n5, final String dataset, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {

			datasets = n5.deepList(dataset, Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList(dataset, datasets, "/");
			final ContainerMetadataNode containerRoot = buildHelperN5(n5, root, gson );
			containerRoot.addPathsRecursive(dataset);
			return containerRoot;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelperN5(final N5Reader n5, N5TreeNode baseNode, Gson gson ) {
		final Optional<HashMap<String, JsonElement>> attrs = getMetadataMapN5(n5, baseNode.getPath(), gson );
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelperN5(n5, child, gson));

		if (attrs.isPresent())
			return new ContainerMetadataNode(attrs.get(), childMap, gson );
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap, gson);
	}

	public static Optional<HashMap<String, JsonElement>> getMetadataMapN5(final N5Reader n5, final String dataset,
			final Gson gson) {
		try {
			final HashMap<String, JsonElement> attrs = new HashMap<>();
			Map<String, Class<?>> attrClasses = n5.listAttributes(dataset);
			for (String k : attrClasses.keySet()) {

				if( attrClasses.get(k).equals(String.class)) {

					String s = n5.getAttribute(dataset, k, String.class );
					Optional<JsonObject> elem = stringToJson( s, gson );
					if( elem.isPresent())
						attrs.put( k, elem.get());
					else
						attrs.put( k, gson.toJsonTree( s ));
				}
				else
					attrs.put(k, gson.toJsonTree(n5.getAttribute(dataset, k, attrClasses.get(k))));
			}

			if (attrs != null)
				return Optional.of(attrs);

		} catch (Exception e) {
		}
		return Optional.empty();
	}

	public static Optional<JsonObject> stringToJson(String s, final Gson gson) {

		try {
			JsonObject elem = gson.fromJson(s, JsonObject.class);
			return Optional.of(elem);
		} catch (JsonSyntaxException e) {
			return Optional.empty();
		}
	}

}
