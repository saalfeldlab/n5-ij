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

import com.amazonaws.services.s3.AmazonS3;
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
import org.janelia.saalfeldlab.n5.dataaccess.s3.AmazonS3ClientBuilderWithDefaultCredentials;
import org.janelia.saalfeldlab.n5.dataaccess.s3.AmazonS3ClientBuilderWithDefaultRegion;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageReader;
import org.janelia.saalfeldlab.n5.googlecloud.N5GoogleCloudStorageWriter;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
		case AMAZON_S3:

			AmazonS3 s3tmp = null;
			DataAccessException exception = new DataAccessException( "" );
			try
			{
				s3tmp = AmazonS3ClientBuilderWithDefaultCredentials.create();
			}
			catch ( DataAccessException e )
			{
				exception = e;
			}

			if ( s3tmp == null )
			{
				System.out.println( "fall back to default region, no credentials" );
				s3tmp = AmazonS3ClientBuilderWithDefaultRegion.create();
			}
			if ( s3tmp == null )
				throw exception;

			s3 = s3tmp;
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
