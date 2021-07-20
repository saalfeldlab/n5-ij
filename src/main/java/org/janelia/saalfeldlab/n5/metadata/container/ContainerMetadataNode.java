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

import com.google.gson.JsonElement;

/**
 * 
 * @author John Bogovic
 *
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

	public static ContainerMetadataNode build(final N5Reader n5) {
		if (n5 instanceof AbstractGsonReader) {
			try {
				return buildGson((AbstractGsonReader) n5);
			} catch (Exception e) {
			}
			return null;
		} else
			return null;
	}

	public static <T extends AbstractGsonReader> ContainerMetadataNode buildGson(final T n5)
			throws InterruptedException, ExecutionException {
		String[] datasets;
		N5TreeNode root;
		try {

			datasets = n5.deepList("", Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList("", datasets, "/");
			return buildHelper(n5, root);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ContainerMetadataNode buildHelper(final AbstractGsonReader n5, N5TreeNode baseNode) {
		final Optional<HashMap<String, JsonElement>> attrs = N5TreeNode.getMetadataMapJson(n5, baseNode.getPath());
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode> childMap = new HashMap<>();
		for (N5TreeNode child : children)
			childMap.put(child.getNodeName(), buildHelper(n5, child));

		if (attrs.isPresent())
			return new ContainerMetadataNode(attrs.get(), childMap);
		else
			return new ContainerMetadataNode(new HashMap<>(), childMap);
	}

}
