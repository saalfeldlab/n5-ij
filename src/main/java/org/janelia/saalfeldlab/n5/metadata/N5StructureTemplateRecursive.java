package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class N5StructureTemplateRecursive
{
	public String name;
	public String attributes;
	public Map<String,N5StructureTemplateRecursive> children;

	public N5StructureTemplateRecursive( 
			final String name,
			final String attributes,
			final Map<String,N5StructureTemplateRecursive> children )
	{
		this.name = name;
		this.attributes = attributes;
		this.children = children;
	}

	public static void main( String[] args ) throws IOException
	{
		String path = "/home/john/dev/json/jq_Examples/sample_multiscale.n5/n5Structure.json";
		String jsonString = new String(Files.readAllBytes(Paths.get( path )));
		
//		N5StructureTemplate n5Structure = new Gson().fromJson( jsonString, N5StructureTemplate.class );
//		System.out.println( n5Structure );
	
		MultiscaleDatasetsTemplate.MultiscaleDataset[] list = 
				new MultiscaleDatasetsTemplate.MultiscaleDataset[] {
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s0", new double[] { 1,1,1 }),
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s1", new double[] { 2,2,2 }),
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s2", new double[] { 4,4,4 }),
				};

		HashMap msmap = new HashMap<>();
		msmap.put( "raw", list );
		
		MultiscaleDatasetsTemplate mst = new MultiscaleDatasetsTemplate( msmap );
		System.out.println( new Gson().toJson( mst ));

		System.out.println("done");
	}
}