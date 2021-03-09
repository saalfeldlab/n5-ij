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
package org.janelia.saalfeldlab.n5.metadata;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Interface representing a transformation from data to physical coordinates.
 * 
 * @author John Bogovic
 */
public interface PhysicalMetadata
{
	public int numPhysicalDimensions();

	public String physicalUnit();

	/**
	 * Fills the given array with the spacing.
	 * 
	 * @param spacing
	 */
	public void spacing( final double[] spacing );

	/**
	 * Fills the given array with the offset.
	 * 
	 * @param offset
	 */
	public void offset( final double[] offset );

	/**
	 * Allocates and returns an array containing the spacing.
	 * 
	 * @return the spacing
	 */
	public default double[] getSpacing()
	{
		final int n = numPhysicalDimensions();
		final double[] spacing = new double[ n ];
		spacing( spacing );
		return spacing;
	}

	/**
	 * Allocates and returns an array containing the offset.
	 * 
	 * @return the offset
	 */
	public default double[] getOffset()
	{
		final int n = numPhysicalDimensions();
		final double[] offset = new double[ n ];
		offset( offset );
		return offset;
	}
	
	public default AffineGet pixelToPhysical()
	{
		final int n = numPhysicalDimensions();

		final double[] spacing = getSpacing();
		final double[] offset = getOffset();
		if( n == 2 )
		{
		final AffineTransform2D transform = new AffineTransform2D();
			transform.set( 
					spacing[0], 0, offset[0], 
					0, spacing[1], offset[1] );
			return transform;
		}
		else if( n == 3 )
		{
			final AffineTransform3D transform = new AffineTransform3D();
			transform.set( 
					spacing[0], 0, 0, offset[0], 
					0, spacing[1], 0, offset[1],
					0, 0, spacing[2], offset[2]);
			return transform;
		}
		else
		{
			final AffineTransform transform = new AffineTransform( n );
			int spacingIndex = 0;
			int offsetIndex = n-1;
			double[] flatMatrix = new double[ n*(n-1) ];
			for( int i = 0; i < n; i++ )
			{
				flatMatrix[ spacingIndex ] = spacing[ i ];
				flatMatrix[ offsetIndex ] = spacing[ i ];
				spacingIndex += (n+1);
				offsetIndex += n;
			}

			return transform;
		}
	}

}
