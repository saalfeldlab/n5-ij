package org.janelia.saalfeldlab.n5.demos;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class TestGCloudServiceAcctAuth
{
	
	private Storage storage;

	public TestGCloudServiceAcctAuth()
	{
		InputStream credsStream = this.getClass().getResourceAsStream("n5testproject-65b45b8b12bd.json");
		GoogleCredentials credentials;
		try {
			credentials = GoogleCredentials.fromStream( credsStream );
			storage = StorageOptions.newBuilder().setCredentials( credentials ).build().getService();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String[] args ) throws Exception
	{
		new TestGCloudServiceAcctAuth().runTest();
	}
	
	public void runTest()
	{
		System.out.println( "Reading.." );
		Blob b = storage.get( BlobId.of( "example_multi-n5_bucket", "position.n5/attributes.json" ) );
		System.out.println( "Blob:" + b );
	}


}
