package org.janelia.saalfeldlab.n5.metadata.template;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataTemplate extends AbstractN5DatasetMetadata implements SpatialMetadata {

//	private final double[] affine;

	private final AffineSpatialTransform transform;
	private final String unit;
	private final String type;

//	public SpatialMetadataTemplate(final String path, final DatasetAttributes attributes,
//			final double[] affine, final String unit) {
//
//		super(path, attributes);
//		this.unit = unit;
//		this.affine = affine;
//		this.type = "affine";
//	}
	
	public SpatialMetadataTemplate(final String path, final DatasetAttributes attributes,
			final AffineSpatialTransform transform, final String unit) {

		super(path, attributes);
		this.unit = unit;
		this.transform = transform;
		this.type = "affine";
	}	

	@Override
	public AffineGet spatialTransform() {
//		final AffineTransform3D transform = new AffineTransform3D();
//		transform.set( affine );
//		return transform;
		return transform.transform;
	}

	@Override
	public String unit() {

		return unit;
	}

}
