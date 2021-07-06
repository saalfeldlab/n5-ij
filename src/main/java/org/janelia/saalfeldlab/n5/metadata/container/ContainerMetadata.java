package org.janelia.saalfeldlab.n5.metadata.container;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.demos.JqExps;
import org.janelia.saalfeldlab.n5.metadata.template.FinalTranslations;
import org.janelia.saalfeldlab.n5.metadata.template.SpatialMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.template.SpatialMetadataTemplateParser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * 
 * @author John Bogovic
 *
 */
public class ContainerMetadata {
	
	public static < T extends AbstractGsonReader> ContainerMetadataNode build( final T n5 ) throws InterruptedException, ExecutionException
	{
		String[] datasets;
		N5TreeNode root;
		try {

//			datasets = n5.deepListDatasets("");
			datasets = n5.deepList("", Executors.newSingleThreadExecutor());
			root = N5TreeNode.fromFlatList("", datasets, "/" );
			return buildHelper( n5, root );
			
//			if( n5 instanceof AbstractGsonReader )
//			{
//				Optional<HashMap<String, JsonElement>> attrs = N5TreeNode.getMetadataMapJson( (AbstractGsonReader)n5, root.getPath());
//			}
//			else
//			{
//				System.err.println("container metadata only implemented for GsonReaders");
//				return null;
//			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static ContainerMetadataNode buildHelper( final AbstractGsonReader n5, N5TreeNode baseNode )
	{
		final Optional<HashMap<String, JsonElement>> attrs = N5TreeNode.getMetadataMapJson( n5, baseNode.getPath());
		final List<N5TreeNode> children = baseNode.childrenList();

		final HashMap<String, ContainerMetadataNode > childMap = new HashMap<>();
		for( N5TreeNode child : children )
			childMap.put( child.getNodeName(), buildHelper( n5, child));

		if( attrs.isPresent() )
			return new ContainerMetadataNode( attrs.get(), childMap);
		else
			return new ContainerMetadataNode( new HashMap<>(), childMap);
	}
	
	public static void main( String[] args ) throws IOException, InterruptedException, ExecutionException
	{
//		N5FSReader n5 = new N5FSReader("/home/john/tmp/assorted.n5");
//		N5FSReader n5 = new N5FSReader("/home/john/tmp/t1-head.n5");
		N5FSReader n5 = new N5FSReader("/home/john/tmp/containerTranslation.n5");

		ContainerMetadataNode node = ContainerMetadata.build( n5 );
		System.out.println( node );
		System.out.println( " " );

		final Gson gson = n5.getGson();
		String jsonString = gson.toJson( node );
		System.out.println( jsonString );
		System.out.println( " " );

//		ContainerMetadataNode nodeParsed = gson.fromJson( jsonString, ContainerMetadataNode.class );
//		System.out.println( nodeParsed );
//
////		Optional<ContainerMetadataNode> childNode = nodeParsed.getChild("volumes/raw/c0", "/" );
//		Optional<ContainerMetadataNode> childNode = nodeParsed.getChild("c0/s1", "/" );
//		System.out.println( childNode );

		StringBuffer translation = new StringBuffer();
		translation.append(FinalTranslations.ISATTRIBUTESFUN + "\n");
		translation.append( FinalTranslations.N5VFUNS + "\n");
		translation.append( FinalTranslations.MULTISCALEFUNS + "\n");
		translation.append( "n5vToTransformAll | addPaths |  walk ( if isChannel then . |= addMultiscale else . end ) " );
		
		System.out.println( translation.toString() );

		final String jsonTranslated = JqExps.translate( jsonString, translation.toString() );
		System.out.println( "" );
		System.out.println( jsonTranslated );
		System.out.println( "" );

		ContainerMetadataNode rootParsed = gson.fromJson( jsonTranslated, ContainerMetadataNode.class );
		System.out.println( "" );
		System.out.println( rootParsed );
		System.out.println( "" );

		SpatialMetadataTemplateParser parser = new SpatialMetadataTemplateParser( gson, "" );
		Optional<SpatialMetadataTemplate> spatialMetadataOpt = parser.parseFromMap(gson, rootParsed.getChild("c0/s0", "/").get().getAttributes() );

		SpatialMetadataTemplate spatialMetadata = spatialMetadataOpt.get();
		System.out.println( spatialMetadata );

		System.out.println("done");
	}

}
