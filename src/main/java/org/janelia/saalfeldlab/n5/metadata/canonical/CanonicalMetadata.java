package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.lang.reflect.Type;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class CanonicalMetadata implements N5Metadata {

	private final String path;
	private final SpatialMetadataCanonical spatialTransform;
	private final MultiResolutionSpatialMetadataCanonical multiscales;
	private final DatasetAttributes attributes;
	
	public CanonicalMetadata(final String path, final SpatialMetadataCanonical spatialTransform,
			final MultiResolutionSpatialMetadataCanonical multiscales, final DatasetAttributes attributes) {
		this.path = path;
		this.spatialTransform = spatialTransform;
		this.multiscales = multiscales;
		this.attributes = attributes;
	}

	@Override
	public String getPath() {
		return path;
	}

	public DatasetAttributes getAttributes() {
		return attributes;
	}

	public SpatialMetadataCanonical getSpatialTransform() {
		return spatialTransform;
	}

	public MultiResolutionSpatialMetadataCanonical getMultiscales() {
		return multiscales;
	}

	public static class CanonicalMetadataAdapter implements JsonDeserializer<CanonicalMetadata> {
		@Override
		public CanonicalMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {

			System.out.println("deserialize CanonicalMetadata");
			JsonObject jsonObj = json.getAsJsonObject();

			final String path = json.getAsJsonObject().get("path").getAsString();
			final Optional<DatasetAttributes> attrs = AbstractMetadataTemplateParser.datasetAttributes(context, json);

			final SpatialMetadataCanonical spatial;
			if( jsonObj.has("spatialTransform"))
				spatial = context.deserialize( jsonObj.get("spatialTransform"), SpatialMetadataCanonical.class);
			else
				spatial = null;

			final MultiResolutionSpatialMetadataCanonical multiscale;
			if( jsonObj.has("multiscales"))
				multiscale = context.deserialize(json, MultiResolutionSpatialMetadataCanonical.class);
			else
				multiscale = null;

			if( attrs.isPresent() )
			{
				System.out.println( path );
				System.out.println( "  parsed CanonicalDatasetMetadata");
				return new CanonicalDatasetMetadata(path, spatial, multiscale, attrs.get());
			}
			else
			{
				System.out.println( path );
				System.out.println( "  parsed CanonicalMetadata");
				return new CanonicalMetadata(path, spatial, multiscale, null );
			}
		}

	}
}