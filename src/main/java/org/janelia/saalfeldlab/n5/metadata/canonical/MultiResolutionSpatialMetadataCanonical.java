package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.metadata.SpatialMetadataGroup;

/**
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class MultiResolutionSpatialMetadataCanonical implements SpatialMetadataGroup<CalibratedTransformMetadata> {

	private String path;

	private CalibratedTransformMetadata[] datasets;

	public MultiResolutionSpatialMetadataCanonical( String path, CalibratedTransformMetadata[] datasets )
	{
		this.path = path;
		this.datasets = datasets;
	}

	@Override
	public String[] getPaths() {

		// children store relative paths to parent
		return Arrays.stream( datasets ).map( x -> path + "/" + x.getPath() ).toArray( String[]::new );
	}

	@Override
	public CalibratedTransformMetadata[] getChildrenMetadata() {

		return datasets;
	}

	@Override
	public String getPath() {

		return path;
	}

	@Override
	public String[] units() {

		return Arrays.stream( datasets ).map( CalibratedTransformMetadata::unit ).toArray( String[]::new );
	}

}
