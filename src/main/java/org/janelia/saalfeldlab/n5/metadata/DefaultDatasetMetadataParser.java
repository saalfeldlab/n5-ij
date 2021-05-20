package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

// TODO this may not be necessary given N5SingleScaleMetadata and parser defaults
public class DefaultDatasetMetadataParser implements N5MetadataParser<DefaultDatasetMetadata> {

  @Override
  public Optional<DefaultDatasetMetadata> parseMetadata(final N5Reader n5, final N5TreeNode node) {

	try {
	  final DatasetAttributes attributes = n5.getDatasetAttributes(node.getPath());
	  if (attributes == null)
	    return Optional.empty();

	  return Optional.of(new DefaultDatasetMetadata(node.getPath(), attributes));
	} catch( IOException e )
	{
	  return Optional.empty();
	}
  }
}
