package org.janelia.saalfeldlab.n5.metadata.container;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * @author John Bogovic
 */
public class ContainerMetadataNode {

	private final HashMap<String, JsonElement> attributes;
	private final Map<String, ContainerMetadataNode> children;

	public ContainerMetadataNode(final HashMap<String, JsonElement> attributes,
			final Map<String, ContainerMetadataNode> children) {
		this.attributes = attributes;
		this.children = children;
	}

	public HashMap<String, JsonElement> getAttributes() {
		return attributes;
	}

	public Map<String, ContainerMetadataNode> getChildren() {
		return children;
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

	public static ContainerMetadataNode build(final N5Reader n5, final Gson gson ) {
		if (n5 instanceof AbstractGsonReader) {
			try {
				return buildGson((AbstractGsonReader) n5, gson );
			} catch (Exception e) {
			}
			return null;
		} else {
			try {
				return buildH5(n5, gson);
			} catch (Exception e) {
			}
		}

		return null;
	}

	public static <T extends AbstractGsonReader> ContainerMetadataNode buildGson(final T n5, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {

			datasets = n5.deepList("", Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList("", datasets, "/");
			return buildHelper(n5, root );

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelper(final AbstractGsonReader n5, N5TreeNode baseNode ) {

		HashMap<String, JsonElement> attrs = null;
		try {
			attrs = n5.getAttributes(baseNode.getPath());
		} catch (IOException e) {
		}

		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelper(n5, child));

		if ( attrs != null )
			return new ContainerMetadataNode(attrs, childMap);
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap);
	}

	public static <T extends N5Reader> ContainerMetadataNode buildH5(final T n5, final Gson gson )
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {

			datasets = n5.deepList("", Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList("", datasets, "/");
			return buildHelperH5(n5, root, gson );

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelperH5(final N5Reader n5, N5TreeNode baseNode, Gson gson ) {
		final Optional<HashMap<String, JsonElement>> attrs = getMetadataMapH5(n5, baseNode.getPath(), gson );
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelperH5(n5, child, gson));

		if (attrs.isPresent())
			return new ContainerMetadataNode(attrs.get(), childMap);
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap);
	}

	public static Optional<HashMap<String, JsonElement>> getMetadataMapH5(final N5Reader n5, final String dataset,
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
