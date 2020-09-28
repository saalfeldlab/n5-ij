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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5TreeNode;

import net.imglib2.realtransform.AffineTransform3D;

public class N5ViewerMultiscaleMetadataParser implements N5GroupParser< N5MultiScaleMetadata >
{
    private static final Predicate<String> scaleLevelPredicate = Pattern.compile("^s\\d+$").asPredicate();
    
    /**
     * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
     * while discovering the N5 tree and filling the metadata for datasets or groups.
     *
     * @param node
     * @return
     */
	public N5MultiScaleMetadata parseMetadataGroup( final N5TreeNode node )
	{
		final Map< String, N5TreeNode > scaleLevelNodes = new HashMap<>();
		for ( final N5TreeNode childNode : node.childrenList() )
		{
			System.out.println( childNode.getNodeName());
			if ( scaleLevelPredicate.test( childNode.getNodeName() ) &&
				 childNode.isDataset() &&
				 childNode.getMetadata() instanceof N5SingleScaleMetadata )
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
			transforms.add( ((N5SingleScaleMetadata)v.getMetadata()).transform );
		});

		return new N5MultiScaleMetadata( 
				node.path, 
				paths.toArray( new String[ 0 ] ),
				transforms.toArray( new AffineTransform3D[ 0 ] ) );
	}

}
