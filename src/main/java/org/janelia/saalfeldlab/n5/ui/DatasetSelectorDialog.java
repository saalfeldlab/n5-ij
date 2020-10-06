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
import ij.Prefs;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
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

	private Function< String, N5Reader > n5Fun;

	private Function< String, String > pathFun;

	private N5Reader n5;

	private boolean virtualOption = false;

	private boolean cropOption = false;

	private double guiScale;

	private Thread loaderThread;

	private Future< N5TreeNode > parserFuture;

	public DatasetSelectorDialog( 
			final Function< String, N5Reader > n5Fun, 
			final Function< String, String > pathFun, 
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.n5Fun = n5Fun;
		this.pathFun = pathFun;
		datasetDiscoverer = new N5DatasetDiscoverer( groupParsers, parsers );
		guiScale = Prefs.getGuiScale();
	}

	public DatasetSelectorDialog( 
			final Function< String, N5Reader > n5Fun, 
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this( n5Fun, x -> "", groupParsers, parsers );
	}

	public DatasetSelectorDialog( 
			final N5Reader n5, 
			final N5GroupParser<?>[] groupParsers,
			final N5MetadataParser< ? >... parsers )
	{
		this.n5 = n5;
		datasetDiscoverer = new N5DatasetDiscoverer( groupParsers, parsers );
	}

	public void setVirtualOption( boolean arg )
	{
		virtualOption = arg;
	}

	public void setCropOption( boolean arg )
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
			browseBtn.addActionListener( e -> openContainer( n5Fun, this::openBrowseDialog ));
			detectBtn.addActionListener( e -> openContainer( n5Fun, () -> getN5RootPath(), pathFun ));
        }

		containerTree.setEnabled( false );
		containerTree.getSelectionModel().setSelectionMode( 
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

        // disable selection of nodes that are not open-able
		containerTree.addTreeSelectionListener( 
				new N5Importer.N5IjTreeSelectionListener( containerTree.getSelectionModel() ));

        // ok and cancel buttons
		okBtn.addActionListener( e -> ok() );
		cancelBtn.addActionListener( e -> cancel() );
		dialog.setVisible( true );
    }

    private JFrame buildDialog()
    {
		int frameSizeX = (int)(guiScale * 600);
		int frameSizeY = (int)(guiScale * 400);

		dialog = new JFrame( "Open N5" );
		dialog.setPreferredSize( new Dimension( frameSizeX, frameSizeY ) );
		dialog.setMinimumSize( dialog.getPreferredSize() );

		Container pane = dialog.getContentPane();
		pane.setLayout( new GridBagLayout() );

		containerPathText = new JTextField();
		containerPathText.setPreferredSize( new Dimension( frameSizeX / 3, containerPathText.getPreferredSize().height ));
		scale( containerPathText );

		GridBagConstraints ctxt = new GridBagConstraints();
		ctxt.gridx = 0;
		ctxt.gridy = 0;
		ctxt.gridwidth = 3;
		ctxt.gridheight = 1;
		ctxt.weightx = 0.8;
		ctxt.weighty = 0.1;
		ctxt.fill = GridBagConstraints.HORIZONTAL;
		ctxt.insets = new Insets( 0, 8, 0, 2 );
		pane.add( containerPathText, ctxt );

		browseBtn = scaleFont( new JButton( "Browse" ));
		GridBagConstraints cbrowse = new GridBagConstraints();
		cbrowse.gridx = 3;
		cbrowse.gridy = 0;
		cbrowse.gridwidth = 1;
		cbrowse.gridheight = 1;
		cbrowse.weightx = 0.1;
		cbrowse.weighty = 0.1;
		pane.add( browseBtn, cbrowse );

		detectBtn = scaleFont( new JButton( "Detect datasets" ) );
		GridBagConstraints cdetect = new GridBagConstraints();
		cdetect.gridx = 4;
		cdetect.gridy = 0;
		cdetect.gridwidth = 2;
		cdetect.gridheight = 1;
		cdetect.weightx = 0.1;
		cdetect.weighty = 0.1;
		pane.add( detectBtn, cdetect );

		GridBagConstraints ctree = new GridBagConstraints();
		ctree.gridx = 0;
		ctree.gridy = 1;
		ctree.gridwidth = 6;
		ctree.gridheight = 3;
		ctree.weightx = 0.9;
		ctree.weighty = 0.9;
		ctree.ipadx = 5;
		ctree.ipady = 10;
		ctree.fill = GridBagConstraints.BOTH;

		treeModel = new DefaultTreeModel( null );
		containerTree = new JTree( treeModel );
		containerTree.setMinimumSize( new Dimension( 550, 230 ));
		containerTree.setPreferredSize( new Dimension( 550, 230 ));
		scaleFont( containerTree, (float)guiScale * 1.2f );

        // By default leaf nodes (datasets) are displayed as files. This changes the default behavior to display them as folders
        final DefaultTreeCellRenderer treeCellRenderer = (DefaultTreeCellRenderer) containerTree.getCellRenderer();
        treeCellRenderer.setLeafIcon(treeCellRenderer.getOpenIcon());

		final JScrollPane treeScroller = new JScrollPane( containerTree );
		JScrollPane scroll = new JScrollPane( treeScroller );
		scroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		pane.add( scroll, ctree );

		// bottom button
		GridBagConstraints cbot = new GridBagConstraints();
		cbot.gridx = 0;
		cbot.gridy = 4;
		cbot.gridwidth = 1;
		cbot.gridheight = 1;
		cbot.weightx = 0.0;
		cbot.weighty = 0.1;
		cbot.anchor = GridBagConstraints.CENTER;

		JPanel virtPanel = new JPanel();
		virtualBox = new JCheckBox();
		JLabel virtLabel = scaleFont(new JLabel( "Open as virtual" ));
		virtPanel.add( virtualBox );
		virtPanel.add( virtLabel );
		pane.add( virtPanel, cbot );

		JPanel cropPanel = new JPanel();
		cropBox = new JCheckBox();
		JLabel cropLabel = scaleFont( new JLabel( "Crop" ));
		cbot.gridx = 1;
		cbot.anchor = GridBagConstraints.WEST;
		cropPanel.add( cropBox );
		cropPanel.add( cropLabel );
		pane.add( cropPanel, cbot );

		messageLabel = scaleFont( new JLabel(""));
		messageLabel.setVisible( false );
		cbot.gridx = 3;
		cbot.anchor = GridBagConstraints.CENTER;
		pane.add( messageLabel, cbot );

		okBtn = scaleFont( new JButton( "OK" ));
		cbot.gridx = 4;
		cbot.anchor = GridBagConstraints.EAST;
		pane.add( okBtn, cbot );

		cancelBtn = scaleFont( new JButton( "Cancel" ));
		cbot.gridx = 5;
		cbot.anchor = GridBagConstraints.CENTER;
		pane.add( cancelBtn, cbot );

		dialog.pack();
		return dialog;
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

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener )
	{
		openContainer( n5Fun, opener, pathFun );
	}

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener,
			final Function<String,String> pathToRoot )
    {
		messageLabel.setText( "Building reader..." );
		messageLabel.setVisible( true );
		dialog.repaint();
		dialog.revalidate();

		final String n5Path = opener.get();
		if( n5Path == null )
		{
			messageLabel.setVisible( false );
			dialog.repaint();
			return;
		}

		n5 = n5Fun.apply( n5Path );
		final String rootPath = pathToRoot.apply( n5Path );

		if ( n5 == null )
		{
			messageLabel.setVisible( false );
			dialog.repaint();
			return;
		}

		messageLabel.setText( "Discovering datasets..." );
		messageLabel.setVisible( true );
		dialog.repaint();

		DiscoverRunner discoverRunner = new DiscoverRunner( datasetDiscoverer, n5, rootPath, treeModel, messageLabel );

	    ExecutorService executor = Executors.newSingleThreadExecutor();
		ExecutorCompletionService<N5TreeNode> ecs = new ExecutorCompletionService<>( executor );
		ecs.submit( discoverRunner );

		containerTree.setEnabled( true );
    }

    private void ok()
    {
		final ArrayList< N5Metadata > selectedMetadata = new ArrayList<>();

		// check if we can skip explicit dataset detection
		if ( containerTree.getSelectionCount() == 0 )
		{
			final String n5Path = getN5RootPath();
			n5 = n5Fun.apply( n5Path );
//			final String dataset = pathFun.apply( n5Path );
			final String dataset = "";
			N5TreeNode node = null;
			try
			{
				node = datasetDiscoverer.parse( n5, dataset );
				if ( node.isDataset() && node.getMetadata() != null )
					selectedMetadata.add( node.getMetadata() );
			}
			catch ( Exception e ){}

			if ( node == null || !node.isDataset() || node.getMetadata() == null )
			{
				JOptionPane.showMessageDialog( null, "Could not find a dataset / metadata at the provided path." );
				return;
			}
		}
		else
		{
			// datasets were selected by the user
			for( TreePath path : containerTree.getSelectionPaths() )
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

	private < T extends Component > T scaleFont( T c )
    {
		Font font = c.getFont();
		if (font == null)
			font = DEFAULT_FONT;
		font = font.deriveFont( (float) guiScale * font.getSize() );
		c.setFont(font);
		return c;
    }

	private static < T extends Component > T scaleFont( T c, float scale )
    {
		Font font = c.getFont();
		if (font == null)
			font = DEFAULT_FONT;
		font = font.deriveFont( (float)( scale * font.getSize()) );
		c.setFont(font);
		return c;
    }

	private < T extends Component > T scaleSize( T c )
	{
		Dimension prefSz = c.getPreferredSize();
		c.setPreferredSize(
				new Dimension( 
						( int ) ( guiScale * prefSz.width ),
						( int ) ( guiScale * prefSz.height )));
		return c;
	}

	private < T extends Component > T scale( T c )
	{
		return scaleSize( scaleFont( c ) );
	}

	private static class DiscoverRunner implements Callable< N5TreeNode >
	{
		private final N5Reader n5;

		private final String rootPath;

		private final N5DatasetDiscoverer datasetDiscoverer;

		private final JLabel message;

		private final DefaultTreeModel treeModel;

		public DiscoverRunner(
				final N5DatasetDiscoverer datasetDiscoverer,
				final N5Reader n5, final String rootPath,
				final DefaultTreeModel treeModel,
				final JLabel message )
		{
			this.n5 = n5;
			this.rootPath = rootPath;
			this.datasetDiscoverer = datasetDiscoverer;
			this.message = message;
			this.treeModel = treeModel;
		}

		@Override
		public N5TreeNode call()
		{
			try
			{
				N5TreeNode node = datasetDiscoverer.discoverRecursive( n5, rootPath );
				SwingUtilities.invokeLater( new Runnable() {
					public void run()
					{
						treeModel.setRoot( node );
						message.setVisible( false );
					}
				});
				return node;
			}
			catch ( final IOException e )
			{
				IJ.handleException( e );
			}
			return null;
		}
	}

}
