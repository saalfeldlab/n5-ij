package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.N5TreeNode;

public interface N5GroupParser<T extends N5Metadata>
{
	
	public T parseMetadataGroup( N5TreeNode node );

}
