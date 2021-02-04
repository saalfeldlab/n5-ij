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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Factory;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

import ij.Prefs;
import net.imglib2.util.Pair;

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
    private N5DatasetDiscoverer datasetDiscoverer;

	private Consumer< DataSelection > okCallback;

	private JFrame dialog;

	private JTextField containerPathText;

	private JCheckBox virtualBox;

	private JCheckBox cropBox;

    private JTree containerTree;

//    private JLabel loadingIcon;

    private JButton browseBtn;

    private JButton detectBtn;

    private JLabel messageLabel;

    private JButton okBtn;

    private JButton cancelBtn;

    private DefaultTreeModel treeModel;

	private String lastBrowsePath;

	private Function< String, String > pathFun;

	private N5Reader n5;

	private boolean virtualOption = false;

	private boolean cropOption = false;

	private double guiScale;

	private Thread loaderThread;

	private ExecutorService loaderExecutor;

	private Future< N5TreeNode > parserFuture;

	private final String initialContainerPath;

	private Consumer< String > containerPathUpdateCallback;

	private Predicate< N5TreeNode > n5NodeFilter;

	private N5DatasetTreeCellRenderer treeRenderer;

	private final N5GroupParser<?>[] groupParsers;

	private final N5MetadataParser<?>[] parsers;

	private LinkedBlockingQueue< Future< N5TreeNode > > loaderFutures;

	private N5TreeNode rootNode;

	public DatasetSelectorDialog(
			final Function< String, String > pathFun,
			final String initialContainerPath,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.pathFun = pathFun;
		this.initialContainerPath = initialContainerPath;

		this.parsers = parsers;
		this.groupParsers = groupParsers;

		guiScale = Prefs.getGuiScale();
	}

	public DatasetSelectorDialog(
			final Function< String, String > pathFun,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this( pathFun, "", groupParsers, parsers );
	}

	public DatasetSelectorDialog(
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this( x -> "", groupParsers, parsers );
	}

	public DatasetSelectorDialog(
			final N5Reader n5,
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.n5 = n5;
		this.pathFun = x -> "";
		this.initialContainerPath = "";

		this.parsers = parsers;
		this.groupParsers = groupParsers;

	}

	public void setLoaderExecutor( final ExecutorService loaderExecutor )
	{
		this.loaderExecutor = loaderExecutor;
	}

	public N5DatasetDiscoverer getDatasetDiscoverer()
	{
		return datasetDiscoverer;
	}

	public void setTreeRenderer( final N5DatasetTreeCellRenderer treeRenderer )
	{
		this.treeRenderer = treeRenderer;
	}

	public void setRecursiveFilterCallback( final Predicate< N5TreeNode > n5NodeFilter )
	{
		this.n5NodeFilter = n5NodeFilter;
	}

	public void setContainerPathUpdateCallback( final Consumer< String > containerPathUpdateCallback )
	{
		this.containerPathUpdateCallback = containerPathUpdateCallback;
	}

	public void setMessage( final String message )
	{
		messageLabel.setText( message );
	}

	public void setVirtualOption( final boolean arg )
	{
		virtualOption = arg;
	}

	public void setCropOption( final boolean arg )
	{
		cropOption = arg;
	}

	public boolean getCropOption()
	{
		return cropBox.isSelected();
	}

	public boolean isVirtual()
	{
		return ( virtualBox != null ) && virtualBox.isSelected();
	}

	public String getN5RootPath()
	{
		return containerPathText.getText();
	}

	public void setLoaderThread( final Thread loaderThread )
	{
		this.loaderThread = loaderThread;
	}

    public void run( final Consumer< DataSelection > okCallback )
    {
        this.okCallback = okCallback;
        dialog = buildDialog();

        if( n5 == null )
        {
			browseBtn.addActionListener( e -> openContainer( this::openBrowseDialog ));
			detectBtn.addActionListener( e -> openContainer( () -> getN5RootPath() ));
        }

        // ok and cancel buttons
		okBtn.addActionListener( e -> ok() );
		cancelBtn.addActionListener( e -> cancel() );
		dialog.setVisible( true );
    }

    private static final int DEFAULT_OUTER_PAD = 8;
    private static final int DEFAULT_BUTTON_PAD = 3;
    private static final int DEFAULT_MID_PAD = 5;

    private JFrame buildDialog()
    {
    	final int OUTER_PAD = (int)( guiScale * DEFAULT_OUTER_PAD );
    	final int BUTTON_PAD = (int)( guiScale * DEFAULT_BUTTON_PAD );
    	final int MID_PAD = (int)( guiScale * DEFAULT_MID_PAD );

		final int frameSizeX = (int)( guiScale * 600 );
		final int frameSizeY = (int)( guiScale * 400 );

		dialog = new JFrame( "Open N5" );
		dialog.setPreferredSize( new Dimension( frameSizeX, frameSizeY ) );
		dialog.setMinimumSize( dialog.getPreferredSize() );

		final Container pane = dialog.getContentPane();
		pane.setLayout( new GridBagLayout() );

		containerPathText = new JTextField();
		containerPathText.setText( initialContainerPath );
		containerPathText.setPreferredSize( new Dimension( frameSizeX / 3, containerPathText.getPreferredSize().height ));

//		containerPathText.addActionListener( e -> openContainer( () -> getN5RootPath(), pathFun ));
		containerPathText.addActionListener( e -> openContainer( () -> getN5RootPath() ));

		scale( containerPathText );

		final GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 3;
		ctxt.gridheight = 1;
		ctxt.weightx = 1.0;
		ctxt.weighty = 0.0;
		ctxt.fill = GridBagConstraints.HORIZONTAL;
		ctxt.insets = new Insets( OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD );
		pane.add( containerPathText, ctxt );

		browseBtn = scaleFont( new JButton( "Browse" ));
		final GridBagConstraints cbrowse = new GridBagConstraints();
		cbrowse.gridx = 3;
		cbrowse.gridy = 0;
		cbrowse.gridwidth = 1;
		cbrowse.gridheight = 1;
		cbrowse.weightx = 0.0;
		cbrowse.weighty = 0.0;
		cbrowse.fill = GridBagConstraints.HORIZONTAL;
		cbrowse.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, BUTTON_PAD );
		pane.add( browseBtn, cbrowse );

		detectBtn = scaleFont( new JButton( "Detect datasets" ) );
		final GridBagConstraints cdetect = new GridBagConstraints();
		cdetect.gridx = 4;
		cdetect.gridy = 0;
		cdetect.gridwidth = 2;
		cdetect.gridheight = 1;
		cdetect.weightx = 0.0;
		cdetect.weighty = 0.0;
		cdetect.fill = GridBagConstraints.HORIZONTAL;
		cdetect.insets = new Insets( OUTER_PAD, BUTTON_PAD, MID_PAD, OUTER_PAD );
		pane.add( detectBtn, cdetect );

		final GridBagConstraints ctree = new GridBagConstraints();
		ctree.gridx = 0;
		ctree.gridy = 1;
		ctree.gridwidth = 6;
		ctree.gridheight = 3;
		ctree.weightx = 1.0;
		ctree.weighty = 1.0;
		ctree.ipadx = 0;
		ctree.ipady = 0;
		ctree.insets = new Insets( 0, OUTER_PAD, 0, OUTER_PAD );
		ctree.fill = GridBagConstraints.BOTH;

		treeModel = new DefaultTreeModel( null );
		containerTree = new JTree( treeModel );
		containerTree.setMinimumSize( new Dimension( 550, 230 ));
		scaleFont( containerTree, (float)guiScale * 1.2f );

		containerTree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

        // disable selection of nodes that are not open-able
		containerTree.addTreeSelectionListener(
				new N5IjTreeSelectionListener( containerTree.getSelectionModel() ));

        // By default leaf nodes (datasets) are displayed as files. This changes the default behavior to display them as folders
//        final DefaultTreeCellRenderer treeCellRenderer = (DefaultTreeCellRenderer) containerTree.getCellRenderer();
		if( treeRenderer != null )
			containerTree.setCellRenderer( treeRenderer );

		final JScrollPane treeScroller = new JScrollPane( containerTree );
		treeScroller.setViewportView( containerTree );
		treeScroller.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
		pane.add( treeScroller, ctree );

		// bottom button
		final GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 4;
		cbot.gridwidth = 1;
		cbot.gridheight = 1;
		cbot.weightx = 0.0;
		cbot.weighty = 0.0;
		cbot.insets = new Insets( OUTER_PAD, OUTER_PAD, OUTER_PAD, OUTER_PAD );
		cbot.anchor = GridBagConstraints.CENTER;

		if( virtualOption )
		{
			final JPanel virtPanel = new JPanel();
			virtualBox = new JCheckBox();
			final JLabel virtLabel = scaleFont(new JLabel( "Open as virtual" ));
			virtPanel.add( virtualBox );
			virtPanel.add( virtLabel );
			pane.add( virtPanel, cbot );
		}

		if( cropOption )
		{
			final JPanel cropPanel = new JPanel();
			cropBox = new JCheckBox();
			final JLabel cropLabel = scaleFont( new JLabel( "Crop" ));
			cbot.gridx = 1;
			cbot.anchor = GridBagConstraints.WEST;
			cropPanel.add( cropBox );
			cropPanel.add( cropLabel );
			pane.add( cropPanel, cbot );
		}

		messageLabel = scaleFont( new JLabel(""));
		messageLabel.setVisible( false );
		cbot.gridx = 2;
		cbot.anchor = GridBagConstraints.CENTER;
		pane.add( messageLabel, cbot );

		okBtn = scaleFont( new JButton( "OK" ));
		cbot.gridx = 4;
		cbot.ipadx = (int) ( 20 * guiScale );
		cbot.anchor = GridBagConstraints.EAST;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.insets = new Insets( MID_PAD, OUTER_PAD, OUTER_PAD, BUTTON_PAD );
		pane.add( okBtn, cbot );

		cancelBtn = scaleFont( new JButton( "Cancel" ));
		cbot.gridx = 5;
		cbot.ipadx = 0;
		cbot.anchor = GridBagConstraints.EAST;
		cbot.fill = GridBagConstraints.HORIZONTAL;
		cbot.insets = new Insets( MID_PAD, BUTTON_PAD, OUTER_PAD, OUTER_PAD );
		pane.add( cancelBtn, cbot );

		dialog.pack();
		return dialog;
    }

    private String openBrowseDialog()
    {
        final JFileChooser fileChooser = new JFileChooser();
        /*
         *  Need to allow files so h5 containers can be opened,
         *  and directories so that filesystem n5's and zarrs can be opened.
         */
        fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );

        if (lastBrowsePath != null && !lastBrowsePath.isEmpty())
            fileChooser.setCurrentDirectory(new File(lastBrowsePath));
        final int ret = fileChooser.showOpenDialog(dialog);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;

        final String path = fileChooser.getSelectedFile().getAbsolutePath();
        containerPathText.setText( path );
        return path;
    }

	private void openContainer( final Supplier< String > opener )
	{
		openContainer( opener.get() );
	}

	private void openContainer( final String n5TotalPath )
    {

		messageLabel.setText( "Building reader..." );
		messageLabel.setVisible( true );
		dialog.repaint();
		dialog.revalidate();

		containerPathUpdateCallback.accept( n5TotalPath );
		if( n5TotalPath == null )
		{
			messageLabel.setVisible( false );
			dialog.repaint();
			return;
		}

//		final String rootPath = pathToRoot.apply( n5Path );
		final Pair<String,String> rootDataset = N5Importer.n5RootDataset( n5TotalPath );
		final String rootPath = rootDataset.getA();
		final String dataset = rootDataset.getB();

		try
		{
			n5 = N5Factory.openReader( rootPath );
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
		}

		if ( n5 == null )
		{
			messageLabel.setVisible( false );
			dialog.repaint();
			return;
		}

		messageLabel.setText( "Discovering datasets..." );
		messageLabel.setVisible( true );
		dialog.repaint();

		if( loaderExecutor == null )
		{
			loaderExecutor = Executors.newCachedThreadPool();
		}

		datasetDiscoverer = new N5DatasetDiscoverer( loaderExecutor, n5NodeFilter, groupParsers, parsers );
		rootNode = new N5TreeNode( dataset, false );

		try
		{
			loaderFutures = datasetDiscoverer.discoverThreads( n5, rootNode );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		// a thread that completes task after parsing is complete
		new LoaderSorterAndCallback( rootNode, 500 ).start();

		containerTree.setEnabled( true );
    }

    private void ok()
    {
		final ArrayList< N5Metadata > selectedMetadata = new ArrayList<>();

		// check if we can skip explicit dataset detection
		if ( containerTree.getSelectionCount() == 0 )
		{
			final String n5Path = getN5RootPath();
			containerPathUpdateCallback.accept( getN5RootPath() );

			try
			{
				n5 = N5Factory.openReader( n5Path );
			}
			catch ( IOException e1 )
			{
				e1.printStackTrace();
				return;
			}

			final String dataset = pathFun.apply( n5Path );
			N5TreeNode node = null;
			try
			{
				node = datasetDiscoverer.parse( n5, dataset );
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
			// datasets were selected by the user
			for( final TreePath path : containerTree.getSelectionPaths() )
				selectedMetadata.add( ((N5TreeNode)path.getLastPathComponent()).getMetadata() );
		}
		okCallback.accept( new DataSelection( n5, selectedMetadata ) );
        dialog.setVisible(false);
        dialog.dispose();
    }

    private void cancel()
    {
		dialog.setVisible( false );
        dialog.dispose();

        if( loaderThread != null )
			loaderThread.interrupt();

		if ( parserFuture != null )
		{
			parserFuture.cancel( true );
		}
    }

	private static final Font DEFAULT_FONT = new Font( Font.SANS_SERIF, Font.PLAIN, 12 );

	private < T extends Component > T scaleFont( final T c )
    {
		Font font = c.getFont();
		if (font == null)
			font = DEFAULT_FONT;
		font = font.deriveFont( (float) guiScale * 1.2f * font.getSize() );
		c.setFont(font);
		return c;
    }

	private static < T extends Component > T scaleFont( final T c, final float scale )
    {
		Font font = c.getFont();
		if (font == null)
			font = DEFAULT_FONT;
		font = font.deriveFont( scale * 1.2f * font.getSize() );
		c.setFont(font);
		return c;
    }

	private < T extends Component > T scaleSize( final T c )
	{
		final Dimension prefSz = c.getPreferredSize();
		c.setPreferredSize(
				new Dimension(
						( int ) ( guiScale * prefSz.width ),
						( int ) ( guiScale * prefSz.height )));
		return c;
	}

	private < T extends Component > T scale( final T c )
	{
		return scaleSize( scaleFont( c ) );
	}

	private class LoaderSorterAndCallback extends Thread
	{
		final long waitIntervalMs;
		private N5TreeNode root;
		public LoaderSorterAndCallback( final N5TreeNode root, final long waitInterval )
		{
			this.waitIntervalMs = waitInterval;
			this.root = root;
		}

		@Override
		public void run()
		{
			try
			{
				while( loaderFutures.size() > 0 )
				{
					try
					{
						loaderFutures.poll().get();
					}
					catch ( ExecutionException e )
					{
						e.printStackTrace();
					}
				}

				datasetDiscoverer.sortAndTrimRecursive( rootNode );
				datasetDiscoverer.parseGroupsRecursive( rootNode );
				datasetDiscoverer.filterRecursive( rootNode );

				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						// expand first layer of the tree
						// in the EDGT
						@SuppressWarnings( "rawtypes" )
						Enumeration e = rootNode.children(); 
						while( e.hasMoreElements() )
						{
							N5TreeNode child = ( N5TreeNode ) e.nextElement();
							containerTree.expandPath( new TreePath( 
									new N5TreeNode[]{ rootNode, child }));
						}

						// set the root node for the JTree
						treeModel.setRoot( rootNode );
						messageLabel.setText( "Done" );
						dialog.repaint();
					}
				});

				Thread.sleep( waitIntervalMs );

				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						messageLabel.setVisible( false );
						dialog.repaint();
					}
				});

				this.join();
			}
			catch( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Removes selected nodes that do not have metadata, and are therefore not openable.
	 */
	public static class N5IjTreeSelectionListener implements TreeSelectionListener
	{
		private TreeSelectionModel selectionModel;

		public N5IjTreeSelectionListener( final TreeSelectionModel selectionModel )
		{
			this.selectionModel = selectionModel;
		}

		@Override
		public void valueChanged( final TreeSelectionEvent sel )
		{
			int i = 0;
			for( final TreePath path : sel.getPaths())
			{
				if( !sel.isAddedPath( i ))
					continue;

				final Object last = path.getLastPathComponent();
				if( last instanceof N5TreeNode )
				{
					final N5TreeNode node = (N5TreeNode)last;
					if( node.getMetadata() == null )
					{
						selectionModel.removeSelectionPath( path );
					}
				}
				i++;
			}
		}
	}

}
