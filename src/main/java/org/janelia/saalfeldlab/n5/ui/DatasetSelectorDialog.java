/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.ui;

import ij.IJ;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataParser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DatasetSelectorDialog
{
    /**
     * The dataset/group discoverer that takes a list of metadata parsers.
     *
     * Currently, there is only one parser for N5 Viewer-style metadata (that comes from the previous version of this plugin).
     *
     * To add more parsers, add a new class that implements {@link N5MetadataParser}
     * and pass an instance of it to the {@link N5DatasetDiscoverer} constructor here.
     */
    private final N5DatasetDiscoverer datasetDiscoverer;

	private Consumer< DataSelection > okCallback;

    private JFrame dialog;
    private JTextField containerPathTxt;

    private JTree containerTree;
    private JList selectedList;

    private JButton addSourceBtn;
    private JButton removeSourceBtn;

    private JButton okBtn;

    private DefaultTreeModel treeModel;
    private DefaultListModel listModel;

    private String lastBrowsePath;

    private Function<String,N5Reader> n5Fun;
    private N5Reader n5;

    private N5TreeNode selectedNode;

//	private final Predicate< N5TreeNode > filter;
  
    public DatasetSelectorDialog( final Function<String,N5Reader> n5Fun,
//    		final Predicate<N5TreeNode > filter,
    		final N5MetadataParser<?>... parsers )
    {
    	this.n5Fun = n5Fun;
//    	this.filter = filter;
		datasetDiscoverer = new N5DatasetDiscoverer( parsers );
    }

    public DatasetSelectorDialog( final Function<String,N5Reader> n5Fun )
    {
//    	this( n5Fun, x -> x.isDataset, new N5ViewerMetadataParser() );
    	this( n5Fun, new N5ViewerMetadataParser() );
    }

    public DatasetSelectorDialog( final N5Reader n5,
    		//final Predicate<N5TreeNode > filter,
    		final N5MetadataParser<?>... parsers )

    {
    	this.n5 = n5;
//    	this.filter = filter;
		datasetDiscoverer = new N5DatasetDiscoverer( parsers );
    }

    public DatasetSelectorDialog( final N5Reader n5 )
    {
//    	this( n5, x -> x.isDataset, new N5ViewerMetadataParser() );
    	this( n5, new N5ViewerMetadataParser() );
    }


    public void run( final Consumer< DataSelection > okCallback )
    {
        this.okCallback = okCallback;

        dialog = new JFrame("N5 Viewer");
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));
        dialog.setMinimumSize(new Dimension(750, 500));
        dialog.setPreferredSize(dialog.getMinimumSize());

        if( n5 == null )
        {
			final JPanel containerPanel = new JPanel();
			containerPanel.add(new JLabel("N5 container:"));

			containerPathTxt = new JTextField();
			containerPathTxt.setPreferredSize(new Dimension(400, containerPathTxt.getPreferredSize().height));
			containerPanel.add(containerPathTxt);

			final JButton browseBtn = new JButton("Browse...");
			browseBtn.addActionListener(e -> openContainer( n5Fun, this::openBrowseDialog));
			containerPanel.add(browseBtn);

			final JButton linkBtn = new JButton("Open link...");
			linkBtn.addActionListener(e -> openContainer( n5Fun, this::openLinkDialog));
			containerPanel.add(linkBtn);

			dialog.getContentPane().add(containerPanel);
        }
//        else
//        {
//			final JPanel containerPanel = new JPanel();
//			containerPanel.add(new JLabel("N5 container:"));
//
//			containerPathTxt = new JTextField();
//			containerPathTxt.setText( n5.toString()  );
//
//			dialog.getContentPane().add(containerPanel);
//        }

        final JPanel datasetsPanel = new JPanel();

        final JPanel containerTreePanel = new JPanel();
        containerTreePanel.setLayout(new BoxLayout(containerTreePanel, BoxLayout.Y_AXIS));
        final JLabel containerLabel = new JLabel("Available:");
        containerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerTreePanel.add(containerLabel);
        treeModel = new DefaultTreeModel(null);
        containerTree = new JTree(treeModel);
        containerTree.setEnabled(false);
        containerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // By default leaf nodes (datasets) are displayed as files. This changes the default behavior to display them as folders
        final DefaultTreeCellRenderer treeCellRenderer = (DefaultTreeCellRenderer) containerTree.getCellRenderer();
        treeCellRenderer.setLeafIcon(treeCellRenderer.getOpenIcon());

        final JScrollPane containerTreeScroller = new JScrollPane(containerTree);
        containerTreeScroller.setPreferredSize(new Dimension(280, 350));
        containerTreeScroller.setMinimumSize(containerTreeScroller.getPreferredSize());
        containerTreeScroller.setMaximumSize(containerTreeScroller.getPreferredSize());
        containerTreePanel.add(containerTreeScroller);
        datasetsPanel.add(containerTreePanel);

        final JPanel sourceButtonsPanel = new JPanel();
        sourceButtonsPanel.setLayout( new BoxLayout( sourceButtonsPanel, BoxLayout.Y_AXIS ) );
        addSourceBtn = new JButton(">");
        addSourceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        addSourceBtn.setEnabled(false);
        addSourceBtn.addActionListener(e -> addSource());
        sourceButtonsPanel.add(addSourceBtn);

        removeSourceBtn = new JButton("<");
        removeSourceBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeSourceBtn.setEnabled(false);
        removeSourceBtn.addActionListener(e -> removeSource());
        sourceButtonsPanel.add(removeSourceBtn);
        datasetsPanel.add(sourceButtonsPanel);

        final JPanel selectedListPanel = new JPanel();
        selectedListPanel.setLayout(new BoxLayout(selectedListPanel, BoxLayout.Y_AXIS));
        final JLabel selectedLabel = new JLabel("Selected:");
        selectedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectedListPanel.add(selectedLabel);
        listModel = new DefaultListModel();
        selectedList = new JList(listModel);
        selectedList.setEnabled(false);
        final JScrollPane selectedListScroller = new JScrollPane(selectedList);
        selectedListScroller.setPreferredSize(new Dimension(280, 350));
        selectedListScroller.setMinimumSize(selectedListScroller.getPreferredSize());
        selectedListScroller.setMaximumSize(selectedListScroller.getPreferredSize());
        selectedListPanel.add(selectedListScroller);
        datasetsPanel.add(selectedListPanel);

        dialog.getContentPane().add(datasetsPanel);

        final JPanel okButtonPanel = new JPanel();
        okBtn = new JButton("OK");
        okBtn.setEnabled(false);
        okBtn.addActionListener(e -> ok());
        okButtonPanel.add(okBtn);

        dialog.getContentPane().add(okButtonPanel);

        dialog.pack();
        dialog.setVisible(true);

        containerTree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) containerTree.getLastSelectedPathComponent();
            selectedNode = (node == null ? null : (N5TreeNode) node.getUserObject());
//            addSourceBtn.setEnabled( selectedNode != null );
            addSourceBtn.setEnabled( selectedNode != null && selectedNode.metadata != null );
//            addSourceBtn.setEnabled( selectedNode != null && filter.test( selectedNode ) );
        });
        
        if( n5 != null )
			openContainer( x -> n5, () -> "" );
    }

    private String openBrowseDialog()
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (lastBrowsePath != null && !lastBrowsePath.isEmpty())
            fileChooser.setCurrentDirectory(new File(lastBrowsePath));
        int ret = fileChooser.showOpenDialog(dialog);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        return fileChooser.getSelectedFile().getAbsolutePath();
    }

    private String openLinkDialog()
    {
        return JOptionPane.showInputDialog(dialog, "Link: ", "N5 Viewer", JOptionPane.PLAIN_MESSAGE);
    }
    
	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener )
    {
        final String n5Path = opener.get();
        n5 = n5Fun.apply( n5Path );

        final N5TreeNode n5RootNode;
        try
        {
			n5RootNode = datasetDiscoverer.discover( n5 );
        }
        catch (final IOException e) {
            IJ.handleException(e);
            return;
        }

        if( containerPathTxt != null )
			containerPathTxt.setText(n5Path);

        treeModel.setRoot(N5DatasetDiscoverer.toJTreeNode(n5RootNode));
        listModel.clear();

        containerTree.setEnabled(true);
        selectedList.setEnabled(true);
        removeSourceBtn.setEnabled(false);
        okBtn.setEnabled(false);
    }

    private void addSource()
    {
        if (selectedNode != null)
        {
            addSourceRecursive(selectedNode);

            selectedNode = null;
            containerTree.clearSelection();

            removeSourceBtn.setEnabled(true);
            okBtn.setEnabled(true);
        }
    }

    private void addSourceRecursive(final N5TreeNode node)
    {
        if (node.metadata != null) {
            listModel.addElement(new SelectedListElement(node.path, node.metadata));
        } else {
            for (final N5TreeNode childNode : node.children)
                addSourceRecursive(childNode);
        }
    }

    private void removeSource()
    {
        for (final Object selectedObject : selectedList.getSelectedValuesList())
            listModel.removeElement(selectedObject);
        removeSourceBtn.setEnabled(!listModel.isEmpty());
        okBtn.setEnabled(!listModel.isEmpty());
    }

    private void ok()
    {
        final List<N5Metadata> selectedMetadata = new ArrayList<>();
        for (final Enumeration enumeration = listModel.elements(); enumeration.hasMoreElements();)
            selectedMetadata.add(((SelectedListElement) enumeration.nextElement()).metadata);
        okCallback.accept(new DataSelection(n5, selectedMetadata));

        dialog.setVisible(false);
        dialog.dispose();
    }

    private static class SelectedListElement
    {
        private final String path;
        private final N5Metadata metadata;

        SelectedListElement(final String path, final N5Metadata metadata)
        {
            this.path = path;
            this.metadata = metadata;
        }

        @Override
        public String toString()
        {
            return path + (metadata instanceof N5MultiScaleMetadata ? " (multiscale)" : "");
        }
    }
}
