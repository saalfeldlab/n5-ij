/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.metadata;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import org.janelia.saalfeldlab.n5.N5Writer;

import java.io.IOException;

public class N5ViewerMetadataWriter implements N5MetadataWriter< N5SingleScaleMetadata >, ImageplusMetadata< N5SingleScaleMetadata >
{

	@Override
	public void writeMetadata( N5SingleScaleMetadata t, N5Writer n5, String dataset ) throws Exception
	{
		double[] pixelResolution = new double[]{
				t.transform.get( 0, 0 ),
				t.transform.get( 1, 1 ),
				t.transform.get( 2, 2 ) };

		FinalVoxelDimensions voxdims = new FinalVoxelDimensions( t.unit, pixelResolution );
		n5.setAttribute( dataset, N5ViewerSingleMetadataParser.PIXEL_RESOLUTION_KEY, voxdims );
	}

	@Override
	public void writeMetadata( N5SingleScaleMetadata t, ImagePlus ip ) throws IOException
	{
		ip.getCalibration().pixelWidth = t.transform.get( 0, 0 );
		ip.getCalibration().pixelHeight = t.transform.get( 1, 1 );
		ip.getCalibration().pixelDepth = t.transform.get( 2, 2 );
		ip.getCalibration().setUnit( t.unit );
		ip.setDimensions( 1, ip.getStackSize(), 1 );
	}

	@Override
	public N5SingleScaleMetadata readMetadata( ImagePlus ip ) throws IOException
	{
		double sx = ip.getCalibration().pixelWidth;
		double sy = ip.getCalibration().pixelHeight;
		double sz = ip.getCalibration().pixelDepth;
		String unit = ip.getCalibration().getUnit();

		AffineTransform3D xfm = new AffineTransform3D();
		xfm.set( sx, 0.0, 0.0, 0.0, 0.0, sy, 0.0, 0.0, 0.0, 0.0, sz, 0.0 );

		return new N5SingleScaleMetadata( "", xfm, unit );
	}

}
