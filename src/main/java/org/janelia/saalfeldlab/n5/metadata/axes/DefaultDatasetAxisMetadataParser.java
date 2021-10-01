package org.janelia.saalfeldlab.n5.metadata.axes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

public class DefaultDatasetAxisMetadataParser implements N5MetadataParser<DefaultDatasetAxisMetadata> {

	@Override
	public Optional<DefaultDatasetAxisMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		try {
			final String path = node.getPath();
			DatasetAttributes attrs = n5.getDatasetAttributes(path);
			if( attrs == null )
				return Optional.empty();

			final String[] axisLabels = n5.getAttribute(path, "axes", String[].class);
			if( axisLabels == null )
				return Optional.empty();

			final String[] types = Arrays.stream( axisLabels ).map(
					l -> AxisUtils.defaultAxisTypes.get(l))
					.toArray( String[]::new );

			String[] units = n5.getAttribute(path, "units", String[].class);
			if (units == null ) {
				units = new String[ n5.getDatasetAttributes(path).getNumDimensions() ] ;
				Arrays.fill(units, "pixel");
			}

			return Optional.of( new DefaultDatasetAxisMetadata(path, axisLabels, types, units, attrs));
		} catch (IOException e) {
			return Optional.empty();
		}
	}
}