package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.metadata.transforms.CalibratedSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.LinearSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.SpatialTransform;

import net.imglib2.realtransform.AffineGet;

/**
 * Interface for metadata describing how spatial data are oriented in physical
 * space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataCanonical implements SpatialMetadata, AxisMetadata {

	private final SpatialTransform transform;
	private final String unit; // redundant, also in axis list
	private final String path;
	private final Axis[] axes;

	public SpatialMetadataCanonical(final String path, final SpatialTransform transform, final String unit,
			final Axis[] axes) {

		this.path = path;
		this.unit = unit;
		this.transform = transform;
		this.axes = axes;
	}

	public SpatialMetadataCanonical(final String path, CalibratedSpatialTransform calTransform, final Axis[] axes) {

		this.path = path;
		this.unit = calTransform.getUnit();
		this.transform = calTransform.getSpatialTransform();
		this.axes = axes;
	}

	public SpatialTransform transform() {
		return transform;
	}

	@Override
	public AffineGet spatialTransform() {
		if (transform instanceof LinearSpatialTransform) {
			return ((LinearSpatialTransform) transform).getTransform();
		} else
			return null;
	}

	@Override
	public String unit() {

		return unit;
	}

	@Override
	public String getPath() {
		return path;
	}

	public Axis[] getAxes() {
		return axes;
	}

	@Override
	public String[] getAxisLabels() {
		return Arrays.stream( axes ).map( Axis::getLabel).toArray( String[]::new );
	}

	@Override
	public String[] getAxisTypes() {
		return Arrays.stream( axes ).map( Axis::getType ).toArray( String[]::new );
	}

	@Override
	public String[] getUnits() {
		return Arrays.stream( axes ).map( Axis::getUnit ).toArray( String[]::new );
	}

//	public static class SpatialMetadataTemplateAdapter implements JsonDeserializer<SpatialMetadataTemplate>
//	{
//
//		@Override
//		public SpatialMetadataTemplate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
//				throws JsonParseException {
//
//			System.out.println( "deserialize SpatialMetadataTemplateAdapter");
//			LinearSpatialTransform transform = context.deserialize(
//					json.getAsJsonObject().get("spatialTransform"), 
//					LinearSpatialTransform.class );
//
//			String path = context.deserialize(
//					json.getAsJsonObject().get("path"), 
//					String.class );
//
//			String unit = context.deserialize(
//					json.getAsJsonObject().get("unit"), 
//					String.class );
//
//			if( attrs.isPresent() )
//				return new SpatialMetadataTemplate( path, transform, unit );
//			else
//				return new SpatialMetadataTemplate( path, transform, unit );
//		}
//		
//	}

}
