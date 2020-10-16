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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5TreeNode;

public class N5CosemMultiScaleMetadata implements N5Metadata, N5GroupParser< N5CosemMultiScaleMetadata >{

    public final String basePath;

    public final String[] paths;

    public final AffineTransform3D[] transforms;
    
    private static final Predicate<String> scaleLevelPredicate = Pattern.compile("^s\\d+$").asPredicate();

    public N5CosemMultiScaleMetadata( )
    {
        this.basePath = null;
        this.paths = null;
        this.transforms = null;
    }

    public N5CosemMultiScaleMetadata( final String basePath, final String[] paths, final AffineTransform3D[] transforms)
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

	/**
	*
    * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
    * while discovering the N5 tree and filling the metadata for datasets or groups.
    *
    * @param node
    * @return
    */
	public N5CosemMultiScaleMetadata parseMetadataGroup( final N5TreeNode node )
	{
		final Map< String, N5TreeNode > scaleLevelNodes = new HashMap<>();
		for ( final N5TreeNode childNode : node.childrenList() )
		{
			if ( scaleLevelPredicate.test( childNode.getNodeName() ) &&
				 childNode.isDataset() &&
				 childNode.getMetadata() instanceof N5CosemMetadata )
			{
				scaleLevelNodes.put( childNode.getNodeName(), childNode );
			}
		}

		if ( scaleLevelNodes.isEmpty() )
			return null;
		
		List<AffineTransform3D> transforms = new ArrayList<>();
		List<String> paths = new ArrayList<>();
		scaleLevelNodes.forEach( (k,v) -> {
			paths.add( v.path );
			transforms.add( ((N5CosemMetadata)v.getMetadata() ).getTransform().toAffineTransform3d() );
		});

		return new N5CosemMultiScaleMetadata( 
				node.path, 
				paths.toArray( new String[ 0 ] ),
				transforms.toArray( new AffineTransform3D[ 0 ] ) );
	}
	
}
