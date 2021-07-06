package org.janelia.saalfeldlab.n5.metadata.template;

import java.util.HashMap;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GsonAttributesParser;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


/**
 * Interface for metadata describing how spatial data are oriented in physical space.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public class SpatialMetadataTemplateParser extends AbstractMetadataTemplateParser<SpatialMetadataTemplate>{

	public SpatialMetadataTemplateParser( final Gson gson, final String translation )
	{
		super( gson, translation );
	}

//	@Override
//	public Optional<SpatialMetadataTemplate> parse( final Gson gson, final JsonElement elem ) {
//
//		try {
//			return Optional.of(gson.fromJson(elem, SpatialMetadataTemplate.class));
//		} catch (Exception e) {}
//
//		return Optional.empty();
//	}

	@Override
	public Optional<SpatialMetadataTemplate> parseFromMap( final Gson gson, final HashMap<String, JsonElement> attributeMap ) {

		try { 
			final String path = GsonAttributesParser.parseAttribute(attributeMap, "path", String.class, gson);
			final String unit = GsonAttributesParser.parseAttribute(attributeMap, "unit", String.class, gson);

//			final double[] affineFlat = GsonAttributesParser.parseAttribute(attributeMap, "affine", double[].class, gson);
			AffineSpatialTransform affine = gson.fromJson( attributeMap.get("transform"), AffineSpatialTransform.class );

			final Optional<DatasetAttributes> attributes = AbstractMetadataTemplateParser.datasetAttributes( gson, attributeMap);

			if( attributes.isPresent())
				return Optional.of( new SpatialMetadataTemplate( path, attributes.get(), affine, unit ));
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
