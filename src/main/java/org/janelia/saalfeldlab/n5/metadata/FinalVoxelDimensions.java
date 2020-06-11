package org.janelia.saalfeldlab.n5.metadata;

public class FinalVoxelDimensions
{
	private final String unit;

	private final double[] dimensions;

	public FinalVoxelDimensions( final String unit, final double... dimensions )
	{
		this.unit = unit;
		this.dimensions = dimensions.clone();
	}

	public int numDimensions()
	{
		return dimensions.length;
	}

	public String unit()
	{
		return unit;
	}

	public void dimensions( final double[] dims )
	{
		for ( int d = 0; d < dims.length; ++d )
			dims[ d ] = this.dimensions[ d ];
	}

	public double dimension( final int d )
	{
		return dimensions[ d ];
	}
}
