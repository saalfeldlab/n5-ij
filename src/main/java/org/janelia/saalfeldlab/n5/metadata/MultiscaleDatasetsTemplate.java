package org.janelia.saalfeldlab.n5.metadata;

import java.util.Map;

public class MultiscaleDatasetsTemplate
{
	public Map< String, MultiscaleDataset[] > multiscaleDatasets;

	public MultiscaleDatasetsTemplate( final Map< String, MultiscaleDataset[] > ms )
	{
		this.multiscaleDatasets = ms;
	}

	public static class MultiscaleDataset
	{
		public String dataset;
		public double[] downsamplingFactors;
		
		public MultiscaleDataset( final String dataset, final double[] downsamplingFactors )
		{
			this.dataset = dataset;
			this.downsamplingFactors = downsamplingFactors;
		}
	}
}
