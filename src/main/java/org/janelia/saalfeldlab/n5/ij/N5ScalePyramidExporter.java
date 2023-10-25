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
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
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
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
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

  private final Map<String, N5MetadataWriter<?>> styles;

  private ImageplusMetadata<?> impMeta;

  private N5MetadataSpecDialog metaSpecDialog;

  private final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

  // consider something like this eventually
//  private BiFunction<RandomAccessibleInterval<? extends NumericType<?>>,long[],RandomAccessibleInterval<?>> downsampler;

  public N5ScalePyramidExporter() {

	styles = new HashMap<String, N5MetadataWriter<?>>();
	styles.put(N5Importer.MetadataOmeZarrKey, new OmeNgffMetadataParser());
	styles.put(N5Importer.MetadataN5ViewerKey, new N5SingleScaleMetadataParser());
	styles.put(N5Importer.MetadataN5CosemKey, new N5CosemMetadataParser());

	// default image plus metadata writers
	impMetaWriterTypes = new HashMap<Class<?>, ImageplusMetadata<?>>();
	impMetaWriterTypes.put(ImagePlusLegacyMetadataParser.class, new ImagePlusLegacyMetadataParser());
	impMetaWriterTypes.put(N5CosemMetadataParser.class, new CosemToImagePlus());
	impMetaWriterTypes.put(N5SingleScaleMetadataParser.class, new N5ViewerToImagePlus());
	impMetaWriterTypes.put(OmeNgffMetadataParser.class, new NgffToImagePlus());

  }

	public static void main(String[] args) {


		final ImageJ ij = new ImageJ();
//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mitosis-xyct.tif");

		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mri-stack_mm.tif" );
		final String root = "/home/john/tmp/mri-test.n5";

//		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mitosis.tif" );
//		final String root = "/home/john/tmp/mitosis-test.n5";

//		final String metaType = N5Importer.MetadataN5ViewerKey;
//		final String metaType = N5Importer.MetadataN5CosemKey;
//		final String metaType = N5Importer.MetadataImageJKey;
		final String metaType = N5Importer.MetadataOmeZarrKey;

//		final String dset = String.format("%s_%d", metaType, nScales);
		final String dset = metaType;

		final N5ScalePyramidExporter exp = new N5ScalePyramidExporter();
		exp.setOptions(imp, root, dset, "64,64,16", metaType, "gzip" );
		exp.run();

	}

  public void setOptions(
		  final ImagePlus image,
		  final String n5RootLocation,
		  final String n5Dataset,
		  final String blockSizeArg,
		  final String metadataStyle,
		  final String compression) {

	this.image = image;
	this.n5RootLocation = n5RootLocation;

	this.n5Dataset = n5Dataset;

	this.blockSizeArg = blockSizeArg;
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
	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> void processMultiscale() throws IOException, InterruptedException, ExecutionException {

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

		long[] downsamplingFactors = initDownsampleFactors(baseImg.numDimensions());
		currentTranslation = new double[ downsamplingFactors.length];

		// get the metadata
		final M baseMetadata = (M)impMeta.readMetadata(image);
		M currentMetadata;

		final ArrayList<M> allMetadata = new ArrayList<>();

		// loop over scale levels
		final RandomAccessibleInterval<T> currentImg = baseImg;

		final int maxNumScales = 31;  // we will stop early though
		for( int s = 0; s < maxNumScales; s++ ) {

			System.out.println("writing scale: " + s);

			currentBlockSize = new int[ blockSize.length ];
			System.arraycopy(blockSize, 0, currentBlockSize, 0, blockSize.length);

			// channel splitting may modify currentBlockSize
			final M channelMetadata = baseMetadata;
			final List<RandomAccessibleInterval<T>> channelImgs = splitChannels(channelMetadata, currentImg);

			boolean lastScale = false;
			// for each channel (if present)
			for (int c = 0; c < channelImgs.size(); c++) {

				RandomAccessibleInterval<T> currentChannelImg = channelImgs.get(c);
				downsamplingFactors = getDownsampleFactors(channelMetadata, currentChannelImg.numDimensions(), s, downsamplingFactors);

				// update metadata to reflect this scale level
				currentMetadata = metadataForThisScale(channelMetadata, downsamplingFactors);

				// downsample when relevant
				if( s > 0 )
					currentChannelImg = downsampleMethod( currentChannelImg, downsamplingFactors );

				System.out.println("current block size : " + Arrays.toString(currentBlockSize));
				System.out.println("current channel img: " + Intervals.toString(currentChannelImg));

				// write to the appropriate dataset
				final String dset = getDatasetName( currentMetadata, c, s );
				write( currentChannelImg, n5, dset, compression, currentMetadata, metadataWriter );
				allMetadata.add(currentMetadata);

				// every channel shuld really be the same size, but
				// break this loop if any channel is small enough
				lastScale = lastScale || lastScale( currentBlockSize, currentChannelImg );
			}

			if( lastScale )
				break;
		}

		System.out.println("finished process multiscale");
	}

	protected boolean lastScale(final int[] blockSize, final Interval imageDimensions) {

		for (int i = 0; i < imageDimensions.numDimensions(); i++) {
			if (imageDimensions.dimension(i) <= blockSize[i])
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M metadataForThisScale(final M baseMetadata, final long[] downsamplingFactors) {

		if (baseMetadata instanceof SpatialModifiable) {
			return (M)(((SpatialModifiable)baseMetadata).modifySpatialTransform(
					Arrays.stream(downsamplingFactors).mapToDouble(x -> (double)x).toArray(), currentTranslation));
		}

		System.err.println("WARNING: metadata not spatial modifiable");
		return baseMetadata;
	}

//	/**
//	 * Calculates the number of scales such that the largest axis in physical dimension
//	 * is small than or equal to the size of a block.
//	 *
//	 * @return the number of scales
//	 */
//	protected int generateNumberOfScales()
//	{
//		// TODO implement me
//		return -1;
//	}

	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> downsampleMethod(RandomAccessibleInterval<T> img, long[] factors) {

		// TODO handle this case
		// if( downsampleMethod.equals(DOWN_AVG))

		return downsample(img, factors);
	}

	protected <M extends N5Metadata> String getDatasetName(final M metadata, final int channelIndex, final int scale) {

		if (image.getNChannels() > 1 &&
				(metadataStyle.equals(N5Importer.MetadataN5ViewerKey) ||
				 metadataStyle.equals(N5Importer.MetadataN5CosemKey))) {

			return n5Dataset + String.format("/c%d/s%d", channelIndex, scale);
		} else
			return n5Dataset + String.format("/s%d", scale);
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

		Axis[] axes;
		if( metadata instanceof AxisMetadata )
			axes = ((AxisMetadata)metadata).getAxes();
		else if( metadata instanceof N5SingleScaleMetadata )
			axes = AxisUtils.defaultN5ViewerAxes( (N5SingleScaleMetadata)metadata ).getAxes();
		else
			axes = AxisUtils.defaultAxes(nd);

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

	protected <M extends SpatialMetadata> long[] updateDownsampleFactors(final M metadata,
			final long[] downsampleFactors) {

		if (metadataStyle.equals(NONE) ||
				metadataStyle.equals(N5Importer.MetadataCustomKey) ||
				metadataStyle.equals(N5Importer.MetadataOmeZarrKey) ) {

		}

		// TODO implement me
		// TODO maybe these should modify the input factors instead?
		return new long[]{2 * downsampleFactors[0], 2 * downsampleFactors[1], 2 * downsampleFactors[2], 1};
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

	private  <T extends RealType<T> & NativeType<T> > void writeOmeZarr(
			final int numScales ) throws IOException, InterruptedException, ExecutionException {

		final N5Writer n5 = new N5Factory()
				.gsonBuilder(OmeNgffMetadataParser.gsonBuilder())
				.openWriter(n5RootLocation);

		final Compression compression = getCompression();
		parseBlockSize();

		final N5MetadataWriter<NgffSingleScaleAxesMetadata> writer = new NgffSingleScaleMetadataParser();

		final NgffToImagePlus metaIo = new NgffToImagePlus();
		final NgffSingleScaleAxesMetadata baseMeta = metaIo.readMetadata(image);

		// check and warn re: RGB image if relevant
		// if (image.getType() == ImagePlus.COLOR_RGB && !(writer instanceof
		// N5ImagePlusMetadata))
		// log.warn("RGB images are best saved using ImageJ metatadata. Other
		// choices "
		// + "may lead to unexpected behavior.");

		final Img<T> img = ImageJFunctions.wrap(image);
		write(img, n5, n5Dataset + "/s0", compression, null, null);

		final DatasetAttributes[] dsetAttrs = new DatasetAttributes[numScales];
		final OmeNgffDataset[] msDatasets = new OmeNgffDataset[numScales];

		String relativePath = String.format("s%d", 0);
		String dset = String.format("%s/%s", n5Dataset, relativePath);
		dsetAttrs[0] = n5.getDatasetAttributes(dset);
		final boolean cOrder = OmeNgffMultiScaleMetadata.cOrder(dsetAttrs[0]);

		final double[] scale = OmeNgffMultiScaleMetadata.reverseIfCorder(dsetAttrs[0], baseMeta.getScale());
		final double[] translation = OmeNgffMultiScaleMetadata.reverseIfCorder(dsetAttrs[0], baseMeta.getTranslation());
		final Axis[] axes = OmeNgffMultiScaleMetadata.reverseIfCorder(dsetAttrs[0], baseMeta.getAxes() );
		final NgffSingleScaleAxesMetadata s0Meta = new NgffSingleScaleAxesMetadata( dset, scale, translation, axes, dsetAttrs[0]);

		msDatasets[0] = new OmeNgffDataset();
		msDatasets[0].path = relativePath;
		msDatasets[0].coordinateTransformations = s0Meta.getCoordinateTransformations();

		try {
			writer.writeMetadata(s0Meta, n5, dset );
		} catch (final Exception e1) { }

		final long[] downsamplingFactors = new long[img.numDimensions()];
		Arrays.fill( downsamplingFactors, 1 );
		for (int i = 1; i < numScales; i++) {

			final long[] factors = MetadataUtils.updateDownsamplingFactors(2, downsamplingFactors, Intervals.dimensionsAsLongArray(img), baseMeta.getAxisTypes());
			final RandomAccessibleInterval<T> imgDown = downsample(img, factors);
			relativePath = String.format("s%d", i);
			dset = String.format("%s/%s", n5Dataset, relativePath);

			write(imgDown, n5, dset, compression, null, null);

			dsetAttrs[i] = n5.getDatasetAttributes(dset);
			final NgffSingleScaleAxesMetadata siMeta = new NgffSingleScaleAxesMetadata( dset,
					OmeNgffMultiScaleMetadata.reverseIfCorder(dsetAttrs[0], MetadataUtils.mul(baseMeta.getScale(), downsamplingFactors)),
					OmeNgffMultiScaleMetadata.reverseIfCorder(dsetAttrs[0], baseMeta.getTranslation()),
					axes,
					dsetAttrs[i]);

			try {
				writer.writeMetadata(siMeta, n5, dset );
			} catch (final Exception e1) { }

			msDatasets[i] = new OmeNgffDataset();
			msDatasets[i].path = relativePath;
			msDatasets[i].coordinateTransformations = siMeta.getCoordinateTransformations();
		}

		final OmeNgffMultiScaleMetadata ms = NgffToImagePlus.buildMetadata( s0Meta, image.getTitle(), n5Dataset, dsetAttrs, msDatasets);
		final OmeNgffMultiScaleMetadata[] msList = new OmeNgffMultiScaleMetadata[]{ms};

		final OmeNgffMetadata meta = new OmeNgffMetadata(n5Dataset, msList);
		try {
			new OmeNgffMetadataParser(cOrder).writeMetadata(meta, n5, n5Dataset);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		n5.close();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends RealType & NativeType, M extends N5Metadata> void write(
			final RandomAccessibleInterval<T> image,
			final N5Writer n5,
			final String dataset,
			final Compression compression, final M metadata, final N5MetadataWriter<M> writer)
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

		if( metadata != null )
			try {
				writer.writeMetadata(metadata, n5, dataset);
			} catch (final Exception e) {
			e.printStackTrace();
			}

//		writeMetadata(n5, dataset, writer);
	}

//	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsampleSimple(
//			final RandomAccessibleInterval<T> img, final int downsampleFactor) {
//		return Views.subsample(img, downsampleFactor);
//	}

	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsample(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {
		return Views.subsample(img, downsampleFactors);
	}

//	@SuppressWarnings({"rawtypes", "unchecked"})
//	private <T extends RealType & NativeType, M extends N5Metadata> void writeSplitChannels(
//			final N5Writer n5,
//			final Compression compression,
//			final M metadata,
//			final N5MetadataWriter<M> writer) throws IOException, InterruptedException, ExecutionException
//	{
//		final Img<T> img;
//		if( image.getType() == ImagePlus.COLOR_RGB )
//			img = (( Img< T > ) N5IJUtils.wrapRgbAsInt( image ));
//		else
//			img = ImageJFunctions.wrap(image);
//
//		String datasetString = "";
//		int[] blkSz = blockSize;
//		for (int c = 0; c < image.getNChannels(); c++) {
//			RandomAccessibleInterval<T> channelImg;
//			// If there is only one channel, img may be 3d, but we don't want to slice
//			// so if we have a 3d image check that the image is multichannel
//			if( image.getNChannels() > 1 )
//			{
//				channelImg = Views.hyperSlice(img, 2, c);
//
//				// if we slice the image, appropriately slice the block size also
//				blkSz = sliceBlockSize( 2 );
//			} else {
//				channelImg = img;
//			}
//
//			if (metadataStyle.equals(N5Importer.MetadataN5ViewerKey)) {
//				datasetString = String.format("%s/c%d/s0", n5Dataset, c);
//			} else if (image.getNChannels() > 1) {
//				datasetString = String.format("%s/c%d", n5Dataset, c);
//			} else {
//				datasetString = n5Dataset;
//			}
//
//			if( metadataStyle.equals(N5Importer.MetadataN5ViewerKey) && image.getNFrames() > 1 && image.getNSlices() == 1 )
//			{
//				// make a 4d image in order XYZT
//				channelImg = Views.permute(Views.addDimension(channelImg, 0, 0), 2, 3);
//				// expand block size
//				blkSz = new int[] { blkSz[0], blkSz[1], 1, blkSz[2] };
//			}
//
//			// use threadPool even for single threaded execution for progress monitoring
//			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
//			progressMonitor( threadPool );
//			N5Utils.save( channelImg, n5, datasetString, blkSz, compression, threadPool );
//			threadPool.shutdown();
//
////			writeMetadata(n5, datasetString, writer);
//			try {
//				writer.writeMetadata(metadata, n5, datasetString);
//			} catch (final Exception e) { }
//		}
//	}

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
