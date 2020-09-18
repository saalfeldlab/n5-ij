package org.janelia.saalfeldlab.n5.metadata;

public class N5DatasetTemplate
{
	public String dataset;
	public String attributes;

	public N5DatasetTemplate( 
			final String dataset,
			final String attributes )
	{
		this.dataset = dataset;
		this.attributes = attributes;
	}
}