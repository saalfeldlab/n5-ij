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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;

import ij.Prefs;

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
		final JFrame frame = new JFrame( "Metadata translation" );

		final double guiScale = Prefs.getGuiScale();
		final int frameSizeX = (int)(guiScale * 600);
		final int frameSizeY = (int)(guiScale * 400);

		frame.setPreferredSize( new Dimension( frameSizeX, frameSizeY ) );
		frame.setMinimumSize( frame.getPreferredSize() );

		final JPanel panel = new JPanel( new BorderLayout() );

		if( listener != null )
			frame.addWindowListener( listener );

		final JTextArea textArea = new JTextArea();
		textArea.setText( init );
		textArea.setFont( textArea.getFont().deriveFont( (float)guiScale * 18f) );

		final JScrollPane textView = new JScrollPane( textArea );
		panel.add( textView, BorderLayout.CENTER );

		final JButton okButton = new JButton("OK");
		okButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				metadataSpecText = textArea.getText();
				mapper = new MetadataTemplateMapper( metadataSpecText );

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

	public static void main( final String[] args ) throws IOException
	{
//		System.out.println( MetadataTemplateMapper.COSEM_MAPPER);
//
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
