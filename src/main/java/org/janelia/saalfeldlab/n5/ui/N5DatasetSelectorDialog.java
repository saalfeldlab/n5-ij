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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.saalfeldlab.n5.N5Reader;

@Deprecated
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
		catch ( final IOException e )
		{
			e.printStackTrace();
			return null;
		}

		final JFrame frame = new JFrame( "Choose N5 datasets" );
		final JPanel panel = new JPanel( new BorderLayout() );

		final JTree tree = new JTree( root );
		final JScrollPane treeView = new JScrollPane( tree );
		panel.add( treeView, BorderLayout.CENTER );

		final JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				selectedDatasets = new ArrayList<>();
				final TreePath[] selectedPaths = tree.getSelectionPaths();
				for( final TreePath path : selectedPaths )
				{
					final StringBuffer pathString = new StringBuffer();
					for( final Object o : path.getPath())
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

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		final JPanel buttonPanel = new JPanel();
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
		final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode( root );
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
					final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( s );
					baseNode.add( childNode );
				}
				else
				{
					String suffix = "";
					if( isMultiscale != null && isMultiscale.test( n5, fullPath ))
					{
						suffix = " (multiscale)";
					}
					final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode( s + suffix );

					baseNode.add( childNode );
					final String[] children = n5.list( fullPath );
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
		catch ( final IOException e )
		{
			e.printStackTrace();
			return null;
		}

		final JFrame frame = new JFrame( "Choose N5 datasets" );
		final JPanel panel = new JPanel( new BorderLayout() );

		final String[] columnNames = new String[]{ "datasets" };
		final String[][] data = new String[ list.size() ][];
		for( int i = 0; i < list.size(); i++ )
		{
			data[ i ] = new String[]{ list.get( i ) };
		}

		final JTable table = new JTable( data, columnNames );
		final JScrollPane treeView = new JScrollPane( table );
		panel.add( treeView, BorderLayout.CENTER );

		final JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				final int[] selected = table.getSelectedRows();
				if( selected.length < 1 )
					return;
				else
				{
					selectedDatasets = new ArrayList<>();
					for( final int i : selected )
					{
						selectedDatasets.add( data[ i ][ 0 ] );
					}
				}

				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				frame.setVisible( false );
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ));
			}
		});

		final JPanel buttonPanel = new JPanel();
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
		final ArrayList<String> list = new ArrayList<>();
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

					final String[] children = n5.list( fullPath );
					datasetListRecursive( list, fullPath, children );
				}
			}
		}
	}

}
