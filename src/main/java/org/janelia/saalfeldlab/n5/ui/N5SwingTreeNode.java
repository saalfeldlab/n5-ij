package org.janelia.saalfeldlab.n5.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.tree.TreeNode;

import org.janelia.saalfeldlab.n5.N5TreeNode;

public class N5SwingTreeNode extends N5TreeNode implements TreeNode {

	private N5SwingTreeNode parent;

	public N5SwingTreeNode( final String path ) {
		super( path );
	}

	public N5SwingTreeNode( final String path, final N5SwingTreeNode parent ) {
		super( path );
		this.parent = parent;
	}

	@Override
	public Enumeration children() {
		return Collections.enumeration(childrenList());
	}

	public void add(final N5SwingTreeNode child) {

		childrenList().add(child);
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

}
