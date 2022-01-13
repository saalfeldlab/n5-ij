package org.janelia.saalfeldlab.n5.ui;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.saalfeldlab.n5.N5TreeNode;

/**
 * Wraps an {@link N5TreeNode} as a swing {@link DefaultMutableTreeNode}.
 * 
 * @author John Bogovic
 */
public class N5TreeNodeWrapper extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 2650578684960249546L;

	private final N5TreeNode node;

	public N5TreeNodeWrapper(N5TreeNode node) {

		super(node.getPath());
		this.node = node;
		refresh();
	}

	public N5TreeNode getNode() {

		return node;
	}

	public void refresh() {
		removeAllChildren();
		for (N5TreeNode c : node.childrenList()) {
			add(new N5TreeNodeWrapper(c));
		}
	}

}
