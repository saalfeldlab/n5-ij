package org.janelia.saalfeldlab.n5.metadata;

import java.util.Arrays;

public class N5MultiscaleDatasetTemplate
{
	public String dataset;
	public double[] downsamplingFactors;
	
	public N5MultiscaleDatasetTemplate( final String dataset, final double[] downsamplingFactors )
	{
		this.dataset = dataset;
		this.downsamplingFactors = downsamplingFactors;
	}
	
	public String toString()
	{
		return String.format( "multiscale %s : %s", dataset, Arrays.toString( downsamplingFactors ));
	}
}