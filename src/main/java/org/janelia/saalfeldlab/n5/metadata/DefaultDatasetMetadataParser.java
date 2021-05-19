package org.janelia.saalfeldlab.n5.metadata;

//public class DefaultDatasetMetadataParser extends AbstractN5DatasetMetadataParser<DefaultDatasetMetadata> {
//
//  @Override
//  public Optional<DefaultDatasetMetadata> parseMetadata(final Map<String, Object> metaMap) {
//
//	if (!check(metaMap))
//	  return Optional.empty();
//
//	final String dataset = (String)metaMap.get("dataset");
//
//	final DatasetAttributes attributes = N5MetadataParser.parseAttributes(metaMap);
//	if (attributes == null)
//	  return Optional.empty();
//
//	return Optional.of(new DefaultDatasetMetadata(dataset, attributes));
//  }
//}
