package org.janelia.saalfeldlab.n5.metadata.transforms;

import java.lang.reflect.Type;

import org.janelia.saalfeldlab.n5.N5Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SpatialTransformAdapter //implements JsonDeserializer<SpatialTransform> {
	implements JsonDeserializer<SpatialTransform>, JsonSerializer<SpatialTransform> {
//	implements JsonDeserializer<LinearSpatialTransform> {

	final N5Reader n5;

	public SpatialTransformAdapter( final N5Reader n5 ) {
		this.n5 = n5;
	}

	@Override
	public SpatialTransform deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		
		if( !json.isJsonObject() )
			return null;
		
		final JsonObject jobj = json.getAsJsonObject();
		if( !jobj.has("type") )
			return null;

		SpatialTransform out = null;
		switch( jobj.get("type").getAsString() )
		{
		case("identity"):
			out = context.deserialize( jobj, IdentitySpatialTransform.class );
			break;
		case("scale"):
			out = context.deserialize( jobj, ScaleSpatialTransform.class );
			break;
		case("translation"):
			out = context.deserialize( jobj, TranslationSpatialTransform.class );
			break;
		case("scale_offset"):
			out = context.deserialize( jobj, ScaleOffsetSpatialTransform.class );
			break;
		case("affine"):
			out = context.deserialize( jobj, AffineSpatialTransform.class );
			break;
		case("sequence"):
			out = context.deserialize( jobj, SequenceSpatialTransform.class );
			break;
		}
		readTransformParameters(out);
		
		return out;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final SpatialTransform readTransformParameters( final SpatialTransform transform ) {

		if( transform instanceof ParametrizedTransform ) {
			ParametrizedTransform pt = (ParametrizedTransform)transform;
			if( pt.getParameterPath() != null ) {
				pt.buildTransform( pt.getParameters(n5));
			}
		}
		return transform;
	}

	@Override
	public JsonElement serialize(SpatialTransform src, Type typeOfSrc, JsonSerializationContext context) {
		return context.serialize(src);
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
		

		
		final SpatialTransformAdapter adapter = new SpatialTransformAdapter( null );

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
