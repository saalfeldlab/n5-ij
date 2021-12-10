package org.janelia.saalfeldlab.n5.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;

public class TranslationResultPanel {

	private float fontScale = 1.0f;

	private JTextArea original;

	private JTextArea translated;

	private static final int OUTER_PAD = 8;
	private static final int BUTTON_PAD = 3;
	private static final int MID_PAD = 5;
	
	private final ObjectMapper objMapper;

	private final DefaultPrettyPrinter prettyPrinter;
	
	public TranslationResultPanel()
	{
		prettyPrinter = new DefaultPrettyPrinter();
		DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
		prettyPrinter.indentArraysWith(i);
		prettyPrinter.indentObjectsWith(i);

		objMapper = new ObjectMapper();
		objMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objMapper.setDefaultPrettyPrinter(prettyPrinter);
	}
	
	public JTextArea getOriginal() {

		return original;
	}

	public JTextArea getTranslated() {

		return translated;
	}
	
	private String prettyJson( final Gson gson, final Object obj )
	{
		String jsonTxt = gson.toJson( obj );
		String jsonPretty = jsonTxt;
		try {
			HashMap<?,?> tmpObj = objMapper.readValue( jsonTxt, HashMap.class );
			jsonPretty = objMapper.writer(prettyPrinter).writeValueAsString(tmpObj);
		} catch (JsonMappingException e) { } catch (JsonProcessingException e) { }
		return jsonPretty;
	}

	public void set( final Gson gson, final Object orig, final Object xlated ) {
		original.setText( prettyJson( gson, orig ));
		translated.setText( prettyJson( gson, xlated ));
	}

	public JPanel buildPanel() {

		original = new JTextArea();
		original.setFont(original.getFont().deriveFont((float) fontScale * 18f));
		original.setText("");

		translated = new JTextArea();
		translated.setFont(translated.getFont().deriveFont((float) fontScale * 18f));
		translated.setText("");

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 0.5;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(OUTER_PAD, OUTER_PAD, MID_PAD, BUTTON_PAD);

		gbc.anchor = GridBagConstraints.LINE_START;
		panel.add( new JLabel("Original"), gbc );

		gbc.gridy = 1;
		gbc.weighty = 1.0;
		gbc.gridheight = 2;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(new JScrollPane(original), gbc);

		gbc.gridx = 1;
		gbc.gridheight = 1;
		gbc.gridy = 0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.LINE_START;
		panel.add( new JLabel("Translated"), gbc );

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weighty = 1.0;
		panel.add(new JScrollPane(translated), gbc);

		return panel;
	}

}
