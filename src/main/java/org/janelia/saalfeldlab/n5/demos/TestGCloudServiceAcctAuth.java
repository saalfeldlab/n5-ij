package org.janelia.saalfeldlab.n5.demos;

import java.io.FileInputStream;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class TestGCloudServiceAcctAuth
{

	public static void main( String[] args ) throws Exception
	{
//		/home/john/googleCloud/n5testproject-65b45b8b12bd.json
		final String jsonPath = args[0];
		GoogleCredentials credentials = GoogleCredentials.fromStream( new FileInputStream( jsonPath ) );
		Storage storage = StorageOptions.newBuilder().setCredentials( credentials ).build().getService();

		System.out.println( "Reading.." );
		Blob b = storage.get( BlobId.of( "example_multi-n5_bucket", "position.n5/attributes.json" ) );
		System.out.println( "Blob:" + b );
		
	}


}
