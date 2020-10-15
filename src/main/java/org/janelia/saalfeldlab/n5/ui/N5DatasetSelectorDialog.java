package org.janelia.saalfeldlab.n5.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.saalfeldlab.n5.N5Reader;

public class N5DatasetSelectorDialog
{
	public final N5Reader n5;

	public final String root;

	public BiPredicate< N5Reader, String > isMultiscale;
	
	public List< String > selectedDatasets;
	
	public List<ActionListener> listenerList;
	
	public static final String sep = File.separator;

	public N5DatasetSelectorDialog( final N5Reader n5, final String root )
	{
		this.n5 = n5;
		this.root = root;
		listenerList = new ArrayList<>();
	}

	public N5DatasetSelectorDialog( final N5Reader n5 )
	{
		this( n5, "" );
	}

	public JFrame showAsTree()
	{

		DefaultMutableTreeNode root;
		try
		{
			root = datasetTree();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}

		JFrame frame = new JFrame( "Choose N5 datasets" );
		JPanel panel = new JPanel( new BorderLayout() );

		JTree tree = new JTree( root );
		JScrollPane treeView = new JScrollPane( tree );
		panel.add( treeView, BorderLayout.CENTER );
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				selectedDatasets = new ArrayList<>();
				TreePath[] selectedPaths = tree.getSelectionPaths();	
				for( TreePath path : selectedPaths )
				{
					StringBuffer pathString = new StringBuffer();
					for( Object o : path.getPath())
					{
						pathString.append( "/");
						pathString.append( o.toString() );
					}
					selectedDatasets.add( pathString.toString() );
				}

				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add( okButton, BorderLayout.WEST ); 
		buttonPanel.add( cancelButton , BorderLayout.EAST );
		panel.add( buttonPanel, BorderLayout.SOUTH );
		
        frame.add( panel );
        frame.pack();
        frame.setVisible( true );
        return frame;
	}

	public List<String> getSelectedDatasets()
	{
		return selectedDatasets;
	}

	public DefaultMutableTreeNode datasetTree() throws IOException
	{
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode( root );
		datasetTreeRecursive( rootNode, root, n5.list( root ) );
		return rootNode;
	}

	private void datasetTreeRecursive( final DefaultMutableTreeNode baseNode, final String path, final String... bases ) throws IOException
	{
		for( final String s : bases )
		{
			String fullPath;
			if( path.equals( sep ))
				fullPath = path + s;
			else
				fullPath = path + sep + s;

			if( n5.exists( fullPath ))
			{
				if( n5.datasetExists( fullPath ))
				{
					DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( s );
					baseNode.add( childNode );
				}
				else
				{
					String suffix = "";
					if( isMultiscale != null && isMultiscale.test( n5, fullPath ))
					{
						suffix = " (multiscale)";
					}
					DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( s + suffix );

					baseNode.add( childNode );
					String[] children = n5.list( fullPath );
					datasetTreeRecursive( childNode, fullPath, children );
				}
			}
		}
	}

	public JFrame show()
	{

		List<String> list;
		try
		{
			list = datasetList();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
			return null;
		}

		JFrame frame = new JFrame( "Choose N5 datasets" );
		JPanel panel = new JPanel( new BorderLayout() );
		
		String[] columnNames = new String[]{ "datasets" };
		String[][] data = new String[ list.size() ][];
		for( int i = 0; i < list.size(); i++ )
		{
			data[ i ] = new String[]{ list.get( i ) };
		}

		JTable table = new JTable( data, columnNames );
		JScrollPane treeView = new JScrollPane( table );
		panel.add( treeView, BorderLayout.CENTER );
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				int[] selected = table.getSelectedRows();
				if( selected.length < 1 )
					return;
				else
				{
					selectedDatasets = new ArrayList<>();
					for( int i : selected )
					{
						selectedDatasets.add( data[ i ][ 0 ] );
					}
				}

				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add( okButton, BorderLayout.WEST ); 
		buttonPanel.add( cancelButton , BorderLayout.EAST );
		panel.add( buttonPanel, BorderLayout.SOUTH );
		
        frame.add( panel );
        frame.pack();
        frame.setVisible( true );
        return frame;
	}

	public List<String> datasetList() throws IOException
	{
		ArrayList<String> list = new ArrayList<>();
		datasetListRecursive( list, root, n5.list( root ) );
		return list;
	}

	private void datasetListRecursive( final List< String > list, final String path, final String... bases ) throws IOException
	{
		for( final String s : bases )
		{
			String fullPath;
			if( path.equals( sep ))
				fullPath = path + s;
			else
				fullPath = path + sep + s;

			if( n5.exists( fullPath ))
			{
				if( n5.datasetExists( fullPath ))
					list.add( fullPath );
				else
				{
					if( isMultiscale != null && isMultiscale.test( n5, fullPath ))
						list.add( fullPath + " (multiscale)" );

					String[] children = n5.list( fullPath );
					datasetListRecursive( list, fullPath, children );
				}
			}
		}
	}

}
