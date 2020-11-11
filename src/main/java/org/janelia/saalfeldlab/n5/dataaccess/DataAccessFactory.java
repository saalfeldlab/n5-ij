/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.dataaccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;
import com.google.gson.GsonBuilder;

import ij.gui.GenericDialog;

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
		// TODO replace or combine with N5Factory in n5-utils
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

			final DefaultAWSCredentialsProviderChain credProverChain = new DefaultAWSCredentialsProviderChain();
			try
			{
				credentials = Optional.of( credProverChain.getCredentials() );
			}
			catch( final Exception e )
			{
				System.out.println( "Could not load AWS credentials, falling back to anonymous." );
			}

			AmazonS3URI uri = null;
			if( path != null && !path.isEmpty() )
			{
				uri = new AmazonS3URI( path );
				region = Optional.ofNullable( uri.getRegion() );
			}

			if( region.isPresent() )
			{
				s3 = AmazonS3ClientBuilder.standard()
					.withCredentials( new AWSStaticCredentialsProvider( credentials.orElse( new AnonymousAWSCredentials() )))
					.withRegion( region.map( Regions::fromName ).orElse( Regions.US_EAST_1 ))
					.build();
			}
			else
			{
				s3 = AmazonS3ClientBuilder.standard()
					.withCredentials( new AWSStaticCredentialsProvider( credentials.orElse( new AnonymousAWSCredentials() )))
					.withRegion( Regions.US_EAST_1 ) // the region will be immediately changed below, if the bucket is elsewhere
					.build();

				try
				{
					s3.doesObjectExist(  uri.getBucket(), "testObjectKey" );
				}
				catch( AmazonS3Exception e )
				{
					// change region to the one this bucket lives in
					String regionName = awsRegionFromError( e );
					if( regionName != null )
						s3.setRegion( RegionUtils.getRegion( regionName ));
					else
						e.printStackTrace();
				}
			}

			googleCloudStorage = null;
			break;
		case GOOGLE_CLOUD:
			s3 = null;
			googleCloudStorage = new GoogleCloudStorageClient().create();

			break;
		default:
			throw new NotImplementedException( "Factory for type " + type + " is not implemented" );
		}
	}

	public static String awsRegionFromError( AmazonS3Exception e )
	{
		String errorMessage = e.getMessage();
		if( errorMessage.startsWith( "The bucket is in this region:" ))
		{
			String s = errorMessage.substring( 
					errorMessage.indexOf( ':' ) + 1,
					errorMessage.indexOf( '.' )).trim();
			return s;
		}
		return null;
	}

	public static String googleCloudProjectDialog()
	{
		final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();
		final Iterator< Project > projectsIterator = resourceManager.list().iterateAll().iterator();

		final ArrayList<String> stringList = new ArrayList<>();
		projectsIterator.forEachRemaining( x -> stringList.add( x.getName() ));

		final GenericDialog dialog = new GenericDialog( "Select google cloud project" );
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

			final String bucket = googleCloudUri.getBucket();
			final String container = googleCloudUri.getKey();

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

			final String bucket = googleCloudUri.getBucket();
			final String key = googleCloudUri.getKey();
			return new N5GoogleCloudStorageWriter( googleCloudStorage, bucket, key, gsonBuilder );
		default:
			throw new NotImplementedException( "Factory for type " + type + " is not implemented" );
		}
	}
}
