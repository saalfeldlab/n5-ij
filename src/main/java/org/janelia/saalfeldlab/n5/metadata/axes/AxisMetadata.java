package org.janelia.saalfeldlab.n5.metadata.axes;

/**
 * Metadata that labels and assigns types to axes. 
 *
 * @author John Bogovic
 *
 */
public interface AxisMetadata {

	public String[] getAxisLabels();

	public String[] getAxisTypes();

	public String[] getUnits();

}
