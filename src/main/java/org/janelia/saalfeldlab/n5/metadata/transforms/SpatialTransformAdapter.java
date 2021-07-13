package org.janelia.saalfeldlab.n5.metadata.transforms;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SpatialTransformAdapter //implements JsonDeserializer<SpatialTransform> {
	implements JsonDeserializer<SpatialTransform> {
//	implements JsonDeserializer<LinearSpatialTransform> {

	@Override
	public SpatialTransform deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		
		if( !json.isJsonObject() )
			return null;
		
		final JsonObject jobj = json.getAsJsonObject();
		if( !jobj.has("type") )
			return null;

		switch( jobj.get("type").getAsString() )
		{
		case("identity"):
			return context.deserialize( jobj, IdentitySpatialTransform.class );
		case("scale"):
			return context.deserialize( jobj, ScaleSpatialTransform.class );
		case("translation"):
			return context.deserialize( jobj, TranslationSpatialTransform.class );
		case("affine"):
			return context.deserialize( jobj, AffineSpatialTransform.class );
		case("sequence"):
			return context.deserialize( jobj, SequenceSpatialTransform.class );
		
		}
		return null;
	}
	
	public static void main( String[] args )
	{
		
		final String affineString = "{"
				+ "\"type\": \"affine\","
				+ "\"affine\" : [ 11.0, 0.0, 0.1, 0.0, 12.0, 0.2 ]"
				+ "}";

		final String scaleString = "{"
				+ "\"type\": \"scale\","
				+ "\"scale\" : [ 11.0, -8.0 ]"
				+ "}";

		final String translationString = "{"
				+ "\"type\": \"translation\","
				+ "\"translation\" : [ -0.9, 2.1 ]"
				+ "}";

		final String idString = "{"
				+ "\"type\": \"identity\""
				+ "}";

		final String seqString = "{"
				+ "\"type\": \"sequence\","
				+ "\"transformations\": [" 
				+ scaleString + "," + translationString 
				+ "]}";
		

		
		final SpatialTransformAdapter adapter = new SpatialTransformAdapter();

		final GsonBuilder gsonBuilder = new GsonBuilder();
//		gsonBuilder.registerTypeHierarchyAdapter(SpatialTransform.class, adapter );
		gsonBuilder.registerTypeAdapter( SpatialTransform.class, adapter );
		gsonBuilder.disableHtmlEscaping();

		final Gson gson = gsonBuilder.create();

		SpatialTransform parsedAffine = gson.fromJson(affineString, SpatialTransform.class);
		System.out.println( affineString );
		System.out.println( parsedAffine );
		System.out.println( gson.toJson( parsedAffine ));
		System.out.println( " " );

		SpatialTransform parsedScale = gson.fromJson(scaleString, SpatialTransform.class);
		System.out.println( scaleString );
		System.out.println( parsedScale );
		System.out.println( gson.toJson( parsedScale));
		System.out.println( " " );

		SpatialTransform parsedTranslation = gson.fromJson(translationString, SpatialTransform.class);
		System.out.println( translationString );
		System.out.println( parsedTranslation );
		System.out.println( gson.toJson( parsedTranslation));
		System.out.println( " " );
		
		SpatialTransform parsedId = gson.fromJson(idString, SpatialTransform.class);
		System.out.println( idString );
		System.out.println( parsedId );
		System.out.println( gson.toJson( parsedId ));
		System.out.println( " " );

		SpatialTransform parsedSeq = gson.fromJson(seqString, SpatialTransform.class);
		System.out.println( seqString );
		System.out.println( parsedSeq );
		System.out.println( gson.toJson( parsedSeq ));
		System.out.println( " " );
		
		ScaleSpatialTransform s = new ScaleSpatialTransform( new double[] {1, 2 });
		TranslationSpatialTransform t = new TranslationSpatialTransform( new double[] {3, 4 });
		SequenceSpatialTransform seq = new SequenceSpatialTransform( new SpatialTransform[] { s, t });
		
		System.out.println( gson.toJson(seq) );

	}


}
