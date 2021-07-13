package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.lang.reflect.Type;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.AbstractN5Metadata;
import org.janelia.saalfeldlab.n5.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.metadata.transforms.CalibratedSpatialTransform;
import org.janelia.saalfeldlab.n5.metadata.transforms.LinearSpatialTransform;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.imglib2.realtransform.AffineGet;

/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataCanonical implements SpatialMetadata {

	private final LinearSpatialTransform transform;
	private final String unit;
	private final String path;
	
	public SpatialMetadataCanonical( final String path, final LinearSpatialTransform transform, final String unit) {

		this.path = path;
		this.unit = unit;
		this.transform = transform;
	}	
	
	public SpatialMetadataCanonical( final String path, CalibratedSpatialTransform calTransform) {

		this.path = path;
		this.unit = calTransform.getUnit();
		this.transform = calTransform.getSpatialTransform();
	}

	@Override
	public AffineGet spatialTransform() {
		return transform.getTransform();
	}

	@Override
	public String unit() {

		return unit;
	}

	@Override
	public String getPath() {
		return path;
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
