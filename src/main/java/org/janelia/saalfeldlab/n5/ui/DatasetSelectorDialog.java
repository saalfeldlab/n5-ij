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
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MultiScaleMetadata;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
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
    private JTextField containerPathTxt;

//	private List< JSpinner > minSpinners;
//	private List< JSpinner > maxSpinners;

	private JCheckBox virtualBox;

	private JCheckBox cropBox;

    private JTree containerTree;

    private JButton okBtn;

    private JButton cancelBtn;

    private DefaultTreeModel treeModel;

	private String lastBrowsePath;

	private Function< String, N5Reader > n5Fun;

	private Function< String, String > pathFun;

	private N5Reader n5;

	private N5TreeNode selectedNode;

	private boolean virtualOption = false;

	private boolean cropOption = false;

	private double guiScale;

//	private boolean minMaxOption = false;

//	private int nd;

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

//	public void setMinMaxOption( boolean arg )
//	{
//		minMaxOption = arg;
//	}

	public boolean isVirtual()
	{
		return ( virtualBox != null ) && virtualBox.isSelected();
	}

//	public Interval getMinMax()
//	{
//		if( !minMaxOption || nd < 1 )
//			return null;
//
//		long[] min = new long[ nd ];
//		long[] max = new long[ nd ];
//		for ( int i = 0; i < nd; i++ )
//		{
//			if( i < 3 ) // limiting to 3d cropping for now
//			{
//				min[ i ] = ( Integer ) minSpinners.get( i ).getValue();
//				max[ i ] = ( Integer ) maxSpinners.get( i ).getValue();
//			}
//		}
//		return new FinalInterval( min, max );
//	}

    public void run( final Consumer< DataSelection > okCallback )
    {
        this.okCallback = okCallback;

        dialog = new JFrame("N5 Viewer");
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.PAGE_AXIS));
		dialog.setMinimumSize(
				new Dimension( ( int ) guiScale * 600, ( int ) guiScale * 400 ) );
        dialog.setPreferredSize(dialog.getMinimumSize());

        if( n5 == null )
        {
			final JPanel containerPanel = new JPanel();
			containerPanel.add( scale( new JLabel("N5 container:" )));

			containerPathTxt = new JTextField();
			System.out.println( containerPathTxt.getPreferredSize().height );
			containerPathTxt.setPreferredSize( 
					new Dimension( 
							( int ) ( guiScale * 200 ),
							( int ) ( guiScale * containerPathTxt.getPreferredSize().height ) ) );
			scale( containerPathTxt );
			containerPanel.add(containerPathTxt);

			final JButton browseBtn = scaleFont( new JButton("Browse..."));
			browseBtn.addActionListener(e -> openContainer( n5Fun, this::openBrowseDialog));
			containerPanel.add(browseBtn);

			final JButton detectBtn = scaleFont( new JButton("Detect datasets"));
			detectBtn.addActionListener( e -> openContainer( 
					n5Fun,
					() -> containerPathTxt.getText(),
					pathFun ));
			containerPanel.add( detectBtn );

			dialog.getContentPane().add( containerPanel );

//			if( minMaxOption )
//			{
//				// add min/max options
//				final JPanel minMaxPanel = new JPanel();
//				minMaxPanel.setLayout( new GridLayout( 2, 4 ));
//
//				minSpinners = new ArrayList<>();
//				maxSpinners = new ArrayList<>();
//				for( int i = 0; i < 3; i++ )
//				{
//					minSpinners.add( new JSpinner() );
//					maxSpinners.add( new JSpinner() );
//				}
//				defaultMinMax();
//
//				minMaxPanel.add( new JLabel( "Min: "));
//				minSpinners.stream().forEach( x -> minMaxPanel.add( x ) );
//				minMaxPanel.add( new JLabel( "Max: "));
//				maxSpinners.stream().forEach( x -> minMaxPanel.add( x ) );
//
//				dialog.add( minMaxPanel );
//			}
        }


		final JPanel containerTreePanel = new JPanel();
		containerTreePanel.setBorder( BorderFactory.createTitledBorder( "N5 dataset tree" ) );
//		containerTreePanel.setLayout( new BoxLayout( containerTreePanel, BoxLayout.Y_AXIS ) );
		containerTreePanel.setLayout( new GridBagLayout() );
		treeModel = new DefaultTreeModel( null );
		containerTree = new JTree( treeModel );
		containerTree.setEnabled( false );

        // TODO disable selection of nodes that are not datasets
		containerTree.getSelectionModel().setSelectionMode( TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION );
        // By default leaf nodes (datasets) are displayed as files. This changes the default behavior to display them as folders
        final DefaultTreeCellRenderer treeCellRenderer = (DefaultTreeCellRenderer) containerTree.getCellRenderer();
        treeCellRenderer.setLeafIcon(treeCellRenderer.getOpenIcon());

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

        // ok and cancel buttons
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.LINE_AXIS ) );
		buttonPanel.setBorder( BorderFactory.createEmptyBorder( 0,  (int) guiScale * 10,  (int) guiScale * 10, 10 ) );
		buttonPanel.add( Box.createHorizontalGlue() );

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

        containerTree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) containerTree.getLastSelectedPathComponent();
            selectedNode = (node == null ? null : (N5TreeNode) node.getUserObject());
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
			n5RootNode = datasetDiscoverer.discoverRecursive( n5, rootPath );
			if( n5RootNode.isDataset() )
				okBtn.setEnabled( true );
        }
		catch ( final IOException e )
        {
            IJ.handleException(e);
            return;
        }

		if ( containerPathTxt != null )
			containerPathTxt.setText( n5Path );

		treeModel.setRoot( n5RootNode );
		containerTree.setEnabled( true );

		if ( n5RootNode.isDataset() )
			okBtn.setEnabled( true );
//		else
//			okBtn.setEnabled( false );
    }

//	private void defaultMinMax()
//	{
//		if( !minMaxOption )
//			return;
//
//		for ( int i = 0; i < 3; i++ )
//		{
//			minSpinners.get( i ).setModel( new SpinnerListModel( new String[] { "-" } ) );
//			maxSpinners.get( i ).setModel( new SpinnerListModel( new String[] { "-" } ) );
//		}
//	}

//	private void updateMinMax( final N5TreeNode node )
//	{
//		if( !minMaxOption )
//			return;
//
//        // add min/max options
//		try
//		{ 
//			if ( !n5.datasetExists( node.path ) )
//			{
//				return;
//			}
//			DatasetAttributes attr = n5.getDatasetAttributes( node.path );
//			nd = attr.getNumDimensions();
//			for( int i = 0; i < nd; i++ )
//			{
//				int max = (int)attr.getDimensions()[ i ] - 1;
//				minSpinners.get( i ).setModel( new SpinnerNumberModel( 0, 0, max, 1 ));
//				maxSpinners.get( i ).setModel( new SpinnerNumberModel( max, 0, max, 1 ));
//			}
//		}
//		catch ( Exception e )
//		{
//			return;
//		}
//	}

    private void ok()
    {
		// TODO FIX
		final ArrayList< N5Metadata > selectedMetadata = new ArrayList<>();

		// TODO what condition to check here to skip explicit selection?
//		if ( containerTree.getSelectionCount() == 0 )
		if ( false )
		{
//			final String n5Path = containerPathTxt.getText();
//			n5 = n5Fun.apply( n5Path );
//			final String dataset = pathFun.apply( n5Path );
//			N5TreeNode node = null;
//			try
//			{
//				node = datasetDiscoverer.parse( n5, dataset );
//				if ( node.isDataset && node.metadata != null )
//					selectedMetadata.add( node.metadata );
//			}
//			catch ( Exception e ){}
//
//			if ( node == null || !node.isDataset || node.metadata == null )
//			{
//				JOptionPane.showMessageDialog( null, "Could not find a dataset / metadata at the provided path." );
//				return;
//			}
		}
		else
		{
			TreePath[] selectedPaths = containerTree.getSelectionPaths();
			for( TreePath path : containerTree.getSelectionPaths() )
				selectedMetadata.add( ((N5TreeNode)path.getLastPathComponent()).getMetadata() );
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
