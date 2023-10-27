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

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataSingleScaleParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadataMutable;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ViewerToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.NgffToImagePlus;
import org.janelia.saalfeldlab.n5.ui.N5MetadataSpecDialog;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Plugin(type = Command.class, menuPath = "File>Save As>Export HDF5/N5/Zarr scale pyramid")
public class N5ScalePyramidExporter extends ContextCommand implements WindowListener {

  public static final String GZIP_COMPRESSION = "gzip";
  public static final String RAW_COMPRESSION = "raw";
  public static final String LZ4_COMPRESSION = "lz4";
  public static final String XZ_COMPRESSION = "xz";
  public static final String BLOSC_COMPRESSION = "blosc";

  public static final String DOWN_SAMPLE = "Sample";
  public static final String DOWN_AVG = "Average";

  public static final String NONE = "None";

  @Parameter(visibility = ItemVisibility.MESSAGE, required = false)
  private final String message = "Export an ImagePlus to an HDF5, N5, or Zarr container."
  		+ "Read the documentation";

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

  @Parameter(
		  label = "Dataset",
		  required = false,
		  description = "This argument is ignored if the N5ViewerMetadata style is selected")
  private String n5Dataset;

  @Parameter(
		  label = "Block size",
		  description = "The size of blocks")
  private String blockSizeArg;

  @Parameter(
		  label = "Downsampling method",
		  choices = {DOWN_SAMPLE, DOWN_AVG})
  private String downsampleMethod = DOWN_SAMPLE;

  @Parameter(
		  label = "Compression",
		  choices = {GZIP_COMPRESSION, RAW_COMPRESSION, LZ4_COMPRESSION, XZ_COMPRESSION, BLOSC_COMPRESSION},
		  style = "listBox")
  private String compressionArg = GZIP_COMPRESSION;

  @Parameter(
		  label = "metadata type",
		  description = "The style for metadata to be stored in the exported N5.",
		  choices = {
				  N5Importer.MetadataOmeZarrKey,
				  N5Importer.MetadataN5ViewerKey,
				  N5Importer.MetadataN5CosemKey,
				  N5Importer.MetadataCustomKey,
				  NONE})
  private String metadataStyle = N5Importer.MetadataOmeZarrKey;

  @Parameter(label = "Thread count", required = true, min = "1", max = "999")
  private int nThreads = 1;

  private int[] blockSize;

  private int[] currentBlockSize;

  // the translation introduced by the downsampling method at the current scale level
  private double[] currentTranslation;

  private RandomAccessibleInterval<?> previousScaleImg;

  private ImageplusMetadata<?> impMeta;

  private N5MetadataSpecDialog metaSpecDialog;

  private final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

  private final Map<String, N5MetadataWriter<?>> styles;

  private final HashMap<Class<?>, N5MetadataWriter<?>> metadataWriters;

  // consider something like this eventually
  //  private BiFunction<RandomAccessibleInterval<? extends NumericType<?>>,long[],RandomAccessibleInterval<?>> downsampler;

  public N5ScalePyramidExporter() {

	styles = new HashMap<String, N5MetadataWriter<?>>();
	styles.put(N5Importer.MetadataOmeZarrKey, new OmeNgffMetadataParser());
	styles.put(N5Importer.MetadataN5ViewerKey, new N5SingleScaleMetadataParser());
	styles.put(N5Importer.MetadataN5CosemKey, new N5CosemMetadataParser());

	metadataWriters = new HashMap<Class<?>, N5MetadataWriter<?>>();
	metadataWriters.put(OmeNgffMetadata.class, new OmeNgffMetadataParser());
	metadataWriters.put(N5SingleScaleMetadata.class, new N5SingleScaleMetadataParser());
	metadataWriters.put(N5CosemMetadata.class, new N5CosemMetadataParser());
	metadataWriters.put(NgffSingleScaleAxesMetadata.class, new OmeNgffMetadataSingleScaleParser());

	// default image plus metadata writers
	impMetaWriterTypes = new HashMap<Class<?>, ImageplusMetadata<?>>();
	impMetaWriterTypes.put(ImagePlusLegacyMetadataParser.class, new ImagePlusLegacyMetadataParser());
	impMetaWriterTypes.put(N5CosemMetadataParser.class, new CosemToImagePlus());
	impMetaWriterTypes.put(N5SingleScaleMetadataParser.class, new N5ViewerToImagePlus());
	impMetaWriterTypes.put(OmeNgffMetadataParser.class, new NgffToImagePlus());

  }

	public static void main(String[] args) {



//		final ImageJ ij = new ImageJ();
//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mitosis-xyct.tif");

		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mri-stack_mm.tif" );
		final String root = "/home/john/tmp/mri-test.n5";

//		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mitosis.tif" );
//		final String root = "/home/john/tmp/mitosis-test.n5";

//		final String metaType = N5Importer.MetadataN5ViewerKey;
//		final String metaType = N5Importer.MetadataN5CosemKey;
		final String metaType = N5Importer.MetadataOmeZarrKey;

//		final String dsMethod = DOWN_SAMPLE;
		final String dsMethod = DOWN_AVG;

//		final String dset = String.format("%s_%d", metaType, nScales);
		final String dset = String.format("%s_%s", metaType, dsMethod);
//		final String dset = metaType;

		final N5ScalePyramidExporter exp = new N5ScalePyramidExporter();
		exp.setOptions(imp, root, dset, "64,64,1,2,16", dsMethod, metaType, "gzip" ); //mitosis
//		exp.setOptions(imp, root, dset, "64,64,16", dsMethod, metaType, "gzip" ); // mri
		exp.run();

//		final N5CosemMetadata cosem = new N5CosemMetadata("", new N5CosemMetadata.CosemTransform(
//				new String[] {"x"}, new double[]{1}, new double[]{0}, new String[]{"mm"}),
//				null);
//		System.out.println( exp.metadataWriters.get(cosem.getClass()).getClass() );

		System.exit(0);
	}

  public void setOptions(
		  final ImagePlus image,
		  final String n5RootLocation,
		  final String n5Dataset,
		  final String blockSizeArg,
		  final String downsampleMethod,
		  final String metadataStyle,
		  final String compression) {

	this.image = image;
	this.n5RootLocation = n5RootLocation;

	this.n5Dataset = n5Dataset;

	this.blockSizeArg = blockSizeArg;
	this.downsampleMethod = downsampleMethod;
	this.metadataStyle = metadataStyle;
	this.compressionArg = compression;
  }

	/**
	 * Set the custom metadata mapper to use programmically.
	 *
	 * @param metadataMapper the metadata template mapper
	 */
	public void setMetadataMapper(final MetadataTemplateMapper metadataMapper) {

		 styles.put(N5Importer.MetadataCustomKey, metadataMapper);
		 impMetaWriterTypes.put(MetadataTemplateMapper.class, new ImagePlusMetadataTemplate());
	}

	public void parseBlockSize() {

		final int nd = image.getNDimensions();
		final String[] blockArgList = blockSizeArg.split(",");
		final int[] dims = Intervals.dimensionsAsIntArray( ImageJFunctions.wrap( image ));

		blockSize = new int[nd];
		int i = 0;
		while (i < blockArgList.length && i < nd) {
			blockSize[i] = Integer.parseInt(blockArgList[i]);
			i++;
		}
		final int N = blockArgList.length - 1;

		while (i < nd) {
			if( blockSize[N] > dims[i] )
				blockSize[i] = dims[i];
			else
				blockSize[i] = blockSize[N];

			i++;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>>
		void processMultiscale() throws IOException, InterruptedException, ExecutionException {

		System.out.println("process multiscale");

		final N5Writer n5 = new N5Factory().openWriter(n5RootLocation);
		final Compression compression = getCompression();

		// TODO should have better behavior for block size parsing when splitting channels
		// initialize block size
		parseBlockSize();
		System.out.println("init block size: " + Arrays.toString(blockSize));

		currentBlockSize = new int[ blockSize.length ];
		System.arraycopy(blockSize, 0, currentBlockSize, 0, blockSize.length);

		N5MetadataWriter<M> metadataWriter = null;
		if (!metadataStyle.equals(NONE)) {
			metadataWriter = (N5MetadataWriter<M>)styles.get(metadataStyle);
			if (metadataWriter != null) {
				impMeta = impMetaWriterTypes.get(metadataWriter.getClass());
			}
		}

		// get the image to save
		final RandomAccessibleInterval<T> baseImg = getBaseImage();

		// initial block size, downsampliong factors, translation (offset)
		currentBlockSize = new int[ blockSize.length ];
		System.arraycopy(blockSize, 0, currentBlockSize, 0, blockSize.length);
		final long[] downsamplingFactors = initDownsampleFactors(baseImg.numDimensions());
		currentTranslation = new double[ downsamplingFactors.length];

		// get the metadata
		final M baseMetadata = (M)impMeta.readMetadata(image);
		final M channelMetadata = baseMetadata;
		M currentMetadata;

		// channel splitting may modify currentBlockSize
		final List<RandomAccessibleInterval<T>> channelImgs = splitChannels(channelMetadata, baseImg);
		for (int c = 0; c < channelImgs.size(); c++) {

			final String channelDataset = getChannelDatasetName(c);
			RandomAccessibleInterval<T> currentChannelImg = channelImgs.get(c);

			System.out.println("channel image sz: " + Intervals.toString(currentChannelImg));

			final N multiscaleMetadata = initializeMultiscaleMetadata(channelMetadata);

			// write scale levels
			final int maxNumScales = 31;  // we will stop early though
			for( int s = 0; s < maxNumScales; s++ ) {

				System.out.println("writing scale: " + s);

				final String dset = getScaleDatasetName(c, s);
//				newDownsamplingFactors = getDownsampleFactors(channelMetadata, currentChannelImg.numDimensions(), s, downsamplingFactors);
				final long[] relativeFactors = getRelativeDownsampleFactors(channelMetadata, currentChannelImg.numDimensions(), s, downsamplingFactors);

				// update absolute downsampling factors
				for( int i = 0; i < downsamplingFactors.length; i++ )
					downsamplingFactors[i] *= relativeFactors[i];

				System.out.println("factors: " + Arrays.toString(downsamplingFactors));

				// update metadata to reflect this scale level, returns new metadata instance
				currentMetadata = metadataForThisScale( dset, channelMetadata, downsamplingFactors);

				// downsample when relevant
				if( s > 0 ) {
					currentChannelImg = downsampleMethod((RandomAccessibleInterval<T>)getPreviousScaleImage(c, s), relativeFactors);
				}

				// write to the appropriate dataset
				write( currentChannelImg, n5, dset, compression, currentMetadata );
				storeScaleReference( c, s, currentChannelImg );

				updateMultiscaleMetadata( multiscaleMetadata, currentMetadata );

				if (lastScale(currentBlockSize, currentChannelImg))
					break;
			}

			writeMetadata( finalizeMultiscaleMetadata(channelDataset, multiscaleMetadata), n5, channelDataset );
		}

		System.out.println("finished process multiscale");
	}

	protected void storeScaleReference(final int channel, final int scale, final RandomAccessibleInterval<?> img) {

		previousScaleImg = img;
	}

	@SuppressWarnings("unchecked")
	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getPreviousScaleImage(final int channel, final int scale) {

		return (RandomAccessibleInterval<T>)previousScaleImg;
	}

	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> N initializeMultiscaleMetadata( M baseMetadata ) {

		if( !metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return null;

		return ((N)new OmeNgffMultiScaleMetadataMutable());
	}

	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> void updateMultiscaleMetadata( N multiscaleMetadata, M scaleMetadata ) {

		if( !metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return;

		if( multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable &&
			scaleMetadata instanceof NgffSingleScaleAxesMetadata) {

			final OmeNgffMultiScaleMetadataMutable ngffMs = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;
			ngffMs.addChild( (NgffSingleScaleAxesMetadata)scaleMetadata );
		}
	}

	protected < N extends SpatialMetadataGroup<?>> N finalizeMultiscaleMetadata( final String path, N multiscaleMetadata ) {

		if( !metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return multiscaleMetadata;

		if( multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable)
		{
			final OmeNgffMultiScaleMetadataMutable ms = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;

			final OmeNgffMultiScaleMetadata meta = new OmeNgffMultiScaleMetadata( ms.getAxes().length,
					path, path, downsampleMethod, "0.4",
					ms.getAxes(), ms.getDatasets(), null,
					ms.coordinateTransformations, ms.metadata);

			return ((N)new OmeNgffMetadata(path, new OmeNgffMultiScaleMetadata[] { meta }));
		}

		return multiscaleMetadata;
	}

	protected boolean lastScale(final int[] blockSize, final Interval imageDimensions) {

		for (int i = 0; i < imageDimensions.numDimensions(); i++) {
			if (imageDimensions.dimension(i) <= blockSize[i])
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M metadataForThisScale(final String newPath, final M baseMetadata, final long[] downsamplingFactors) {

		if (baseMetadata instanceof SpatialModifiable) {
			return (M)(((SpatialModifiable)baseMetadata).modifySpatialTransform(
					newPath,
					Arrays.stream(downsamplingFactors).mapToDouble(x -> (double)x).toArray(), currentTranslation));
		}

		System.err.println("WARNING: metadata not spatial modifiable");
		return baseMetadata;
	}

	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> downsampleMethod(final RandomAccessibleInterval<T> img,
			final long[] factors) {

		if (downsampleMethod.equals(DOWN_AVG))
			return downsampleAvgBy2(img, factors);
		else
			return downsample(img, factors);
	}

	protected <M extends N5Metadata> String getChannelDatasetName(final int channelIndex ) {

		if ( metadataStyle.equals(N5Importer.MetadataN5ViewerKey) ||
				( image.getNChannels() > 1 && metadataStyle.equals(N5Importer.MetadataN5CosemKey))) {

			return n5Dataset + String.format("/c%d", channelIndex );
		} else
			return n5Dataset;
	}

	protected <M extends N5Metadata> String getScaleDatasetName(final int channelIndex, final int scale) {

		return getChannelDatasetName(channelIndex ) +  String.format("/s%d", scale);
//		if ( metadataStyle.equals(N5Importer.MetadataN5ViewerKey) ||
//				( image.getNChannels() > 1 && metadataStyle.equals(N5Importer.MetadataN5CosemKey))) {
//
//			return n5Dataset + String.format("/c%d/s%d", channelIndex, scale);
//		} else
//			return n5Dataset + String.format("/s%d", scale);
	}

	/**
	 * Intialize the downsampling factors as ones. The first (zeroth?) scale is always at the original resolution.
	 *
	 * @param nd number of dimensions
	 * @return downsampling factors
	 */
	protected long[] initDownsampleFactors(final int nd) {

		final long[] factors = new long[nd];
		Arrays.fill(factors, 1);
		return factors;
	}

	protected <M extends N5Metadata> long[] getDownsampleFactors(final M metadata, final int nd, final int scale,
			final long[] downsampleFactors) {

		final Axis[] axes = getAxes(metadata, nd);

		// under what condisions is nd != axes.length
		final long[] factors = new long[axes.length];
		for( int i = 0; i < nd; i++ ) {

			// only downsample spatial dimensions
			if( axes[i].getType().equals(Axis.SPACE))
				factors[i] = 1 << scale; // 2 to the power of scale
			else
				factors[i] = 1;
		}

		return factors;
	}

	protected <M extends N5Metadata> long[] getRelativeDownsampleFactors(final M metadata, final int nd, final int scale,
			final long[] downsampleFactors) {

		final Axis[] axes = getAxes(metadata, nd);

		// under what condisions is nd != axes.length
		final long[] factors = new long[axes.length];
		for( int i = 0; i < nd; i++ ) {

			// only downsample spatial dimensions
			if( axes[i].getType().equals(Axis.SPACE))
				factors[i] = 2;
			else
				factors[i] = 1;
		}

		return factors;
	}

	protected <M extends N5Metadata> Axis[] getAxes(final M metadata, final int nd)
	{
		if( metadata instanceof AxisMetadata )
			return ((AxisMetadata)metadata).getAxes();
		else if( metadata instanceof N5SingleScaleMetadata )
			return AxisUtils.defaultN5ViewerAxes( (N5SingleScaleMetadata)metadata ).getAxes();
		else
			return AxisUtils.defaultAxes(nd);
	}

	// also extending NativeType causes build failures using maven, unclear why
//	protected <T extends NumericType<T> & NativeType<T>> RandomAccessibleInterval<T> getBaseImage() {
	@SuppressWarnings("unchecked")
	protected <T extends NumericType<T>> RandomAccessibleInterval<T> getBaseImage() {
		// TODO put logic checking for virtual image special cases here

		// get the image
		final RandomAccessibleInterval<T> baseImg;
		if (image.getType() == ImagePlus.COLOR_RGB)
			baseImg = (RandomAccessibleInterval<T>)(N5IJUtils.wrapRgbAsInt(image));
		else
			baseImg = (RandomAccessibleInterval<T>)ImageJFunctions.wrap(image);

		return baseImg;
	}

	/**
	 * If relevant, according to the passed {@link N5DatasetMetadata} metadata instance,
	 * return a list containing
	 */
	protected <T extends RealType<T> & NativeType<T>, M extends N5Metadata> List<RandomAccessibleInterval<T>> splitChannels(M metadata,
			RandomAccessibleInterval<T> img) {

		// TODO perhaps should return new metadata that is not

		// some metadata styles never split channels, return input image in that case
		if (metadataStyle.equals(NONE) || metadataStyle.equals(N5Importer.MetadataCustomKey) ||
				metadataStyle.equals(N5Importer.MetadataOmeZarrKey) ) {
			return Collections.singletonList(img);
		}

		// otherwise, split channels
		final ArrayList<RandomAccessibleInterval<T>> channels = new ArrayList<>();
		for (int c = 0; c < image.getNChannels(); c++) {

			RandomAccessibleInterval<T> channelImg;
			// If there is only one channel, img may be 3d, but we don't want to slice
			// so if we have a 3d image check that the image is multichannel
			if (image.getNChannels() > 1) {
				channelImg = Views.hyperSlice(img, 2, c);

				// if we slice the image, appropriately slice the block size also
				currentBlockSize = sliceBlockSize(2);
			} else {
				channelImg = img;
			}

			// make the image 4d and update the block size, if needed
			if (metadataStyle.equals(N5Importer.MetadataN5ViewerKey) && image.getNFrames() > 1 && image.getNSlices() == 1) {

				// make a 4d image in order XYZT
				channelImg = Views.permute(Views.addDimension(channelImg, 0, 0), 2, 3);
				// expand block size
				currentBlockSize = new int[]{currentBlockSize[0], currentBlockSize[1], 1, currentBlockSize[2]};
			}

			channels.add(channelImg);
		}
		return channels;
	}

	protected <M extends N5Metadata> void writeMetadata(final M metadata, final N5Writer n5, final String dataset) {

		if (metadata != null)
			Optional.ofNullable(metadataWriters.get(metadata.getClass())).ifPresent(writer -> {
				try {
					((N5MetadataWriter<M>)writer).writeMetadata(metadata, n5, dataset);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends RealType & NativeType, M extends N5Metadata> void write(
			final RandomAccessibleInterval<T> image,
			final N5Writer n5,
			final String dataset,
			final Compression compression, final M metadata )
			throws IOException, InterruptedException, ExecutionException {

		if ( n5.datasetExists(dataset)) {
			if (ui != null)
				ui.showDialog(String.format("Dataset (%s) already exists, not writing.", dataset));
			else
				System.out.println(String.format("Dataset (%s) already exists, not writing.", dataset));

			return;
		}

		// Here, either allowing overwrite, or not allowing, but the dataset does not exist.
		// use threadPool even for single threaded execution for progress monitoring
		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		progressMonitor(threadPool);
		N5Utils.save(image, n5, dataset, currentBlockSize, compression, Executors.newFixedThreadPool(nThreads));

		writeMetadata( metadata, n5, dataset );
	}

	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsample(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {
		return Views.subsample(img, downsampleFactors);
	}

	/**
	 * Downsamples an image by factors of 2 using averaging.
	 * <p>
	 * Not the most efficient when some dimensions are not downsampled.
	 *
	 * @param <T> the image data type
	 * @param img the image
	 * @param downsampleFactors the factors
	 * @return a downsampled image
	 */
	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsampleAvgBy2(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {

		// ensure downsampleFactors contains only 1's and 2's
		assert Arrays.stream(downsampleFactors).filter(x -> (x == 1) || (x == 2)).count() == downsampleFactors.length;

		final int nd = downsampleFactors.length;
		final double[] scale = new double[ nd ];
		final double[] translation = new double[ nd ];

		final long[] dims = new long[ nd ];

		for (int i = 0; i < nd; i++) {

			if (downsampleFactors[i] == 2) {
				scale[i] = 0.5;
				translation[i] = -0.25;
				dims[i] = (long)Math.ceil( img.dimension(i) / 2 );
			} else {
				scale[i] = 1.0;
				translation[i] = 0.0;
				dims[i] = img.dimension(i);
			}
		}

		// TODO clamping NLinearInterpFactory when relevant
		// TODO record offset in metadata as (s-0.5)
		final RealRandomAccessible<T> imgE = Views.interpolate(Views.extendBorder(img), new NLinearInterpolatorFactory());
		return Views.interval(RealViews.transform(imgE, new ScaleAndTranslation(scale, translation)),
				new FinalInterval(dims));
	}

	private int[] sliceBlockSize( final int exclude )
	{
		final int[] out = new int[ blockSize.length - 1 ];
		int j = 0;
		for( int i = 0; i < blockSize.length; i++ )
			if( i != exclude )
			{
				out[j] = blockSize[i];
				j++;
			}

		return out;
	}

	@Override
	public void run() {

		// add more options
		if (metadataStyle.equals(N5Importer.MetadataCustomKey)) {

			metaSpecDialog = new N5MetadataSpecDialog(this);
			metaSpecDialog.show(MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER);

		} else {

			try {
				processMultiscale();
			} catch (IOException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
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

	private Compression getCompression() {

		switch (compressionArg) {
		case GZIP_COMPRESSION:
			return new GzipCompression();
		case LZ4_COMPRESSION:
			return new Lz4Compression();
		case XZ_COMPRESSION:
			return new XzCompression();
		case RAW_COMPRESSION:
			return new RawCompression();
		case BLOSC_COMPRESSION:
			return new BloscCompression();
		default:
			return new RawCompression();
		}
	}

	@Override
	public void windowOpened(final WindowEvent e) {}

	@Override
	public void windowIconified(final WindowEvent e) {}

	@Override
	public void windowDeiconified(final WindowEvent e) {}

	@Override
	public void windowDeactivated(final WindowEvent e) {}

	@Override
	public void windowClosing(final WindowEvent e) {

	  styles.put(N5Importer.MetadataCustomKey, metaSpecDialog.getMapper());
	  impMetaWriterTypes.put(MetadataTemplateMapper.class, new ImagePlusMetadataTemplate());
	  try {
		processMultiscale();
	} catch (IOException | InterruptedException | ExecutionException e1) {
		e1.printStackTrace();
	}
	}

	@Override
	public void windowClosed(final WindowEvent e) {}

	@Override
	public void windowActivated(final WindowEvent e) {}

}
