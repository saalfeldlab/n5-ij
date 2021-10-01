package org.janelia.saalfeldlab.n5.metadata.axes;

public class Axis {

	private final String type;

	private final String label;

	private final String unit;

	public Axis( final String type, final String label, final String unit )
	{
		this.type = type;
		this.label = label;
		this.unit = unit;
	}

	public String getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public String getUnit() {
		return unit;
	}
}
