package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.lang.reflect.Type;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class CanonicalMetadataAdapter implements JsonDeserializer<CanonicalMetadata> {

	@Override
	public CanonicalMetadata deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		JsonObject jsonObj = json.getAsJsonObject();
		final String path = json.getAsJsonObject().get("path").getAsString();
		final Optional<DatasetAttributes> attrs = AbstractMetadataTemplateParser.datasetAttributes(context, json);

		SpatialMetadataCanonical spatial = null;
		if (jsonObj.has("spatialTransform")) {
			spatial = context.deserialize(jsonObj.get("spatialTransform"), SpatialMetadataCanonical.class);
		}

		MultiResolutionSpatialMetadataCanonical multiscale = null;
		if (jsonObj.has("multiscales")) {
			multiscale = context.deserialize(jsonObj.get("multiscales"), MultiResolutionSpatialMetadataCanonical.class);
		}

		MultiChannelMetadataCanonical multichannel = null;
		if (jsonObj.has("multichannel")) {
			multichannel = context.deserialize(jsonObj.get("multichannel"), MultiChannelMetadataCanonical.class);
		}

		if (attrs.isPresent()) {
			if( spatial != null )
				return new CanonicalSpatialDatasetMetadata(path, spatial, attrs.get());
			else 
				return new CanonicalDatasetMetadata(path, attrs.get());
		} else if (multiscale != null && multiscale.getChildrenMetadata() != null) {
			return new CanonicalMultiscaleMetadata(path, multiscale );
		} else if (multichannel != null && multichannel.getPaths() != null) {
			return new CanonicalMultichannelMetadata(path, multichannel );
		} else {
			// if lots of things are present
			return null;
		}
	}

}
