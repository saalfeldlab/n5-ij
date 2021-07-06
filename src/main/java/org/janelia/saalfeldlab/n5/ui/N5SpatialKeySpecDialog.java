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

import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.saalfeldlab.n5.metadata.N5GenericSingleScaleMetadataParser;

public class N5SpatialKeySpecDialog {

	private JTextField resolutionField;
	private JTextField offsetField;
	private JTextField unitField;
	private JTextField downsamplingFactorsField;
	private JTextField minIntensityField;
	private JTextField maxIntensityField;

	public N5SpatialKeySpecDialog() {}

	public N5GenericSingleScaleMetadataParser getParser() {

		return new N5GenericSingleScaleMetadataParser(
				minIntensityField.getText(), maxIntensityField.getText(),
				resolutionField.getText(), offsetField.getText(), unitField.getText(),
				downsamplingFactorsField.getText());
	}

	/**
	 * Returns an optional containing the parser if any fields are non empty.
	 * 
	 * @return the parser optional
	 */
	public Optional<N5GenericSingleScaleMetadataParser> getParserOptional() {

		if (anyNonEmptyFields())
			return Optional.of(getParser());
		else
			return Optional.empty();
	}

	public boolean anyNonEmptyFields() {

		return !resolutionField.getText().isEmpty() ||
				!offsetField.getText().isEmpty() ||
				!downsamplingFactorsField.getText().isEmpty() ||
				!minIntensityField.getText().isEmpty() ||
				!maxIntensityField.getText().isEmpty();
	}

	public JPanel buildPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(  new JLabel( "Resolution key"));
		resolutionField = new JTextField("resolution");
		panel.add( resolutionField );

		panel.add(  new JLabel( "Offset key"));
		offsetField = new JTextField();
		panel.add( offsetField );

		panel.add(  new JLabel( "Unit key"));
		unitField = new JTextField();
		panel.add( unitField );

		panel.add(  new JLabel( "Downsampling factors key"));
		downsamplingFactorsField = new JTextField();
		downsamplingFactorsField = new JTextField("downsamplingFactors");
		panel.add( downsamplingFactorsField );

		panel.add(  new JLabel( "min intensity key"));
		minIntensityField = new JTextField("");
		panel.add( minIntensityField );

		panel.add(  new JLabel( "max intensity key"));
		maxIntensityField = new JTextField("");
		panel.add( maxIntensityField );

        return panel;
	}

}
