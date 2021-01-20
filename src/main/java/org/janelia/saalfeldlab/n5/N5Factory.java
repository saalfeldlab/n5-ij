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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageURI;
import org.janelia.saalfeldlab.n5.Compression;
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

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.google.cloud.resourcemanager.Project;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;

import ch.systemsx.cisd.hdf5.HDF5Factory;

/**
*
* @author Igor Pisarev &lt;pisarevi@janelia.hhmi.org&gt;
* @author John Bogovic&lt;bogovicj@janelia.hhmi.org&gt;
* @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
*/
public class N5Factory {

	public static class N5Options {

		public final String containerPath;
		public final int[] blockSize;

		public N5Options(final String containerPath, final int[] blockSize )
		{
			this.containerPath = containerPath;
			this.blockSize = blockSize;
		}
	}

	private static enum N5AccessType
	{

		Reader, Writer
	}

	public static N5Reader createN5Reader( final N5Options options ) throws IOException
	{

		return createN5( options, N5AccessType.Reader );
	}

	public static N5Writer createN5Writer( final N5Options options ) throws IOException
	{

		return createN5( options, N5AccessType.Writer );
	}

	@SuppressWarnings("unchecked")
	private static <N5 extends N5Reader> N5 createN5(final N5Options options, final N5AccessType accessType) throws IOException {

		URI uri = null;
		try {
			uri = URI.create(options.containerPath);
		} catch (final IllegalArgumentException e) {}
		if (uri == null || uri.getScheme() == null) {
			if (isHDF5(options.containerPath, accessType))
				return (N5) createN5HDF5(options.containerPath, options.blockSize, accessType);
			else if (isZarr(options.containerPath))
				return (N5) createN5Zarr(options.containerPath, accessType);
			else
				return (N5) createN5FS(options.containerPath, accessType);
		}

		if (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https")) {
			// s3 uri parser is capable of parsing http links, try to parse it first as an s3 uri
			AmazonS3URI s3Uri;
			try {
				s3Uri = new AmazonS3URI(uri);
			} catch (final Exception e) {
				s3Uri = null;
			}

			if (s3Uri != null) {
				return (N5) createN5S3(options.containerPath, accessType);
			} else {
				// might be a google cloud link
				final GoogleCloudStorageURI googleCloudUri;
				try {
					googleCloudUri = new GoogleCloudStorageURI(uri);
				} catch (final Exception e) {
					throw new IllegalArgumentException("Expected either a local path or a link to AWS S3 bucket / Google Cloud Storage bucket.");
				}

				if (googleCloudUri.getBucket() == null || googleCloudUri.getBucket().isEmpty() || (googleCloudUri.getKey() != null && !googleCloudUri.getKey().isEmpty()))
					throw new IllegalArgumentException("N5 datasets on Google Cloud are stored in buckets. Please provide a link to a bucket.");
				return (N5) createN5GoogleCloud(options.containerPath, accessType);
			}
		} else {
			switch (uri.getScheme().toLowerCase()) {
			case "file":
				final String parsedPath = Paths.get(uri).toString();
				if (isHDF5(parsedPath, accessType))
					return (N5) createN5HDF5(parsedPath, options.blockSize, accessType);
				else if (isZarr(parsedPath))
					return (N5) createN5Zarr(parsedPath, accessType);
				else
					return (N5) createN5FS(parsedPath, accessType);
			case "s3":
				return (N5) createN5S3(options.containerPath, accessType);
			case "gs":
				return (N5) createN5GoogleCloud(options.containerPath, accessType);
			default:
				throw new IllegalArgumentException("Unsupported protocol: " + uri.getScheme());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <N5 extends N5FSReader> N5 createN5FS(final String containerPath, final N5AccessType accessType) throws IOException {

		switch (accessType) {
		case Reader:
			return (N5) new N5FSReader(containerPath);
		case Writer:
			return (N5) new N5FSWriter(containerPath);
		default:
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <N5 extends N5HDF5Reader> N5 createN5HDF5(final String containerPath, final int[] blockSize, final N5AccessType accessType) throws IOException {

		switch (accessType) {
		case Reader:
			return (N5) new N5HDF5Reader(HDF5Factory.openForReading(containerPath), blockSize);
		case Writer:
			return (N5) new N5HDF5Writer(HDF5Factory.open(containerPath), blockSize);
		default:
			return null;
		}
	}

	@SuppressWarnings( "unchecked" )
	private static <N5 extends N5ZarrReader> N5 createN5Zarr(final String containerPath, final N5AccessType accessType) throws IOException {

		switch (accessType) {
		case Reader:
			return (N5) new N5ZarrReader(containerPath, true);
		case Writer:
			return (N5) new N5ZarrWriter(containerPath, true);
		default:
			return null;
		}
	}

	private static boolean isHDF5(final String containerPath, final N5AccessType accessType) {

		switch (accessType) {
		case Reader:
			return Files.isRegularFile(Paths.get(containerPath));
		case Writer:
			return containerPath.toLowerCase().endsWith(".h5") || containerPath.toLowerCase().endsWith(".hdf5") || containerPath.toLowerCase().endsWith(".hdf");
		default:
			throw new RuntimeException();
		}
	}

	private static boolean isZarr(final String containerPath)
	{
		return containerPath.toLowerCase().endsWith(".zarr") ||
				(Files.isDirectory(Paths.get(containerPath)) &&
						(Files.isRegularFile(Paths.get(containerPath, ".zarray")) ||
								Files.isRegularFile(Paths.get(containerPath, ".zgroup"))));
	}

	@SuppressWarnings("unchecked")
	private static <N5 extends N5AmazonS3Reader> N5 createN5S3(final String containerPath, final N5AccessType accessType) throws IOException {

		AmazonS3 s3;
		try {
			s3 = AmazonS3ClientBuilder.standard().
					withCredentials(new ProfileCredentialsProvider()).build();
		} catch (final SdkClientException e) {
			try {
				s3 = AmazonS3ClientBuilder.defaultClient();
			} catch (final SdkClientException f) {
				final Region region = Regions.getCurrentRegion();
				s3 = AmazonS3ClientBuilder.standard().withRegion(region == null ? Regions.US_EAST_1.getName() : region.getName()).build();
			}
		}

		final AmazonS3URI s3Uri = new AmazonS3URI(containerPath);

		switch (accessType) {
		case Reader:
			return (N5) new N5AmazonS3Reader(s3, s3Uri);
		case Writer:
			return (N5) new N5AmazonS3Writer(s3, s3Uri);
		default:
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <N5 extends N5GoogleCloudStorageReader> N5 createN5GoogleCloud(final String containerPath, final N5AccessType accessType) throws IOException {

		final ResourceManager resourceManager = new GoogleCloudResourceManagerClient().create();

		final GoogleCloudStorageClient storageClient;
		switch (accessType) {
		case Reader:
			storageClient = new GoogleCloudStorageClient();
			break;
		case Writer:
			final Iterator<Project> projectsIterator = resourceManager.list().iterateAll().iterator();
			if (!projectsIterator.hasNext())
				return null;
			final String projectId = projectsIterator.next().getProjectId();

			storageClient = new GoogleCloudStorageClient(projectId);
			break;
		default:
			storageClient = null;
		}
		final Storage storage = storageClient.create();
		final GoogleCloudStorageURI googleCloudUri = new GoogleCloudStorageURI(containerPath);
		final String bucketName = googleCloudUri.getBucket();

		switch (accessType) {
		case Reader:
			return (N5) new N5GoogleCloudStorageReader(storage, bucketName);
		case Writer:
			return (N5) new N5GoogleCloudStorageWriter(storage, bucketName);
		default:
			return null;
		}
	}
}
