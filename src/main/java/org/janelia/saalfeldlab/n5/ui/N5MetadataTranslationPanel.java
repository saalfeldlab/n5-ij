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
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Reader;

import com.google.gson.Gson;

public class N5MetadataTranslationPanel {

	private static final String DEFAULT_TEXT = "include \"n5\";";

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

	public TranslatedN5Reader getTranslatedN5( final N5Reader n5, final Gson gson ) {
		
		final TranslatedN5Reader translatedN5 = new TranslatedN5Reader(n5, gson, textArea.getText(), ".");
		if( translatedN5.getTranslation().getTranslationFunction().isValid())
			return translatedN5;
		else 
			return null;
	}

	/**
	 * Returns an optional containing the parser if any fields are non empty.
	 * 
	 * @param n5 the {@link N5Reader}
	 * @param gson the {@link Gson}
	 * @return the parser optional
	 */
	public Optional<TranslatedN5Reader> getTranslatedN5Optional( final N5Reader n5, final Gson gson ) {

		if (isTranslationProvided())
			return Optional.ofNullable(getTranslatedN5( n5, gson ));
		else
			return Optional.empty();
	}

	public boolean isTranslationProvided() {

		if( textArea == null )
			return false;

		final String txt = textArea.getText();
		final boolean textSet = !( txt.isEmpty() || txt.equals(DEFAULT_TEXT));
		return textSet;
//		if( textSet ) {
//			return TranslatedTreeMetadataParser.testTranslation( txt );
//		}
//		else
//			return false;
	}

	public JPanel buildPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		panel.add( new JLabel("Translation specification"));

		textArea = new JTextArea();
		textArea.setFont(textArea.getFont().deriveFont((float) fontScale * 18f));
		textArea.setText(DEFAULT_TEXT);

		final JScrollPane textView = new JScrollPane( textArea );
		panel.add( textView, BorderLayout.CENTER );

        return panel;
	}

}
