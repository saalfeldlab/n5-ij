/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.dataaccess;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

import ij.gui.GenericDialog;

import org.apache.commons.lang.NotImplementedException;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.dataaccess.googlecloud.GoogleCloudClientBuilderWithDefaultCredentials;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

public class DataAccessFactory
{
	private final DataAccessType type;
	private final AmazonS3 s3;
	private final Storage googleCloudStorage;
	private GoogleCloudStorageURI googleCloudUri;

	public DataAccessFactory( final DataAccessType type ) throws DataAccessException
	{
		this( type, "" );
	}

	public DataAccessFactory( final DataAccessType type, final String path ) throws DataAccessException
	{
		this.type = type;
		switch ( type )
		{
		case FILESYSTEM:
			s3 = null;
			googleCloudStorage = null;
			break;
		case ZARR:
			s3 = null;
			googleCloudStorage = null;
			break;
		case HDF5:
			s3 = null;
			googleCloudStorage = null;
			break;
		case AMAZON_S3:

			Optional<String> region = Optional.empty();
			Optional< AWSCredentials > credentials = Optional.empty();

			DefaultAWSCredentialsProviderChain credProverChain = new DefaultAWSCredentialsProviderChain();
			Exception credException = null;
			try
			{ 
				credentials = Optional.of( credProverChain.getCredentials() );
			}
			catch( Exception e )
			{
				System.out.println( "Could not load AWS credentials, falling back to anonymous." );
				credException = e;
			}

			AmazonS3URI uri = null;
			if( path != null && !path.isEmpty() )
			{
				uri = new AmazonS3URI( path );
				region = Optional.ofNullable( uri.getRegion() );
			}

			s3 = AmazonS3ClientBuilder.standard()
				.withCredentials( new AWSStaticCredentialsProvider( credentials.orElse( new AnonymousAWSCredentials() )))
				.withRegion( region.map( Regions::fromName ).orElse( Regions.US_EAST_1 ))
				.build();

			googleCloudStorage = null;
			break;
		case GOOGLE_CLOUD:
			s3 = null;
			googleCloudUri = new GoogleCloudStorageURI( path );

			String project = googleCloudUri.getProject() ;
			if( project == null || project.isEmpty() )
			{
				project = googleCloudProjectDialog();
			}

			if( project != null && !project.isEmpty() )
				googleCloudStorage = GoogleCloudClientBuilderWithDefaultCredentials.createStorage( project );
			else
				googleCloudStorage = GoogleCloudClientBuilderWithDefaultCredentials.createStorage();

			break;
		default:
			throw new NotImplementedException( "Factory for type " + type + " is not implemented" );
		}
	}
	
	public static String googleCloudProjectDialog()
	{
		final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();
		final Iterator< Project > projectsIterator = resourceManager.list().iterateAll().iterator();

		ArrayList<String> stringList = new ArrayList<>();
		projectsIterator.forEachRemaining( x -> stringList.add( x.getName() ));

		GenericDialog dialog = new GenericDialog( "Select google cloud project" );
		dialog.addChoice( "Project", stringList.toArray( new String[0]), stringList.get( 0 ) );

		dialog.showDialog();
		if ( dialog.wasCanceled() )
			return null;

		return dialog.getNextChoice();
	}
	
	public N5Reader createN5Reader( final String basePath ) throws IOException
	{
		final GsonBuilder gsonBuilder = N5Metadata.getGsonBuilder();
		switch ( type )
		{
		case FILESYSTEM:
			return new N5FSReader( basePath, gsonBuilder );
		case ZARR:
			return new N5ZarrReader( basePath, gsonBuilder );
		case HDF5:
			return new N5HDF5Reader( basePath, 64, 64, 64 );
		case AMAZON_S3:
			final AmazonS3URI s3Uri = new AmazonS3URI( basePath );
			return new N5AmazonS3Reader( s3, s3Uri.getBucket(), s3Uri.getKey(), gsonBuilder );
		case GOOGLE_CLOUD:

			if( googleCloudUri == null )
				googleCloudUri = new GoogleCloudStorageURI( basePath );

			String bucket = googleCloudUri.getBucket();
			String container = googleCloudUri.getKey();

			return new N5GoogleCloudStorageReader( googleCloudStorage, bucket, container, gsonBuilder );
		default:
			throw new NotImplementedException( "Factory for type " + type + " is not implemented" );
		}
	}

	public N5Writer createN5Writer( final String basePath ) throws IOException
	{
		final GsonBuilder gsonBuilder = N5Metadata.getGsonBuilder();
		switch ( type )
		{
		case FILESYSTEM:
			return new N5FSWriter( basePath, gsonBuilder );
		case ZARR:
			return new N5ZarrWriter( basePath, gsonBuilder );
		case HDF5:
			return new N5HDF5Writer( basePath, 64, 64, 64 );
		case AMAZON_S3:
			final AmazonS3URI s3Uri = new AmazonS3URI( basePath );
			return new N5AmazonS3Writer( s3, s3Uri.getBucket(), s3Uri.getKey(), gsonBuilder );
		case GOOGLE_CLOUD:
			if( googleCloudUri == null )
				googleCloudUri = new GoogleCloudStorageURI( basePath );

			String bucket = googleCloudUri.getBucket();
			String key = googleCloudUri.getKey();
			return new N5GoogleCloudStorageWriter( googleCloudStorage, bucket, key, gsonBuilder );
		default:
			throw new NotImplementedException( "Factory for type " + type + " is not implemented" );
		}
	}
}
