package org.janelia.saalfeldlab.n5.ui;

import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;

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

		return  (resolutionField != null && !resolutionField.getText().isEmpty() ) ||
				(offsetField != null && !offsetField.getText().isEmpty()) ||
				(downsamplingFactorsField != null && !downsamplingFactorsField.getText().isEmpty()) ||
				(minIntensityField != null && !minIntensityField.getText().isEmpty()) ||
				(maxIntensityField != null && !maxIntensityField.getText().isEmpty());
	}

	public JPanel buildPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(  new JLabel( "Resolution key"));
		resolutionField = new JTextField();
		panel.add( resolutionField );

		panel.add(  new JLabel( "Offset key"));
		offsetField = new JTextField();
		panel.add( offsetField );

		panel.add(  new JLabel( "Unit key"));
		unitField = new JTextField();
		panel.add( unitField );

		panel.add( new JLabel( "Downsampling factors key"));
		downsamplingFactorsField = new JTextField();
		panel.add( downsamplingFactorsField );

		panel.add(  new JLabel( "min intensity key"));
		minIntensityField = new JTextField();
		panel.add( minIntensityField );

		panel.add(  new JLabel( "max intensity key"));
		maxIntensityField = new JTextField();
		panel.add( maxIntensityField );

        return panel;
	}

}
