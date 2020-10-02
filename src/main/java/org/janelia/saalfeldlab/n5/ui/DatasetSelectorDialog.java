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

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

        dialog = new JFrame("N5 Viewer");
        dialog.setLayout( new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS ));
		dialog.setMinimumSize(
				new Dimension( ( int ) guiScale * 600, ( int ) guiScale * 320 ) );
        dialog.setPreferredSize(dialog.getMinimumSize());

        if( n5 == null )
        {
			final JPanel containerPanel = new JPanel();
			containerPanel.add( scale( new JLabel("N5 container:" )));

			containerPathText = new JTextField();
			containerPathText.setPreferredSize( 
					new Dimension( 
							( int ) ( guiScale * 200 ),
							( int ) ( guiScale * containerPathText.getPreferredSize().height ) ) );
			scale( containerPathText );
			containerPanel.add(containerPathText);

			final JButton browseBtn = scaleFont( new JButton("Browse..."));
			browseBtn.addActionListener(e -> openContainer( n5Fun, this::openBrowseDialog));
			containerPanel.add(browseBtn);

			final JButton detectBtn = scaleFont( new JButton("Detect datasets"));
			detectBtn.addActionListener( e -> openContainer( 
					n5Fun,
					() -> getN5RootPath(),
					pathFun ));
			containerPanel.add( detectBtn );

			dialog.getContentPane().add( containerPanel );
        }

		final JPanel containerTreePanel = new JPanel();
		containerTreePanel.setBorder( BorderFactory.createTitledBorder( "N5 dataset tree" ) );
		containerTreePanel.setLayout( new BoxLayout( containerTreePanel, BoxLayout.Y_AXIS ) );

		treeModel = new DefaultTreeModel( null );
		containerTree = new JTree( treeModel );
		containerTree.setEnabled( false );
		containerTree.getSelectionModel().setSelectionMode( 
				TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );

        // disable selection of nodes that are not open-able
		containerTree.addTreeSelectionListener( 
				new N5Importer.N5IjTreeSelectionListener( containerTree.getSelectionModel() ));

        // By default leaf nodes (datasets) are displayed as files. This changes the default behavior to display them as folders
        final DefaultTreeCellRenderer treeCellRenderer = (DefaultTreeCellRenderer) containerTree.getCellRenderer();
        treeCellRenderer.setLeafIcon(treeCellRenderer.getOpenIcon());
        scaleFont( treeCellRenderer );

		final JScrollPane containerTreeScroller = new JScrollPane( containerTree );
		containerTreeScroller.setPreferredSize( 
				new Dimension( ( int ) guiScale * 550, ( int ) guiScale * 200 ) );
        containerTreeScroller.setMinimumSize(containerTreeScroller.getPreferredSize());
        containerTreeScroller.setMaximumSize(containerTreeScroller.getPreferredSize());
        containerTreePanel.add(containerTreeScroller, new GridBagConstraints() );

        dialog.getContentPane().add(containerTreePanel, new GridBagConstraints() );

        // bottom panel
        final JPanel buttonPanel = new JPanel();

        // add is virtual checkbox if requested
		if ( virtualOption )
		{
			virtualBox = new JCheckBox();
			virtualBox.setSelected( true );
			buttonPanel.add( virtualBox );
			buttonPanel.add( scaleFont( new JLabel( "Open as virtual" ) ));
		}

		if ( cropOption )
		{
			if ( virtualOption )
				buttonPanel.add( Box.createRigidArea( new Dimension( ( int ) guiScale * 15, 0 ) ) );

			cropBox = new JCheckBox();
			cropBox.setSelected( true );
			buttonPanel.add( cropBox );
			buttonPanel.add( scaleFont( new JLabel( "Crop" )));
		}

		// space goes here
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.LINE_AXIS ) );
		buttonPanel.setBorder( BorderFactory.createEmptyBorder( 0,  (int) guiScale * 10,  (int) guiScale * 10, 10 ) );
		buttonPanel.add( Box.createHorizontalGlue() );

//		String grifPath = "/home/john/Pictures/load_small.gif";
//		//BufferedImage loadingGif = ImageIO.read( new File( 
//		Icon imgIcon = new ImageIcon( grifPath );
//		loadingIcon = new JLabel( imgIcon );
//
//		if( loadingIcon != null )
//			buttonPanel.add( loadingIcon );

        // ok and cancel buttons
        okBtn = scaleFont( new JButton("OK"));
        okBtn.addActionListener(e -> ok());
        buttonPanel.add(okBtn);
		buttonPanel.add( Box.createRigidArea( new Dimension( ( int ) guiScale * 10, 0 ) ) );

		cancelBtn = scaleFont( new JButton( "Cancel" ));
		cancelBtn.addActionListener( e -> cancel() );
		buttonPanel.add( cancelBtn );

        dialog.getContentPane().add(buttonPanel);

        dialog.pack();
        dialog.setVisible(true);

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

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener )
	{
		openContainer( n5Fun, opener, pathFun );
	}

	private void openContainer( final Function< String, N5Reader > n5Fun, final Supplier< String > opener,
			final Function<String,String> pathToRoot )
    {
		final String n5Path = opener.get();
		if( n5Path == null )
			return;

		n5 = n5Fun.apply( n5Path );
		final String rootPath = pathToRoot.apply( n5Path );

		if ( n5 == null )
			return;

        final N5TreeNode n5RootNode;
        try
        {
			n5RootNode = datasetDiscoverer.discoverRecursive( n5, rootPath );
			okBtn.setEnabled( true );
	}
		catch ( final IOException e )
        {
            IJ.handleException(e);
            return;
        }

		if ( containerPathText != null )
			containerPathText.setText( n5Path );

		treeModel.setRoot( n5RootNode );
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
			final String dataset = pathFun.apply( n5Path );
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

	private < T extends Component > T scaleSize( T c )
	{
		Dimension sz = c.getSize();
		c.setSize( ( int ) ( guiScale * sz.width ), ( int ) ( guiScale * sz.height ) );
		return c;
	}

	private < T extends Component > T scale( T c )
	{
		return scaleSize( scaleFont( c ) );
	}

}
