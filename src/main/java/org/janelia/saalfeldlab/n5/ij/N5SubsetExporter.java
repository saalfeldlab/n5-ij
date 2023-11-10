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

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "File>Save As>Export Subset HDF5/N5/Zarr")
public class N5SubsetExporter extends ContextCommand {

	public static final String GZIP_COMPRESSION = "gzip";
	public static final String RAW_COMPRESSION = "raw";
	public static final String LZ4_COMPRESSION = "lz4";
	public static final String XZ_COMPRESSION = "xz";
	public static final String BLOSC_COMPRESSION = "blosc";

	public static final String DOWN_SAMPLE = "Sample";
	public static final String DOWN_AVG = "Average";

	public static final String NONE = "None";

	public static final String NO_OVERWRITE = "No overwrite";
	public static final String OVERWRITE = "Overwrite";
	public static final String DELETE_OVERWRITE = "Delete and overwrite";
	public static final String WRITE_SUBSET = "Overwrite subset";

	public static enum OVERWRITE_OPTIONS {
		NO_OVERWRITE, OVERWRITE, WRITE_SUBSET
	}

	@Parameter(visibility = ItemVisibility.MESSAGE, required = false)
	private final String message = "Export an ImagePlus to an HDF5, N5, or Zarr container.";

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private UIService ui;

	@Parameter(label = "Image")
	private ImagePlus image; // or use Dataset? - maybe later

	@Parameter(label = "N5 root url")
	private String n5RootLocation;

	@Parameter(label = "Dataset", required = false, description = "This argument is ignored if the N5ViewerMetadata style is selected")
	private String n5Dataset;

	@Parameter(label = "Thread count", required = true, min = "1", max = "256")
	private int nThreads = 1;

	@Parameter(label = "Overwrite subset offset", required = false, description = "The point in pixel units where the origin of this image will be written into the n5-dataset (comma-delimited)")
	private String subsetOffset;

	private long[] offset;

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

	public void setOptions( final ImagePlus image, final String n5RootLocation, final String n5Dataset, final String subsetOffset) {

		this.image = image;
		this.n5RootLocation = n5RootLocation;
		this.n5Dataset = n5Dataset;
		this.subsetOffset = subsetOffset;
	}

	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> void process() throws IOException, InterruptedException, ExecutionException {

		final N5Writer n5 = new N5Factory().openWriter(n5RootLocation);
		write(n5);
		n5.close();
	}

	public void parseOffset() {

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
		if (!n5.datasetExists(n5Dataset))
			IJ.showMessage("No dataset found at: " + n5Dataset);

		final Img<T> ipImg;
		if (image.getType() == ImagePlus.COLOR_RGB)
			ipImg = (Img<T>)N5IJUtils.wrapRgbAsInt(image);
		else
			ipImg = ImageJFunctions.wrap(image);

		final IntervalView<T> rai = Views.translate(ipImg, offset);
		if (nThreads > 1)
			N5Utils.saveRegion( rai, n5, n5Dataset );
		else {
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			progressMonitor( threadPool );
			N5Utils.saveRegion( rai, n5, n5Dataset, threadPool);
			threadPool.shutdown();
		}
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
