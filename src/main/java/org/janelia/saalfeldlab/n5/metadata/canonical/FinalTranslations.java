package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;

public class FinalTranslations {
	

	public static final String ATTRPATHSFUN = "def attrPaths: paths | select(.[-1] == \"attributes\");";
	public static final String PARENTPATHFUN = "def parentPath: if length <= 1 then \"\" elif length == 2 then .[0] else .[0:-1] | map(select( . != \"children\")) | join(\"/\") end;";
	public static final String ADDPATHSFUN = "def addPaths: reduce attrPaths as $path ( . ; setpath( [ ($path | .[]), \"path\"] ; ( $path | parentPath )));";
	public static final String PATHFUNS = ATTRPATHSFUN + "\n" + PARENTPATHFUN + "\n" + ADDPATHSFUN;
	
	public static final String BUILDMULTISCALEFUN = "def buildMultiscale: [(.children | keys | .[]) as $k | .children |  {\"path\": (.[$k].attributes.path | split(\"/\") |.[-1]), \"spatialTransform\" : .[$k].attributes.spatialTransform }];\n";
	public static final String ADDMULTISCALEFUNS = "def addMultiscale: buildMultiscale as $ms | .attributes |= . + { \"multiscales\": { \"datasets\": $ms } };"
		+ "def addAllMultiscales: walk( if hasMultiscales then addMultiscale else . end );";
	public static final String HASMULTISCALEFUNS = "def attrHasTform: (.attributes | has(\"spatialTransform\"));\n"
		+ "def numTformChildren: .children | reduce (keys| .[]) as $k ( \n"
		+ "    [.,0]; \n"
		+ "    [  .[0], \n"
		+ "       if (.[0] | .[$k] | attrHasTform) then .[1] + 1 else .[1] end ]) \n"
		+ "      | .[1];                                                                                                                                                                                         \n"
		+ "def hasMultiscales: type == \"object\" and has(\"children\") and ( numTformChildren > 1 );\n";

	public static final String MULTISCALEFUNS = PATHFUNS + "\n" 
	+ HASMULTISCALEFUNS + "\n" 
	+ BUILDMULTISCALEFUN  + "\n" 
	+ ADDMULTISCALEFUNS; 

	public static final String COSEM2AFFINEFUN = "def cosemToAffine: [ .transform.scale[0], 0, 0, .transform.translate[0], 0, .transform.scale[1], 0, .transform.translate[1], 0, 0, .transform.scale[2], .transform.translate[2] ];";
	public static final String COSEMAXISINDEXESFUN = "def cosemAxisIndexes: [ (.axes | index(\"x\")) , (.axes | index(\"y\")), (.axes | index(\"z\"))];";

	public static final String N5V2AFFINEFUN = "def n5vToAffine: [ .transform.scale[0], 0, 0, .transform.translate[0], 0, .transform.scale[1], 0, .transform.translate[1], 0, 0, .transform.scale[2], .transform.translate[2] ];";
	public static final String N5VISCHANNELFUN = "def isChannel: type == \"object\" and has(\"attributes\") and (.attributes.path | test(\"^c\\\\d+$\")) and has(\"children\") and (.children | has(\"s0\"));";


	public static final String ISATTRIBUTESFUN = "def isAttributes: type == \"object\" and has(\"dimensions\") and has(\"dataType\");";
	public static final String ISDATASETFUN = "def isDataset: type == \"object\" and has(\"attribute\") and (.attributes | has(\"dimensions\") and has(\"dataType\") );";
	public static final String FLATTENTREEFUN = "def flattenTree: .. | select( type == \"object\" and has(\"path\")) | del(.children);";

	public static final String ADDDSOFFSETS3DFUN = "def addDsOffsets3d: .[1] as $d | .[0] | (.[3] |= ( 0.5* ($d[0] - 1))) | (.[7] |= ( 0.5* ($d[1] - 1))) | (.[11] |= ( 0.5* ($d[2] - 1)));";
	public static final String ADDDSOFFSETS2DFUN  = "def addDsOffsets2d: .[1] as $d | .[0] | (.[2] |= ( 0.5* ($d[0] - 1))) | (.[5] |= ( 0.5* ($d[1] - 1)));";
	public static final String ADDDSOFFSETSFUN  = "def addDsOffsets: if ( .[1] | length ) == 2 then addDsOffsets2d elif ( .[1] | length ) == 3 then addDsOffsets3d else null end;";

	public static final String N5VTOTRANSFORMARRFUN = "def n5vTransformArr: { \"type\":\"affine\", \"affine\": (.pixelResolution | affineFromScaleArray)};";
	public static final String N5VTOTRANSFORMOBJFUN = "def n5vTransformObj: { \"type\":\"affine\", \"affine\": (.pixelResolution.dimensions | affineFromScaleArray )};";

	public static final String AFFINEFROMSCALESFUN = 
		"def affineFromScaleArray: \n"
		+ "if length == 2 then [.[0], 0, 0, 0, .[1], 0] \n"
		+ "elif length == 3 then [.[0], 0, 0, 0, 0, .[1], 0, 0, 0, 0, .[2], 0] \n"
		+ "else null end;\n";
	
	public static final String AFFINEFROMSCALESANDFACTORSFUN = "def affineFromScaleAndFactors: .[0] as $s | .[1] as $d |\n"
		+ "if ( $s | length) == 2 then [ \n"
		+ "($s[0]*$d[0]), 0, ($d[0]-1)/2.0, 0, ($s[1]*$d[1]), ($d[1]-1)/2.0 ]"
		+ "elif ($s | length) == 3 then ["
		+ "($s[0]*$d[0]), 0, 0, ($d[0]-1)/2.0, 0, ($s[1]*$d[1]), 0, ($d[1]-1)/2.0, 0, 0,  ($s[2]*$d[2]), ($d[2]-1)/2.0 ] "
		+ "else null end;";

	public static final String N5VCHECKFUNS = 
		  "def isN5v: type == \"object\" and has(\"pixelResolution\");"
		+ "def n5visResObjDs: has(\"downsamplingFactors\") and has(\"pixelResolution\") and (.pixelResolution | type == \"object\");\n"
		+ "def n5visResObj: has(\"pixelResolution\") and (.pixelResolution | type == \"object\");\n"
		+ "def n5visResArrDs: has(\"downsamplingFactors\") and has(\"pixelResolution\") and (.pixelResolution | type == \"array\");\n"
		+ "def n5visResArr: has(\"pixelResolution\") and (.pixelResolution | type == \"array\");"; 

	public static final String N5VADDDSOFFSETSFUNS = 
			"def addDsOffsets3d: .[1] as $d | .[0] | (.[3] |= ( 0.5* ($d[0] - 1))) | (.[7] |= ( 0.5* ($d[1] - 1))) | (.[11] |= ( 0.5* ($d[2] - 1)));\n"
			+ "def addDsOffsets2d: .[1] as $d | .[0] | (.[2] |= ( 0.5* ($d[0] - 1))) | (.[5] |= ( 0.5* ($d[1] - 1)));\n"
			+ "def addDsOffsets: if ( .[1] | length ) == 2 then addDsOffsets2d elif ( .[1] | length ) == 3 then addDsOffsets3d else null end;";


//	public static final String N5VTOTRANSFORMFUN  = "def n5vToTransform:"
//		+ "{  \"transform\":"
//		+ "{ \"type\": \"affine\","
//		+ "\"affine\": [ "
//		+ "if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end,"
//		+ ".downsamplingFactors // [1, 1, 1] ] | affineFromScaleAndFactors\n"
//		+ "},"
//		+ "\"unit\" : (.pixelResolution.unit // \"pixel\")"
//		+ "};";
	
	public static final String arrayAndUnitToTransformFun = "def arrayAndUnitToTransform: {"
			+ "    \"spatialTransform\": {"
			+ "        \"transform\" : {"
			+ "            \"type\": \"affine\","
			+ "            \"affine\": .[0]"
			+ "        },"
			+ "        \"unit\": .[1]"
			+ "    }"
			+ "};";

	public static final String N5VTOTRANSFORMFUN  =
			"    def n5vToTransform: . + { \n"
			+ "        \"spatialTransform\": { \n"
			+ "            \"transform\": {\n"
			+ "                \"type\": \"affine\",\n"
			+ "                \"affine\": \n"
			+ "                    [ if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end,\n"
			+ "                       .downsamplingFactors // [1, 1, 1] ] \n"
			+ "                    | affineFromScaleAndFactors },\n"
			+ "            \"unit\" : (.pixelResolution.unit // \"pixel\") \n"
			+ "         }\n"
			+ "    };";

	public static final String N5VTOTRANSFORMOLDFUN  = "def n5vToTransformOld:"
			+ "{\"transform\":"
			+ "{ \"type\": \"affine\","
			+ "\"affine\":"
			+ " [(( if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end) | affineFromScaleArray),"
			+ ".downsamplingFactors // [1, 1, 1] ] | addDsOffsets"
			+ "},                                                                                                                                                                                           \n"
			+ "\"unit\" : (.pixelResolution.unit // \"pixel\")"
			+ "};";

	public static final String N5VTOTRANSFORMALLFUN  = "def n5vToTransformAll: "
			+ "walk ( if isAttributes and has(\"pixelResolution\") then . + (.|n5vToTransform) else . end );";
	
	public static final String N5VFUNS = String.join( "\n",
			N5VISCHANNELFUN,
			AFFINEFROMSCALESFUN,
			AFFINEFROMSCALESANDFACTORSFUN,
			N5VTOTRANSFORMARRFUN,
			N5VTOTRANSFORMOBJFUN,
			N5VCHECKFUNS,
			N5VADDDSOFFSETSFUNS,
			N5VTOTRANSFORMFUN,
			N5VTOTRANSFORMALLFUN);

	public static final java.util.function.Function<String,String> IJ2AFFINEFUN_WITHPATH = s -> String.format( 
			"{ \"affine\": [.pixelWidth, 0, 0, 0, 0, .pixelHeight, 0, 0, 0, 0, .pixelDepth, 0], "
			+ "\"unit\": .pixelUnit, "
			+ "\"path\": \"%s\" }",
			s );
	
//	public static final java.util.function.Function<String,String> APPLY2DATASETS = s -> String.format( 
//			"{ \"affine\": [.pixelWidth, 0, 0, 0, 0, .pixelHeight, 0, 0, 0, 0, .pixelDepth, 0], "
//			+ "\"unit\": .pixelUnit, "
//			+ "\"path\": \"%s\" }",
//			s );


	// TODO need to incorporate path variable here
	public static final String COSEM_TO_AFFINE_NOAXES= 
			"def cosemToAffine: [ .transform.scale[0], 0, 0, .transform.translate[0], 0, .transform.scale[1], 0, .transform.translate[1], 0, 0, .transform.scale[2], .transform.translate[2] ];\n" 
			+ "{\n"
			+ "\t\"affine\": (. | cosemToAffine),\n" +
			"\t\"unit\": .transform.units[0],\n" +
			"\t\"path\": \"\"\n" +
			"}\n";

	public static final String COSEM_TO_AFFINE = 
			"def isCosem: has(\"transform\") and "
			+ 	"(.transform | has(\"axes\") and has(\"scale\") and has(\"translate\") and has(\"units\") );"
			+ "def cosemAxisIndexes: [ (.axes | index(\"x\")) , (.axes | index(\"y\")), (.axes | index(\"z\"))];\n"
			+ "def cosemToAffine: [ .transform.scale[ .transform.axisIndexes[0] ], 0, 0, .transform.translate[.transform.axisIndexes[0]],"
				+ "0, .transform.scale[ .transform.axisIndexes[1] ], 0, .transform.translate[.transform.axisIndexes[1]]," 
				+ "0, 0, .transform.scale[ .transform.axisIndexes[2] ], .transform.translate[.transform.axisIndexes[2]] ];"
			+ "(.transform | cosemAxisIndexes) as $a | .transform.axisIndexes = $a |"
			+ " { \"affine\": cosemToAffine, "
			+ 	"\"unit\": .transform.units[0],"
			+ 	"\"path\": \"\"}";
	
	public static final String COSEMFUNS = 
			  "def isCosem: type == \"object\" and has(\"transform\") and (.transform | has(\"axes\") and has(\"scale\") and has(\"translate\") and has(\"units\") );\n"
			+ "def cosemAxisIndexes: {\"axisIndexes\":[ (.axes | index(\"x\")) , (.axes | index(\"y\")), (.axes | index(\"z\")) ]};\n"
			+ "def cosemToTransformSimple: { \"spatialTransform\": {\n"
			+ "        \"transform\": {\n"
			+ "            \"type\":\"affine\", \n"
			+ "            \"affine\": [ .scale[.axisIndexes[0]], 0.0, 0.0, .translate[.axisIndexes[0]], \n"
			+ "                        0.0, .scale[.axisIndexes[1]], 0.0, .translate[.axisIndexes[1]], \n"
			+ "                        0.0, 0.0, .scale[.axisIndexes[2]], .translate[.axisIndexes[2]]]\n"
			+ "        },\n"
			+ "        \"unit\" : .units[0]\n"
			+ "        }\n"
			+ "    };\n"
			+ "def cosemToTransform: (.transform |= . + cosemAxisIndexes) | . + (.transform | cosemToTransformSimple);";

	public static final String IJ_TO_AFFINE = "{\n"
			+ "\t\"affine\": [.pixelWidth, 0, 0, 0, 0, .pixelHeight, 0, 0, 0, 0, .pixelDepth, 0],\n" +
			"\t\"unit\": .pixelUnit,\n" +
			"\t\"path\": \"\"\n" +
			"}\n";

	public static void main(String[] args) throws IOException {
		
//		final String translation = COSEM_TO_AFFINE;
		final String translation = COSEM_TO_AFFINE;

//		final String attributesPath = "/home/john/tmp/assorted.n5/mitosis/attributes.json";
//		final String attributesPath = "/home/john/tmp/jfrc2010.n5/volumes/raw/attributes.json";
		final String attributesPath = "/home/john/tmp/jqExamples/flipAxes.json";
		
		final List<String> lines = Files.readAllLines(Paths.get(attributesPath));

//		lines.stream().forEach( System.out::println );

		final String json = lines.stream().collect(Collectors.joining("\n"));
		System.out.println( json );
		System.out.println( " " );
		System.out.println( "#######################################" );
		System.out.println( " " );


		System.out.println( translation );


		System.out.println( " " );
		System.out.println( "#######################################" );
		System.out.println( " " );

		System.out.println(translate(json, translation));

	}
	
	public static String translate(final String json, final String translation) {

		// First of all, you have to prepare a Scope which s a container of
		// built-in/user-defined functions and variables.
		final Scope rootScope = Scope.newEmptyScope();

		// Use BuiltinFunctionLoader to load built-in functions from the
		// classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {

			@Override
			public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output,
					final Version version) throws JsonQueryException {

				args.get(0).apply(scope, in, (time) -> {
					output.emit(new TextNode(Strings.repeat(in.asText(), time.asInt())), null);
				});
			}
		});

		ObjectMapper objMapper = new ObjectMapper();		
		JsonNode in;
		try {

			in = objMapper.readTree( json );

			final List<JsonNode> out = new ArrayList<>();
			JsonQuery.compile(translation, Versions.JQ_1_6).apply(rootScope, in, out::add);

			final StringBuffer stringOutput = new StringBuffer();
			for (final JsonNode n : out)
				stringOutput.append(n.toString() + "\n");

			return stringOutput.toString();

		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return null;
	}

}
