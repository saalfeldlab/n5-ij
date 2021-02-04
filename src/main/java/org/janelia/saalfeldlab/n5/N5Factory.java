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
package org.janelia.saalfeldlab.n5;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Optional;

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
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

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

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

/**
 * Factory methods for various N5 readers and writers.  The factory methods
 * do not expose all parameters of each reader or writer to keep things
 * simple in this tutorial.  In particular, custom JSON adapters for not so
 * simple types are not considered, albeit incredibly useful.  Please inspect
 * the constructors of the readers and writers for further parameters.
 *
 * @author Stephan Saalfeld
 */
public interface N5Factory
{

	/**
	 * Helper method.
	 *
	 * @param url
	 * @return
	 */
	public static AmazonS3 createS3(final String url) {

		AmazonS3 s3;
		AWSCredentials credentials = null;
		try {
			credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
		} catch(final Exception e) {
			System.out.println( "Could not load AWS credentials, falling back to anonymous." );
		}
		final AWSStaticCredentialsProvider credentialsProvider =
				new AWSStaticCredentialsProvider(credentials == null ? new AnonymousAWSCredentials() : credentials);

		final AmazonS3URI uri = new AmazonS3URI(url);
		final Optional<String> region = Optional.ofNullable(uri.getRegion());

		if(region.isPresent()) {
			s3 = AmazonS3ClientBuilder.standard()
					.withCredentials(credentialsProvider)
					.withRegion(region.map(Regions::fromName).orElse(Regions.US_EAST_1))
					.build();
		} else {
			s3 = AmazonS3ClientBuilder.standard()
					.withCredentials(credentialsProvider)
					.withRegion(Regions.US_EAST_1)
					.build();
		}

		return s3;
	}

	/**
	 * Open an {@link N5Reader} for N5 filesystem.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static N5FSReader openFSReader(final String path) throws IOException {

		return new N5FSReader(path);
	}

	/**
	 * Open an {@link N5Reader} for Zarr.
	 *
	 * For more options of the Zarr backend study the {@link N5ZarrReader}
	 * constructors.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static N5ZarrReader openZarrReader(final String path) throws IOException {

		return new N5ZarrReader(path);
	}

	/**
	 * Open an {@link N5Reader} for HDF5. Close the reader when you do not need
	 * it any more.
	 *
	 * For more options of the HDF5 backend study the {@link N5HDF5Reader}
	 * constructors.
	 *
	 * @param path
	 * @param defaultBlockSize
	 * 		This block size will be used for reading non-chunked datasets.
	 * 		It is also possible to override the block-size for reading chunked
	 * 		datasets but we do not do that here as it's rarely useful.
	 * @return
	 * @throws IOException
	 */
	public static N5HDF5Reader openHDF5Reader(final String path, final int... defaultBlockSize) throws IOException {

		final IHDF5Reader hdf5Reader = HDF5Factory.openForReading(path);
		return new N5HDF5Reader(hdf5Reader, defaultBlockSize);
	}

	/**
	 * Open an {@link N5Reader} for Google Cloud.
	 *
	 * @param url
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	public static N5GoogleCloudStorageReader openGoogleCloudReader(final String url) throws IOException {

		final GoogleCloudStorageClient storageClient = new GoogleCloudStorageClient();
		final Storage storage = storageClient.create();
		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);
		final String bucketName = googleCloudUri.getBucket();
		return new N5GoogleCloudStorageReader(storage, bucketName);
	}

	/**
	 * Open an {@link N5Reader} for AWS S3.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static N5AmazonS3Reader openAWSS3Reader(final String url) throws IOException {

		return new N5AmazonS3Reader(createS3(url), new AmazonS3URI(url));
	}

	/**
	 * Open an {@link N5Writer} for N5 filesystem.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static N5FSWriter openFSWriter(final String path) throws IOException {

		return new N5FSWriter(path);
	}

	/**
	 * Open an {@link N5Writer} for Zarr.
	 *
	 * For more options of the Zarr backend study the {@link N5ZarrWriter}
	 * constructors.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static N5ZarrWriter openZarrWriter(final String path) throws IOException {

		return new N5ZarrWriter(path);
	}

	/**
	 * Open an {@link N5Writer} for HDF5.  Don't forget to close the writer
	 * after writing to close the file and make it available to other
	 * processes.
	 *
	 * For more options of the HDF5 backend study the {@link N5HDF5Writer}
	 * constructors.
	 *
	 * @param path
	 * @param defaultBlockSize
	 * 		This block size will be used for reading non-chunked datasets.
	 * 		It is also possible to override the block-size for reading non-
	 * 		chunked datasets but we do not do that here as it's rarely
	 * 		useful.
	 * @return
	 * @throws IOException
	 */
	public static N5HDF5Writer openHDF5Writer(final String path, final int... defaultBlockSize) throws IOException {

		final IHDF5Writer hdf5Writer = HDF5Factory.open(path);
		return new N5HDF5Writer(hdf5Writer, defaultBlockSize);
	}

	/**
	 * Open an {@link N5Writer} for Google Cloud.
	 *
	 * @param url
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	public static N5GoogleCloudStorageWriter openGoogleCloudWriter(final String url, final String projectId) throws IOException {

		final GoogleCloudStorageClient storageClient;
		if (projectId == null) {
			final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();
			final Iterator<Project> projectsIterator = resourceManager.list().iterateAll().iterator();
			if (!projectsIterator.hasNext())
				return null;
			storageClient = new GoogleCloudStorageClient(projectsIterator.next().getProjectId());
		} else
			storageClient = new GoogleCloudStorageClient(projectId);

		final Storage storage = storageClient.create();
		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(url);
		final String bucketName = googleCloudUri.getBucket();
		return new N5GoogleCloudStorageWriter(storage, bucketName);
	}

	/**
	 * Open an {@link N5Writer} for AWS S3.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static N5AmazonS3Writer openAWSS3Writer(final String url) throws IOException {

		return new N5AmazonS3Writer(createS3(url), new AmazonS3URI(url));
	}

	/**
	 * Open an {@link N5Reader} based on some educated guessing from the url.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static N5Reader openReader(final String url) throws IOException {

		try {
			final URI uri = new URI(url);
			final String scheme = uri.getScheme();
			if (scheme == null);
			else if (scheme.equals("s3"))
				return openAWSS3Reader(url);
			else if (scheme.equals("gs"))
				return openGoogleCloudReader(url);
			else if (scheme.equals("https") || scheme.equals("http")) {
				if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
					return openAWSS3Reader(url);
				else if (uri.getHost().matches(".*cloud\\.google\\.com"))
					return openGoogleCloudReader(url);
			}
		} catch (final URISyntaxException e) {}
		if (url.matches("(?i).*\\.(h5|hdf5|hdf)"))
			return openHDF5Reader(url, 64);
		else if (url.matches("(?i).*\\.zarr"))
			return openZarrReader(url);
		else
			return openFSReader(url);
	}

	/**
	 * Open an {@link N5Writer} based on some educated guessing from the url.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static N5Writer openWriter(final String url) throws IOException {

		try {
			final URI uri = new URI(url);
			final String scheme = uri.getScheme();
			if (scheme == null);
			else if (scheme.equals("s3"))
				return openAWSS3Writer(url);
			else if (scheme.equals("gs"))
				return openGoogleCloudWriter(url, null);
			else if (scheme.equals("https") || scheme.equals("http")) {
				if (uri.getHost().matches(".*s3\\.amazonaws\\.com"))
					return openAWSS3Writer(url);
				else if (uri.getHost().matches(".*cloud\\.google\\.com"))
					return openGoogleCloudWriter(url, null);
			}
		} catch (final URISyntaxException e) {}
		if (url.matches("(?i).*\\.(h5|hdf5|hdf)"))
			return openHDF5Writer(url, 64);
		else if (url.matches("(?i).*\\.zarr"))
			return openZarrWriter(url);
		else
			return openFSWriter(url);
	}
}
