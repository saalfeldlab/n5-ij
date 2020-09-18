package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class N5StructureTemplate
{
	public final Map<String,String> datasets;

	public Map< String, N5MultiscaleDatasetTemplate[] > multiscaleDatasets;

	public N5StructureTemplate(
			final Map<String,String> datasets,
			final Map< String, N5MultiscaleDatasetTemplate[] > multiscaleDatasets )
	{
		this.datasets = datasets;
		this.multiscaleDatasets = multiscaleDatasets;
	}

	public static void main( String[] args ) throws IOException
	{
		String path = "/home/john/dev/json/jq_Examples/sample_multiscale.n5/n5Structure.json";
		String jsonString = new String(Files.readAllBytes(Paths.get( path )));
		
		N5StructureTemplate n5Structure = new Gson().fromJson( jsonString, N5StructureTemplate.class );
		System.out.println( n5Structure );
		System.out.println( n5Structure.datasets );
		System.out.println( n5Structure.multiscaleDatasets );

		N5MultiscaleDatasetTemplate[] msDsets = n5Structure.multiscaleDatasets.get( "/volumes/raw" );
		for( N5MultiscaleDatasetTemplate msd : msDsets )
			System.out.println( msd );
		
//		for( String k : n5Structure.datasets.keySet() )
//			System.out.println( String.format( "%s : %s", k, n5Structure.datasets.get( k ) ));

		System.out.println("done");
	}
}