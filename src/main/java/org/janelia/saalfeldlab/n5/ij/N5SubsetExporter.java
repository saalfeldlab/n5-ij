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
package org.janelia.saalfeldlab.n5.ij;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "File>Save As>HDF5/N5/Zarr/OME-NGFF (patch)",
	description = "Insert the current image as a patch into an existing dataset at a user-defined offset. New datasets can be created and existing "
			+ "datsets can be extended.")
public class N5SubsetExporter extends ContextCommand {

//	@Parameter(visibility = ItemVisibility.MESSAGE, required = false)
//	private final String message = "Insert the current image into an existing dataset at a user-defined offset. New datasets can be created, and existing"
//			+ "datsets can be extended.";

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private UIService ui;

	@Parameter(label = "Image")
	private ImagePlus image; // or use Dataset? - maybe later

	@Parameter(label = "Root url")
	private String containerRoot;

	@Parameter(label = "Dataset", required = false, description = "This argument is ignored if the N5ViewerMetadata style is selected")
	private String dataset;

	@Parameter(label = "Thread count", required = true, min = "1", max = "256")
	private int nThreads = 1;

	@Parameter(label = "Offset", required = false, description = "The point in pixel units where the origin of this image will be written into the n5-dataset (comma-delimited)")
	private String subsetOffset;

	@Parameter(label = "Format", style = "listBox", choices = {N5ScalePyramidExporter.AUTO_FORMAT,
		N5ScalePyramidExporter.HDF5_FORMAT, N5ScalePyramidExporter.N5_FORMAT, N5ScalePyramidExporter.ZARR_FORMAT})
	private String storageFormat = N5ScalePyramidExporter.AUTO_FORMAT;

	@Parameter(label = "Chunk size", description = "The size of chunks to use if a new array is created. Comma separated, for example: \"64,32,16\".\n " +
			"You may provide fewer values than the data dimension. In that case, the size will "
			+ "be expanded to necessary size with the last value, for example \"64\", will expand " +
			"to \"64,64,64\" for 3D data.")
	private String chunkSizeArg = "64";

	@Parameter(label = "Compression", style = "listBox", description = "The compression type to use if a new array is created.",
			choices = {
			N5ScalePyramidExporter.GZIP_COMPRESSION,
			N5ScalePyramidExporter.RAW_COMPRESSION,
			N5ScalePyramidExporter.LZ4_COMPRESSION,
			N5ScalePyramidExporter.XZ_COMPRESSION,
			N5ScalePyramidExporter.BLOSC_COMPRESSION,
			N5ScalePyramidExporter.ZSTD_COMPRESSION})
	private String compressionArg = N5ScalePyramidExporter.GZIP_COMPRESSION;

	private long[] offset;

	public N5SubsetExporter() {}

	public N5SubsetExporter(final ImagePlus image, final String n5RootLocation, final String n5Dataset, final String subsetOffset) {

		setOptions(image, n5RootLocation, n5Dataset, subsetOffset);
	}

	public N5SubsetExporter(final ImagePlus image, final String n5RootLocation, final String n5Dataset, final long[] subsetOffset) {

		setOptions(image, n5RootLocation, n5Dataset, subsetOffset);
	}

	public static void main(final String[] args) {

//		final ImageJ ij = new ImageJ();
//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mitosis-xyct.tif");

//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mri-stack.tif");
//		final String root = "/home/john/tmp/mri-test.n5";

//		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mitosis.tif" );
//		final String root = "/home/john/tmp/mitosis-test.zarr";

		final ImagePlus imp = IJ.openImage( "/home/john/tmp/boats.tif");
		final String root = "/home/john/tmp/asdf.n5";
		final String dset = "a/b";

		final N5SubsetExporter exp = new N5SubsetExporter();
		exp.setOptions(imp, root, dset, "200,400");
		exp.run();
	}

	public void setOptions(final ImagePlus image, final String containerRoot, final String dataset, final String subsetOffset) {

		this.image = image;
		this.containerRoot = containerRoot;
		this.dataset = dataset;
		this.subsetOffset = subsetOffset;
	}

	public void setOptions(final ImagePlus image, final String containerRoot, final String dataset, final long[] subsetOffset) {

		this.image = image;
		this.containerRoot = containerRoot;
		this.dataset = dataset;
		this.offset = subsetOffset;
	}

	public void setOptions(final ImagePlus image, final String containerRoot, final String dataset, final String subsetOffset,
			final String chunkSizeArg, final String compression) {

		this.image = image;
		this.containerRoot = containerRoot;
		this.dataset = dataset;
		this.subsetOffset = subsetOffset;
		this.chunkSizeArg = chunkSizeArg;
		this.compressionArg = compression;
	}

	public void setOptions(final ImagePlus image, final String containerRoot, final String dataset, final long[] subsetOffset,
			final String chunkSizeArg, final String compression) {

		this.image = image;
		this.containerRoot = containerRoot;
		this.dataset = dataset;
		this.offset = subsetOffset;
		this.chunkSizeArg = chunkSizeArg;
		this.compressionArg = compression;
	}

	public void setOffset(final long[] offset) {

		this.offset = offset;
	}

	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> void process() throws IOException, InterruptedException, ExecutionException {

		final String rootWithFormatPrefix = N5ScalePyramidExporter.containerRootWithFormatPrefix(containerRoot, storageFormat, true);
		if (rootWithFormatPrefix == null)
			return;

		final N5Writer n5 = new N5Factory()
				.s3UseCredentials()
				.getWriter(containerRoot);
		write(n5);
		n5.close();
	}

	public void parseOffset() {

		if (this.offset != null)
			return;

		final int nd = image.getNDimensions();
		final String[] blockArgList = subsetOffset.split(",");
		final int[] dims = Intervals.dimensionsAsIntArray( ImageJFunctions.wrap( image ));

		offset = new long[nd];
		int i = 0;
		while (i < blockArgList.length && i < nd) {
			offset[i] = Integer.parseInt(blockArgList[i]);
			i++;
		}
		final int N = blockArgList.length - 1;

		while (i < nd) {
			if( offset[N] > dims[i] )
				offset[i] = dims[i];
			else
				offset[i] = offset[N];

			i++;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <T extends RealType & NativeType, M extends N5DatasetMetadata> void write(
			final N5Writer n5) throws IOException, InterruptedException, ExecutionException {

		parseOffset();

		final Img<T> ipImg;
		if (image.getType() == ImagePlus.COLOR_RGB)
			ipImg = (Img<T>)N5IJUtils.wrapRgbAsInt(image);
		else
			ipImg = ImageJFunctions.wrap(image);

		final IntervalView<T> rai = Views.translate(ipImg, offset);

		// create an empty dataset if it one does not exist
		if (!n5.datasetExists(dataset)) {
			final long[] dimensions = outputInterval(rai).dimensionsAsLongArray();
			final int[] blockSize = N5ScalePyramidExporter.parseBlockSize(chunkSizeArg, dimensions);
			final DatasetAttributes attributes = new DatasetAttributes(
					dimensions,
					blockSize,
					N5Utils.dataType((T)Util.getTypeFromInterval(rai)),
					N5ScalePyramidExporter.getCompression(compressionArg));

			n5.createDataset(dataset, attributes);
		}

		if (nThreads > 1)
			N5Utils.saveRegion(rai, n5, dataset);
		else {
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			progressMonitor(threadPool);
			N5Utils.saveRegion(rai, n5, dataset, threadPool);
			threadPool.shutdown();
		}
	}

	private Interval outputInterval(final Interval interval) {

		final int N = interval.numDimensions();
		final long[] min = new long[N];
		final long[] max = new long[N];
		for (int i = 0; i < N; i++) {
			min[i] = 0;
			if( interval.min(i) < 0 )
				max[i] = interval.dimension(i) - 1;
			else
				max[i] = interval.max(i);
		}

		return new FinalInterval(min, max);
	}

	@Override
	public void run() {

		try {
			process();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void progressMonitor( final ThreadPoolExecutor exec )
	{
		new Thread()
		{
			@Override
			public void run()
			{
				IJ.showProgress( 0.01 );
				try
				{
					Thread.sleep( 333 );
					boolean done = false;
					while( !done && !exec.isShutdown() )
					{
						final long i = exec.getCompletedTaskCount();
						final long N = exec.getTaskCount();
						done = i == N;
						IJ.showProgress( (double)i / N );
						Thread.sleep( 333 );
					}
				}
				catch ( final InterruptedException e ) { }
				IJ.showProgress( 1.0 );
			}
		}.start();
		return;
	}

}
