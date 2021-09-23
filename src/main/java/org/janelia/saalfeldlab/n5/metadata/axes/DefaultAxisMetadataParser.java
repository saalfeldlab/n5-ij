package org.janelia.saalfeldlab.n5.metadata.axes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

public class DefaultAxisMetadataParser implements N5MetadataParser<DefaultAxisMetadata> {

	@Override
	public Optional<DefaultAxisMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		try {
			final String path = node.getPath();
			final String[] axisLabels = n5.getAttribute(path, "axes", String[].class);
			if( axisLabels == null )
				return Optional.empty();

			final String[] types = Arrays.stream( axisLabels ).map(
					l -> AxisUtils.defaultAxisTypes.get(l))
					.toArray( String[]::new );

			return Optional.of( new DefaultAxisMetadata(path, axisLabels, types));
		} catch (IOException e) {
			return Optional.empty();
		}
	}
}