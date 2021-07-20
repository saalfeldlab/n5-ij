package org.janelia.saalfeldlab.n5.metadata.container;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.FinalTranslations;
import org.janelia.saalfeldlab.n5.metadata.canonical.MultiResolutionSpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataTemplateCanonical;
import org.janelia.saalfeldlab.n5.metadata.canonical.TranslatedTreeMetadataParser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * 
 * @author John Bogovic
 *
 */
public class ContainerMetadata {

//	public static String container2 = "{\"attributes\":{\"n5\":\"2.5.0\"},\"children\":{\"cosem\":{\"attributes\":{},\"children\":{\"raw\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[8,8,8],\"translate\":[0,0,0],\"units\":[\"nm\",\"nm\",\"nm\"]},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[32,32,32],\"dimensions\":[512,512,260]},\"children\":{}},\"s1\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[16,16,16],\"translate\":[2,2,2],\"units\":[\"nm\",\"nm\",\"nm\"]},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[32,32,32],\"dimensions\":[512,512,260]},\"children\":{}}}},\"labels\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[8,8,8],\"translate\":[0,0,0],\"units\":[\"nm\",\"nm\",\"nm\"]},\"dataType\":\"uint64\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[32,32,32],\"dimensions\":[512,512,260]},\"children\":{}},\"s1\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[16,16,16],\"translate\":[2,2,2],\"units\":[\"nm\",\"nm\",\"nm\"]},\"dataType\":\"uint64\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[32,32,32],\"dimensions\":[512,512,260]},\"children\":{}}}}}},\"n5v\":{\"attributes\":{},\"children\":{\"c0\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"pixelResolution\":{\"unit\":\"microns\",\"dimensions\":[1.24296,1.24296,1.24296]},\"dataType\":\"uint8\",\"compression\":{\"type\":\"gzip\",\"useZlib\":false,\"level\":-1},\"blockSize\":[64,64,64],\"dimensions\":[505,235,147]},\"children\":{}},\"s1\":{\"attributes\":{\"pixelResolution\":{\"unit\":\"microns\",\"dimensions\":[2.48592,2.48592,2.469123243243243]},\"dataType\":\"uint8\",\"compression\":{\"type\":\"gzip\",\"useZlib\":false,\"level\":-1},\"blockSize\":[64,64,64],\"dimensions\":[253,118,74]},\"children\":{}}}},\"c1\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"pixelResolution\":{\"unit\":\"microns\",\"dimensions\":[3.551314285714286,3.551314285714286,3.5137523076923074]},\"dataType\":\"uint8\",\"compression\":{\"type\":\"gzip\",\"useZlib\":false,\"level\":-1},\"blockSize\":[64,64,64],\"dimensions\":[177,83,52]},\"children\":{}}}}}}}}\n";

	public static String t1headTranslateOld = "{\"attributes\":{\"n5\":\"2.4.0\",\"scales\":[[1.0,1.0,1.0],[2.0,2.0,2.0]]},\"children\":{\"c0\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"transform\":{\"matrix\":[[1.5,0,0,0],[0,1.5,0,0],[0,0,1.5,0]],\"unit\":\"mm\"},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[16,16,16],\"dimensions\":[256,256,129]},\"children\":{}},\"s1\":{\"attributes\":{\"transform\":{\"matrix\":[[3.0,0,0,0.5],[0,3.0,0,0.5],[0,0,3.0,0.5]],\"unit\":\"mm\"},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[16,16,16],\"dimensions\":[128,128,64]},\"children\":{}}}}}}\n";

	public static String t1headTranslate = "{\"attributes\":{\"n5\":\"2.5.0\",\"scales\":[[1.0,1.0,1.0],[2.0,2.0,2.0]]},\"children\":{\"c0\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"transform\":{\"matrix\":[[1.5,0,0,0],[0,1.5,0,0],[0,0,1.5,0]],\"unit\":\"mm\"},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[16,16,16],\"dimensions\":[256,256,129]},\"children\":{}},\"s1\":{\"attributes\":{\"transform\":{\"matrix\":[[3.0,0,0,0.5],[0,3.0,0,0.5],[0,0,3.0,0.5]],\"unit\":\"mm\"},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"level\":-1},\"blockSize\":[16,16,16],\"dimensions\":[128,128,64]},\"children\":{}}}},\"c1\":{\"attributes\":{},\"children\":{\"s0\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[1.0,1.0,1.0],\"translate\":[0.0,0.0,0.0],\"units\":[\"pixel\",\"pixel\",\"pixel\"]},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"useZlib\":false,\"level\":-1},\"blockSize\":[64,64,64],\"dimensions\":[256,256,129]},\"children\":{}},\"s1\":{\"attributes\":{\"transform\":{\"axes\":[\"z\",\"y\",\"x\"],\"scale\":[2.0,2.0,2.0],\"translate\":[0.5,0.5,0.5],\"units\":[\"pixel\",\"pixel\",\"pixel\"]},\"dataType\":\"uint16\",\"compression\":{\"type\":\"gzip\",\"useZlib\":false,\"level\":-1},\"blockSize\":[64,64,64],\"dimensions\":[128,128,64]},\"children\":{}}}}}}\n";;
	
	public static void main( String[] args ) throws IOException, InterruptedException, ExecutionException {
////		N5FSReader n5 = new N5FSReader("/home/john/tmp/assorted.n5");
////		N5FSReader n5 = new N5FSReader("/home/john/tmp/t1-head.n5");
////		N5FSReader n5 = new N5FSReader("/home/john/tmp/containerTranslation.n5");
//		N5FSReader n5 = new N5FSReader("/home/john/tmp/containerTranslation2.n5");
		N5FSReader n5 = new N5FSReader( "/home/john/tmp/t1-head_multiscale_toTranslate.n5" );
////
		ContainerMetadataNode node = ContainerMetadataNode.build( n5 );
		System.out.println( node );
		System.out.println( " " );
		Gson gson = n5.getGson();
		String jsonString = gson.toJson( node );

//		Gson gson = new Gson();
//		String jsonString = container2;
		
//		String jsonString = t1headTranslate;

		System.out.println( " " );
		System.out.println( jsonString );
		System.out.println( " " );

//		ContainerMetadataNode nodeParsed = gson.fromJson( jsonString, ContainerMetadataNode.class );
//		System.out.println( nodeParsed );
//
////		Optional<ContainerMetadataNode> childNode = nodeParsed.getChild("volumes/raw/c0", "/" );
//		Optional<ContainerMetadataNode> childNode = nodeParsed.getChild("c0/s1", "/" );
//		System.out.println( childNode );

//		StringBuffer translation = new StringBuffer();
//		translation.append( FinalTranslations.ISATTRIBUTESFUN + "\n");
//		translation.append( FinalTranslations.N5VFUNS + "\n");
//		translation.append( FinalTranslations.COSEMFUNS + "\n");
//		translation.append( FinalTranslations.MULTISCALEFUNS + "\n");
//		translation.append( "n5vToTransformAll | addPaths |  walk ( if isChannel then . |= addMultiscale else . end ) " );

//		StringBuffer translation = new StringBuffer();
//		translation.append( FinalTranslations.ISATTRIBUTESFUN + "\n");
//		translation.append( FinalTranslations.N5VFUNS + "\n");
//		translation.append( FinalTranslations.COSEMFUNS + "\n");
//		translation.append( FinalTranslations.MULTISCALEFUNS + "\n");
//		translation.append( "walk( if isCosem then cosemToTransform else . end ) | walk ( if isN5v then n5vToTransform else . end )\n" );
//		translation.append( "| addPaths | addAllMultiscales" );

		StringBuffer translation = new StringBuffer();
		translation.append( FinalTranslations.ISATTRIBUTESFUN + "\n");
		translation.append( FinalTranslations.N5VFUNS + "\n");
//		translation.append( FinalTranslations.COSEMFUNS + "\n");
		translation.append( FinalTranslations.MULTISCALEFUNS + "\n");
//		translation.append( FinalTranslations.MULTICHANNELFUNSSIMPLE + "\n");
		translation.append( FinalTranslations.MULTICHANNELFUNS + "\n");
		translation.append(
				" def canConvert: type == \"object\" and has(\"transform\") and \n"
				+ "        (.transform | has(\"matrix\") and has(\"unit\"));\n"
				+ " def convert:\n"
				+ "  arrayUnitToTransform(\n"
				+ "    .transform.matrix | flatten;\n"
				+ "    .transform.unit ) ;\n" );
		translation.append( "walk( if canConvert then . + convert else . end )" );

//		translation.append( "| addPaths | addAllMultiscales | addAllMultiChannelSimple" );
		translation.append( "| addPaths | addAllMultiscales | addAllMultiChannel" );
		
		System.out.println( translation.toString() );

		final String jsonTranslated = FinalTranslations.translate( jsonString, translation.toString() );
		System.out.println( "" );
		System.out.println( jsonTranslated );
		System.out.println( "" );

//		ContainerMetadataNode rootParsed = gson.fromJson( jsonTranslated, ContainerMetadataNode.class );
//		System.out.println( "" );
//		System.out.println( rootParsed );
//		System.out.println( "" );

//		SpatialMetadataTemplateParser parser = new SpatialMetadataTemplateParser( gson, "" );
////		ContainerMetadataNode parsedNode = rootParsed.getChild("c0/s0", "/").get();
//		ContainerMetadataNode parsedNode = rootParsed.getChild("cosem/raw/s0", "/").get();
//
//		HashMap<String, JsonElement> attrs = parsedNode.getAttributes();
//		Optional<SpatialMetadataTemplate> spatialMetadataOpt = parser.parseFromMap(gson, attrs );
//
//		SpatialMetadataTemplate spatialMetadata = spatialMetadataOpt.get();
//		System.out.println( spatialMetadata );
//
//		System.out.println("done");
		
//		N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer(n5, loaderExecutor, n5NodeFilter,
//				parserList, groupParserList );

//		ArrayList parserList = new ArrayList();
		
		TranslatedTreeMetadataParser parser = new TranslatedTreeMetadataParser(n5, translation.toString() );
//		TranslatedTreeMetadataParser parser = new TranslatedTreeMetadataParser( jsonString, translation.toString() );
////		Gson gson;
		gson = parser.getGson();


		Optional<CanonicalMetadata> meta = parser.parseMetadata("", "/");
		System.out.println( "" );
		System.out.println( meta );
		System.out.println( "" );
		
//		MultiResolutionSpatialMetadataCanonical ms = gson.fromJson( GsonOneOffs.multiscales,
//				MultiResolutionSpatialMetadataCanonical.class);
//		System.out.println( ms );
		

//		List<N5MetadataParser<?>> parserList = Collections.singletonList(parser);
//		N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer(
//				n5, 
//				Executors.newSingleThreadExecutor(),
//				x -> true,
//				parserList,
//				null);
//
//		  N5TreeNode rootNode = datasetDiscoverer.discoverAndParseRecursive("");
//		  System.out.println( rootNode );
		  
		  
	}

}
