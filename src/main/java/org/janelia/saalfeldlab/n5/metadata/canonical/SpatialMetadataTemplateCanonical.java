package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.util.HashMap;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.metadata.transforms.CalibratedSpatialTransform;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataTemplateCanonical extends AbstractMetadataTemplateParser<SpatialMetadataCanonical>{

	public SpatialMetadataTemplateCanonical( final Gson gson, final String translation )
	{
		super( gson, translation );
	}

	@Override
	public Optional<SpatialMetadataCanonical> parseFromMap( final Gson gson, final HashMap<String, JsonElement> attributeMap ) {

		try { 
			final String path = GsonAttributesParser.parseAttribute(attributeMap, "path", String.class, gson);
			final CalibratedSpatialTransform transform = GsonAttributesParser.parseAttribute(attributeMap, "spatialTransform", CalibratedSpatialTransform.class, gson);
			final Axis[] axes = GsonAttributesParser.parseAttribute(attributeMap, "axes", Axis[].class, gson);
			final Optional<DatasetAttributes> attributes = AbstractMetadataTemplateParser.datasetAttributes(gson, attributeMap);

			if( attributes.isPresent())
				return Optional.of( new SpatialMetadataCanonical( path, transform, axes ));
			else
				return Optional.empty();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return Optional.empty();
	}

//	@Override
//	public Optional<SpatialMetadataTemplate> parseFromMap( final Gson gson, final HashMap<String, JsonElement> attributeMap ) {
//
//		try { 
//			final String path = GsonAttributesParser.parseAttribute(attributeMap, "path", String.class, gson);
//			
//			String unit = "pixel";
////			final String unit = GsonAttributesParser.parseAttribute(spatialTransformMap, "unit", String.class, gson);	
//
////			final JsonObject spatialTransformObj = attributeMap.get("spatialTransform").getAsJsonObject();
////			final HashMap<String, JsonElement> spatialTransformMap = gson.fromJson(spatialTransformObj, HashMap.class );
////
//////			final String unit = GsonAttributesParser.parseAttribute(spatialTransformMap, "unit", String.class, gson);
////			final String unit;
////			if( spatialTransformObj.has("unit"))
////				unit = spatialTransformObj.get("unit").getAsString();
////			else
////				unit = "pixel";
//
//
////			final double[] affineFlat = GsonAttributesParser.parseAttribute(attributeMap, "affine", double[].class, gson);
////			AffineSpatialTransform affine = gson.fromJson( attributeMap.get("transform"), AffineSpatialTransform.class );
//
////			AffineSpatialTransform affine = gson.fromJson( spatialTransformMap.get("transform"), AffineSpatialTransform.class );
////			AffineSpatialTransform affine = gson.fromJson( spatialTransformMap.get("transform"), AffineSpatialTransform.class );
//
////			Map transformObj = spatialTransformMap.get("transform");
////			double[] affineParams = gson.fromJson( transformObj.getAsJsonObject().get("affine"), double[].class );
//			AffineSpatialTransform affine = new AffineSpatialTransform( new double[12]);
//
//			final Optional<DatasetAttributes> attributes = AbstractMetadataTemplateParser.datasetAttributes(gson, attributeMap);
//
//			if( attributes.isPresent())
//				return Optional.of( new SpatialMetadataTemplate( path, attributes.get(), affine, unit ));
//			else
//				return Optional.empty();
//		}
//		catch( Exception e )
//		{
//			e.printStackTrace();
//		}
//
//		return Optional.empty();
//	}
	
//	@Override
//	public Optional<SpatialMetadataTemplate> parseFromMap( final Gson gson, HashMap<String, JsonElement> attributeMap) {
//
//		try { 
//			final String path = (String)attributeMap.get( "path" );
//
//			final Object affineObject = attributeMap.get( "affine" );
//			final ArrayList affineList = (ArrayList)attributeMap.get( "affine" );
//			final double[] affineFlat = affineList.stream().mapToDouble( x -> (Double)x).toArray();
//
////			final double[] affineFlat = (double[])attributeMap.get( "affine" );
//			final AffineTransform3D affine = new AffineTransform3D();
//			affine.set( affineFlat );
//			
//			
//			DatasetAttributes attributes = new DatasetAttributes();
//			AbstractMetadataTemplateParser.datasetAttributes( gson, attributeMap);
//
//			final String unit = (String)attributeMap.get( "unit" );
//			SpatialMetadataTemplate meta = new SpatialMetadataTemplate( path, attributes, affine, unit );
//			return Optional.of( meta );
//		}
//		catch( Exception e )
//		{}
//
//		return Optional.empty();
//	}


}
