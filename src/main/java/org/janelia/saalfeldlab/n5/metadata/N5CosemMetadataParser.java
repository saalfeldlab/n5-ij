package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

public class N5CosemMetadataParser implements N5MetadataParser<N5CosemMetadata> {

  @Override
  public Optional<N5CosemMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

	try {
	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());

	  if (attributes == null)
		return Optional.empty();

	  final String path = node.getPath();
	  final Optional<N5CosemMetadata.CosemTransform> cosemTransform = Optional.ofNullable(
			  n5.getAttribute(node.getPath(), N5CosemMetadata.CosemTransform.KEY, N5CosemMetadata.CosemTransform.class));

	  return cosemTransform.map( t -> new N5CosemMetadata(path, t, attributes));
	} catch (IOException e) {
	  return Optional.empty();
	}
  }
}
