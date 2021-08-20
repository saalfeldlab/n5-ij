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
	private final MultiChannelMetadataCanonical multichannels;
	private final DatasetAttributes attributes;
	
	public CanonicalMetadata(final String path, 
			final SpatialMetadataCanonical spatialTransform,
			final MultiResolutionSpatialMetadataCanonical multiscales, 
			final MultiChannelMetadataCanonical multichannels, 
			final DatasetAttributes attributes) {
		this.path = path;
		this.spatialTransform = spatialTransform;
		this.multiscales = multiscales;
		this.multichannels = multichannels;
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

	public MultiChannelMetadataCanonical getMultichannels() {
		return multichannels;
	}

	public static class CanonicalMetadataAdapter implements JsonDeserializer<CanonicalMetadata> {
		@Override
		public CanonicalMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {

			JsonObject jsonObj = json.getAsJsonObject();
			final String path = json.getAsJsonObject().get("path").getAsString();
			final Optional<DatasetAttributes> attrs = AbstractMetadataTemplateParser.datasetAttributes(context, json);

			SpatialMetadataCanonical spatial = null;
			if( jsonObj.has("spatialTransform")) {
				spatial = context.deserialize( jsonObj.get("spatialTransform"), SpatialMetadataCanonical.class);
			}

			MultiResolutionSpatialMetadataCanonical multiscale = null;
			if( jsonObj.has("multiscales")) {
				multiscale = context.deserialize( jsonObj.get("multiscales"), MultiResolutionSpatialMetadataCanonical.class);
			}

			MultiChannelMetadataCanonical multichannel = null;
			if( jsonObj.has("multichannel")) {
				multichannel = context.deserialize( jsonObj.get("multichannel"), MultiChannelMetadataCanonical.class);
			}

			if( attrs.isPresent() ) {
				return new CanonicalDatasetMetadata(path, spatial, multiscale, multichannel, attrs.get());
			}
			else if( multiscale != null && multiscale.getChildrenMetadata() != null ) {
				return new CanonicalMultiscaleMetadata(path, spatial, multiscale, multichannel, null );
			}
			else if( multichannel != null && multichannel.getPaths() != null ) {
				return new CanonicalMultichannelMetadata(path, null, null, multichannel, null );
			}
			else {
				return new CanonicalMetadata(path, spatial, multiscale, multichannel, null );
			}
		}

	}
}
