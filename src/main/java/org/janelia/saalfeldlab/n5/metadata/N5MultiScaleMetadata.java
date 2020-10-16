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

import net.imglib2.realtransform.AffineTransform3D;

import java.util.Objects;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

public class N5MultiScaleMetadata implements N5Metadata {

    public final String basePath;

    public final String[] paths;

    public final AffineTransform3D[] transforms;

    public N5MultiScaleMetadata( final String basePath, final String[] paths, final AffineTransform3D[] transforms)
    {
        Objects.requireNonNull(paths);
        Objects.requireNonNull(transforms);
        for (final String path : paths)
            Objects.requireNonNull(path);
        for (final AffineTransform3D transform : transforms)
            Objects.requireNonNull(transform);

        this.basePath = basePath;
        this.paths = paths;
        this.transforms = transforms;
    }

	@Override
	public String getPath()
	{
		return basePath;
	}

	@Override
	public DatasetAttributes getAttributes()
	{
		return null;
	}
	
}
