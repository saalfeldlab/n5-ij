package org.janelia.saalfeldlab.n5.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;

public class N5MetadataSpecDialog
{
	private String metadataSpecText;
	
	private String mapperString;

	private MetadataTemplateMapper mapper;

	private WindowListener listener;

	public N5MetadataSpecDialog( ){ } 

	public N5MetadataSpecDialog( final WindowListener listener )
	{
		this.listener = listener;
	} 

	public String getMapperString()
	{
		return mapperString;
	}

	public MetadataTemplateMapper getMapper()
	{
		return mapper;
	}

	public JFrame show( final String init )
	{
		JFrame frame = new JFrame( "Choose N5 datasets" );
		JPanel panel = new JPanel( new BorderLayout() );

		if( listener != null )
			frame.addWindowListener( listener );

		JTextArea textArea = new JTextArea();
		textArea.setText( init );
		textArea.setFont( textArea.getFont().deriveFont(18f) );

		JScrollPane textView = new JScrollPane( textArea );
		panel.add( textView, BorderLayout.CENTER );
		
		JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent event )
			{
				metadataSpecText = textArea.getText();
				mapper = new MetadataTemplateMapper( metadataSpecText );

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
	
	public static void main( String[] args ) throws IOException
	{
		System.out.println( MetadataTemplateMapper.COSEM_MAPPER);

//		N5MetadataSpecDialog dialog = new N5MetadataSpecDialog( );
//
//		String templateJsonF = "/home/john/dev/json/jq_Examples/template.json";
//		String content = new String(Files.readAllBytes(Paths.get( templateJsonF )));
//
//		dialog.show( MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );
//				String result;
//				try
//				{
//					mapperString = mapper.compute( metadataSpecText, content );
//					System.out.println( "result: " );
//					System.out.println( result );
//				}
//				catch ( IOException e )
//				{
//					e.printStackTrace();
//				}
	}
}
