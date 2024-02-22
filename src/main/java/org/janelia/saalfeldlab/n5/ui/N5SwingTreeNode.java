package org.janelia.saalfeldlab.n5.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.janelia.saalfeldlab.n5.universe.N5TreeNode;

public class N5SwingTreeNode extends N5TreeNode implements MutableTreeNode {

	private N5SwingTreeNode parent;

	private DefaultTreeModel treeModel;

	public N5SwingTreeNode( final String path ) {
		super( path );
	}

	public N5SwingTreeNode( final String path, final DefaultTreeModel model ) {
		super( path );
		this.treeModel = model;
	}

	public N5SwingTreeNode( final String path, final N5SwingTreeNode parent ) {
		super( path );
		this.parent = parent;
	}

	public N5SwingTreeNode( final String path, final N5SwingTreeNode parent, final DefaultTreeModel model ) {
		super( path );
		this.parent = parent;
		this.treeModel = model;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration children() {
		return Collections.enumeration(childrenList());
	}

	public void add(final N5SwingTreeNode child) {

		childrenList().add(child);
	}

	@Override
	public N5SwingTreeNode addPath(final String path) {

		String normPath = removeLeadingSlash(path);
		if (!getPath().isEmpty() && !normPath.startsWith(getPath()))
			return null;

		if (getPath().equals(normPath))
			return this;

		final String relativePath = removeLeadingSlash(normPath.replaceAll("^" + getPath(), ""));

		final int sepIdx = relativePath.indexOf("/");
		final String childName;
		if (sepIdx < 0)
			childName = relativePath;
		else
			childName = relativePath.substring(0, sepIdx);

		// get the appropriate child along the path if it exists, otherwise add
		// it
		N5TreeNode child = null;
		Stream<N5TreeNode> cs = childrenList().stream().filter(n -> n.getNodeName().equals(childName));;
		Optional<N5TreeNode> copt = cs.findFirst();
		if (copt.isPresent())
			child = copt.get();
		else {
			child = new N5SwingTreeNode(
					getPath().isEmpty() ? childName : getPath() + "/" + childName,
					this, treeModel);

			add(child);

			if (treeModel != null)
				treeModel.nodesWereInserted(this, new int[]{childrenList().size() - 1});
		}
		return (N5SwingTreeNode)child.addPath(normPath);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public N5SwingTreeNode getChildAt(int i) {
		return (N5SwingTreeNode) childrenList().get(i);
	}

	@Override
	public int getChildCount() {
		return childrenList().size();
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public int getIndex(TreeNode n) {
		return childrenList().indexOf(n);
	}

	@Override
	public TreeNode getParent() {
		return parent;
	}

	@Override
	public boolean isLeaf() {
		return getChildCount() < 1;
	}

	public static void fromFlatList(final N5SwingTreeNode root, final String[] pathList, final String groupSeparator) {

		final HashMap<String, N5SwingTreeNode> pathToNode = new HashMap<>();

		final String normalizedBase = normalDatasetName(root.getPath(), groupSeparator);
		pathToNode.put(normalizedBase, root);

		// sort the paths by length such that parent nodes always have smaller
		// indexes than their children
		Arrays.sort(pathList);

		final String prefix = normalizedBase == groupSeparator ? "" : normalizedBase;
		for (final String datasetPath : pathList) {

			final String fullPath = prefix + groupSeparator + datasetPath;
			final String parentPath = fullPath.substring(0, fullPath.lastIndexOf(groupSeparator));

			N5SwingTreeNode parent = pathToNode.get(parentPath);
			if (parent == null) {
				// possible for the parent to not appear in the list
				// if deepList is called with a filter
				parent = new N5SwingTreeNode(parentPath);
				pathToNode.put(parentPath, parent);
			}
			final N5SwingTreeNode node = new N5SwingTreeNode(fullPath, parent );
			pathToNode.put(fullPath, node);

			parent.add(node);
		}
	}

	private static String normalDatasetName(final String fullPath, final String groupSeparator) {

		return fullPath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
	}

	@Override
	public void insert(MutableTreeNode child, int index) {
		if( child instanceof N5SwingTreeNode )
			childrenList().add(index, (N5SwingTreeNode)child);
	}

	@Override
	public void remove(int index) {
		childrenList().remove(index);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public void remove(MutableTreeNode node) {
		childrenList().remove(node);
	}

	@Override
	public void removeFromParent() {
		parent.childrenList().remove(this);
	}

	@Override
	public void setParent(MutableTreeNode newParent) {
		if( newParent instanceof N5SwingTreeNode )
			this.parent = (N5SwingTreeNode)newParent;
	}

	@Override
	public void setUserObject(Object object) {
		// does nothing
	}

}
