package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;

import java.io.IOException;
import java.util.Optional;

/**
 * The {@link N5MetadataParser} for {@link N5CosemMetadata}.
 * 
 * @see <a href="https://www.janelia.org/project-team/cosem">https://www.janelia.org/project-team/cosem</a>
 * 
 * @author John Bogovic
 *
 */
public class N5CosemMetadataParser implements N5MetadataParser<N5CosemMetadata>, N5MetadataWriter<N5CosemMetadata> {

  @Override public Optional<N5CosemMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

	try {
	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());

	  if (attributes == null)
		return Optional.empty();

	  final String path = node.getPath();
	  final Optional<N5CosemMetadata.CosemTransform> cosemTransform = Optional.ofNullable(n5.getAttribute(
			  node.getPath(), N5CosemMetadata.CosemTransform.KEY, N5CosemMetadata.CosemTransform.class));

	  return cosemTransform.map(t -> new N5CosemMetadata(path, t, attributes));
	} catch (IOException e) {
	  return Optional.empty();
	}
  }

  @Override
  public void writeMetadata(final N5CosemMetadata t, final N5Writer n5, final String group) throws Exception {

	if (t.getCosemTransform() != null)
	  n5.setAttribute(group, N5CosemMetadata.CosemTransform.KEY, t.getCosemTransform());
  }
}
