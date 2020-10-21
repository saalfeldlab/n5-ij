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

import java.io.File;
import java.net.URI;

import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

import com.amazonaws.services.s3.AmazonS3URI;

public enum DataAccessType
{
	FILESYSTEM,
	AMAZON_S3,
	GOOGLE_CLOUD,
	HDF5,
	ZARR;

	/**
	 * Detects the type of N5 container for readers
	 * @param link the link or path
	 * @return the container type
	 */
	public static DataAccessType detectType( final String link )
	{

		// check if it is a valid directory path
		final File f = new File( link );
		if ( f.isDirectory() )
		{
			if ( link.contains( ".n5" ) )
				return FILESYSTEM;
			else if ( link.contains( ".zarr" ) )
				return ZARR;
			else if ( new File( f, "attributes.json" ).exists() )
				return FILESYSTEM;
			else if ( 	new File( f, ".zarray" ).exists() ||
						new File( f, ".zgroups" ).exists() ||
						new File( f, ".zattrs" ).exists() )
			{
				return ZARR;
			}
//			else
//				search parent folders?
		}

		if ( link.contains( ".h5" ) || link.contains( ".hdf5" ) || link.contains( ".hdf" ) )
		{
			if( new File( N5Importer.h5DatasetPath( link, true )).exists() )
				return HDF5;
		}

		final URI uri;
		try
		{
			uri = URI.create( link );
		}
		catch ( final IllegalArgumentException e )
		{
			// not a valid input
			return null;
		}

		// try parsing as S3 link
		AmazonS3URI s3Uri;
		try
		{
			s3Uri = new AmazonS3URI( uri );
		}
		catch ( final Exception e )
		{
			s3Uri = null;
		}
		if ( s3Uri != null )
			return AMAZON_S3;

		// try parsing as Google Cloud link
		GoogleCloudStorageURI googleCloudUri;
		try
		{
			googleCloudUri = new GoogleCloudStorageURI( uri );
		}
		catch ( final Exception e )
		{
			googleCloudUri = null;
		}
		if ( googleCloudUri != null )
			return GOOGLE_CLOUD;

		// not a valid input
		return null;
	}
}
