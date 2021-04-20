/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MultiScaleMetadata;

import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;

public class DatasetSelectorDialogV0
{
    /**
     * The dataset/group discoverer that takes a list of metadata parsers.
     *
     * Currently, there is only one parser for N5 Viewer-style metadata (that comes from the previous version of this plugin).
     *
     * To add more parsers, add a new class that implements {@link N5MetadataParser}
     * and pass an instance of it to the {@link N5DatasetDiscoverer} constructor here.
     */
    
	private final N5GroupParser<?>[] groupParsers;

	private final N5MetadataParser< ? >[] parsers;

	private Consumer< DataSelection > okCallback;

    private JFrame dialog;
    private JTextField containerPathTxt;

	private List< JSpinner > minSpinners;
	private List< JSpinner > maxSpinners;

	private JCheckBox virtualBox;

    private JTree containerTree;
    private JList selectedList;

    private JButton addSourceBtn;
    private JButton removeSourceBtn;

    private JButton okBtn;

    private JButton cancelBtn;

    private DefaultTreeModel treeModel;

    private DefaultListModel listModel;

	private String lastBrowsePath;

	private Function< String, N5Reader > n5Fun;

	private Function< String, String > pathFun;

	private N5Reader n5;

	private N5TreeNode selectedNode;

	private boolean virtualOption = false;

	private boolean minMaxOption = false;

	private int nd;

	public DatasetSelectorDialogV0(
			final Function< String, N5Reader > n5Fun,
			final Function< String, String > pathFun,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.n5Fun = n5Fun;
		this.pathFun = pathFun;
		this.groupParsers = groupParsers;
		this.parsers = parsers;
	}

	public DatasetSelectorDialogV0(
			final Function< String, N5Reader > n5Fun,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this( n5Fun, x -> "", groupParsers, parsers );
	}

	public DatasetSelectorDialogV0(
			final N5Reader n5,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.n5 = n5;
		this.groupParsers = groupParsers;
		this.parsers = parsers;
	}

	public void setVirtualOption( final boolean arg )
	{
		virtualOption = arg;
	}

	public void setMinMaxOption( final boolean arg )
	{
		minMaxOption = arg;
	}

	public boolean isVirtual()
	{
		return ( virtualBox != null ) && virtualBox.isSelected();
	}

	public Interval getMinMax()
	{
		if( !minMaxOption || nd < 1 )
			return null;

		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];
		for ( int i = 0; i < nd; i++ )
		{
			if( i < 3 ) // limiting to 3d cropping for now
			{
				min[ i ] = ( Integer ) minSpinners.get( i ).getValue();
				max[ i ] = ( Integer ) maxSpinners.get( i ).getValue();
			}
		}
		return new FinalInterval( min, max );
	}

    public void run( final Consumer< DataSelection > okCallback )
    {
        this.okCallback = okCallback;

        dialog = new JFrame("N5 Viewer");
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));
        dialog.setMinimumSize(new Dimension(800, 600));
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

			final JButton detectBtn = new JButton("Detect datasets");
			detectBtn.addActionListener( e -> openContainer(
					n5Fun,
					() -> containerPathTxt.getText(),
					pathFun ));
			containerPanel.add( detectBtn );

			dialog.getContentPane().add( containerPanel );

			if( minMaxOption )
			{
				// add min/max options
				final JPanel minMaxPanel = new JPanel();
				minMaxPanel.setLayout( new GridLayout( 2, 4 ));

				minSpinners = new ArrayList<>();
				maxSpinners = new ArrayList<>();
				for( int i = 0; i < 3; i++ )
				{
					minSpinners.add( new JSpinner() );
					maxSpinners.add( new JSpinner() );
				}
				defaultMinMax();

				minMaxPanel.add( new JLabel( "Min: "));
				minSpinners.stream().forEach( x -> minMaxPanel.add( x ) );
				minMaxPanel.add( new JLabel( "Max: "));
				maxSpinners.stream().forEach( x -> minMaxPanel.add( x ) );

				dialog.add( minMaxPanel );
			}
        }

		if ( virtualOption )
		{
			final JPanel virtualPanel = new JPanel();
			virtualPanel.add( new JLabel( "Open as virtual: " ) );

			virtualBox = new JCheckBox();
			virtualBox.setSelected( true );
			virtualPanel.add( virtualBox );

			dialog.add( virtualPanel );
		}

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
        okBtn.addActionListener(e -> ok());
        okButtonPanel.add(okBtn);

		cancelBtn = new JButton( "Cancel" );
		cancelBtn.addActionListener( e -> cancel() );
		okButtonPanel.add( cancelBtn );

        dialog.getContentPane().add(okButtonPanel);

        dialog.pack();
        dialog.setVisible(true);

        containerTree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) containerTree.getLastSelectedPathComponent();
            selectedNode = (node == null ? null : (N5TreeNode) node.getUserObject());
            addSourceBtn.setEnabled( selectedNode != null && selectedNode.getMetadata() != null );
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
        final int ret = fileChooser.showOpenDialog(dialog);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        return fileChooser.getSelectedFile().getAbsolutePath();
    }

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener )
	{
		openContainer( n5Fun, opener, pathFun );
	}

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener,
			final Function<String,String> pathToRoot )
    {
		final String n5Path = opener.get();
		n5 = n5Fun.apply( n5Path );
		final String rootPath = pathToRoot.apply( n5Path );

		if ( n5 == null )
			return;

        final N5TreeNode n5RootNode;
        try
        {
        	final N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer( n5, groupParsers, parsers );
			n5RootNode = datasetDiscoverer.discoverRecursive( rootPath );
			if( n5RootNode.isDataset() )
				okBtn.setEnabled( true );
        }
		catch ( final IOException e )
        {
            IJ.handleException(e);
            return;
        }

        if( containerPathTxt != null )
			containerPathTxt.setText(n5Path);

//        treeModel.setRoot(N5DatasetDiscoverer.toJTreeNode(n5RootNode));
		treeModel.setRoot( n5RootNode.asTreeNode() );
        listModel.clear();

        containerTree.setEnabled(true);
        selectedList.setEnabled(true);
        removeSourceBtn.setEnabled(false);

		if ( n5RootNode.isDataset() )
			okBtn.setEnabled( true );
		else
			okBtn.setEnabled( false );
    }

	private void defaultMinMax()
	{
		if( !minMaxOption )
			return;

		for ( int i = 0; i < 3; i++ )
		{
			minSpinners.get( i ).setModel( new SpinnerListModel( new String[] { "-" } ) );
			maxSpinners.get( i ).setModel( new SpinnerListModel( new String[] { "-" } ) );
		}
	}

	private void updateMinMax( final N5TreeNode node )
	{
		if( !minMaxOption )
			return;

        // add min/max options
		try
		{
			if ( !n5.datasetExists( node.getPath() ) )
			{
				return;
			}
			final DatasetAttributes attr = n5.getDatasetAttributes( node.getPath() );
			nd = attr.getNumDimensions();
			for( int i = 0; i < nd; i++ )
			{
				final int max = (int)attr.getDimensions()[ i ] - 1;
				minSpinners.get( i ).setModel( new SpinnerNumberModel( 0, 0, max, 1 ));
				maxSpinners.get( i ).setModel( new SpinnerNumberModel( max, 0, max, 1 ));
			}
		}
		catch ( final Exception e )
		{
			return;
		}
	}

    private void addSource()
    {
        if (selectedNode != null)
        {
            addSourceRecursive(selectedNode);
            updateMinMax( selectedNode );

            selectedNode = null;
            containerTree.clearSelection();

            removeSourceBtn.setEnabled(true);
            okBtn.setEnabled(true);
        }
    }

    private void addSourceRecursive(final N5TreeNode node)
    {
		if ( node.getMetadata() != null )
		{
			listModel.addElement( new SelectedListElement( node.getPath(), node.getMetadata() ) );
		}
		else
		{
			for ( final N5TreeNode childNode : node.childrenList() )
				addSourceRecursive( childNode );
		}
    }

    private void removeSource()
    {
		for ( final Object selectedObject : selectedList.getSelectedValuesList() )
			listModel.removeElement( selectedObject );

		removeSourceBtn.setEnabled( !listModel.isEmpty() );
		okBtn.setEnabled( !listModel.isEmpty() );

		if ( listModel.size() == 0 )
			defaultMinMax();
    }

    private void ok()
    {
		final List< N5Metadata > selectedMetadata = new ArrayList<>();
		if ( listModel.isEmpty() )
		{
			final String n5Path = containerPathTxt.getText();
			n5 = n5Fun.apply( n5Path );
			final String dataset = pathFun.apply( n5Path );
			N5TreeNode node = null;
			try
			{
	        	final N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer( n5, groupParsers, parsers );
				node = datasetDiscoverer.parse( dataset );
				if ( node.isDataset() && node.getMetadata() != null )
					selectedMetadata.add( node.getMetadata() );
			}
			catch ( final Exception e ){}

			if ( node == null || !node.isDataset() || node.getMetadata() == null )
			{
				JOptionPane.showMessageDialog( null, "Could not find a dataset / metadata at the provided path." );
				return;
			}
		}
		else
		{
			for ( final Enumeration enumeration = listModel.elements(); enumeration.hasMoreElements(); )
				selectedMetadata.add( ( ( SelectedListElement ) enumeration.nextElement() ).metadata );
		}

		okCallback.accept( new DataSelection( n5, selectedMetadata ) );
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void cancel()
    {
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
