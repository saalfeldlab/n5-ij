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
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.TranslatedTreeMetadataParser;

import com.google.gson.Gson;

public class N5MetadataTranslationPanel {

	private float fontScale = 1.0f;
	
	private JTextArea textArea;

	private Predicate<CanonicalMetadata> filter;

	public N5MetadataTranslationPanel() {
	}

	public N5MetadataTranslationPanel(final float fontScale) {
		this.fontScale = fontScale;
	}

	public void setFilter(Predicate<CanonicalMetadata> filter) {
		this.filter = filter;
	}

//	public SpatialMetadataTemplateParser getParser( final Gson gson ) {
//		return new SpatialMetadataTemplateParser( gson, textArea.getText() );
//	}
//
//	/**
//	 * Returns an optional containing the parser if any fields are non empty.
//	 * 
//	 * @return the parser optional
//	 */
//	public Optional<SpatialMetadataTemplateParser> getParserOptional( final Gson gson ) {
//
//		if (isTranslationProvided())
//			return Optional.of(getParser( gson ));
//		else
//			return Optional.empty();
//	}

	public TranslatedTreeMetadataParser getParser(final N5Reader n5) {
		if (filter == null)
			return new TranslatedTreeMetadataParser(n5, textArea.getText());
		else
			return new TranslatedTreeMetadataParser(n5, textArea.getText(), filter);
	}

	/**
	 * Returns an optional containing the parser if any fields are non empty.
	 * 
	 * @return the parser optional
	 */
	public Optional<TranslatedTreeMetadataParser> getParserOptional(final N5Reader n5) {

		if (isTranslationProvided())
			return Optional.of(getParser(n5));
		else
			return Optional.empty();
	}

	public boolean isTranslationProvided() {

		return !textArea.getText().isEmpty();
	}

	public JPanel buildPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		panel.add( new JLabel("Translation specification"));

		textArea = new JTextArea();
		textArea.setFont( textArea.getFont().deriveFont( (float)fontScale * 18f) );

		final JScrollPane textView = new JScrollPane( textArea );
		panel.add( textView, BorderLayout.CENTER );

        return panel;
	}

}
