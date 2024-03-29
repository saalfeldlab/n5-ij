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

import javax.swing.JOptionPane;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ViewerToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.NgffToImagePlus;
import org.janelia.saalfeldlab.n5.ui.N5MetadataSpecDialog;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.AbstractN5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialModifiable;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataSingleScaleParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadataMutable;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;
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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "File>Save As>HDF5/N5/Zarr/OME-NGFF ...", description = "Save the current image as a new dataset or multi-scale pyramid.")
public class N5ScalePyramidExporter extends ContextCommand implements WindowListener {

	public static final String GZIP_COMPRESSION = "gzip";
	public static final String RAW_COMPRESSION = "raw";
	public static final String LZ4_COMPRESSION = "lz4";
	public static final String XZ_COMPRESSION = "xz";
	public static final String BLOSC_COMPRESSION = "blosc";
	public static final String ZSTD_COMPRESSION = "zstd";

	public static final String AUTO_FORMAT = "Auto";
	public static final String HDF5_FORMAT = "HDF5";
	public static final String N5_FORMAT = "N5";
	public static final String ZARR_FORMAT = "Zarr";

	public static enum DOWNSAMPLE_METHOD {
		Sample, Average
	};

	public static final String DOWN_SAMPLE = "Sample";
	public static final String DOWN_AVERAGE = "Average";

	public static final String NONE = "None";

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter
	private UIService ui;

	@Parameter(label = "Image")
	private ImagePlus image; // or use Dataset? - maybe later

	@Parameter(
			label = "Root url",
			description = "The type of hierarchy is inferred from the extension of the url. Use \".h5\", \".n5\", or \".zarr\"")
	private String containerRoot;

	@Parameter(
			label = "Dataset",
			required = false,
			description = "This argument is ignored if the N5ViewerMetadata style is selected")
	private String dataset;

	@Parameter(
			label = "Format",
			style = "listBox",
			choices = {AUTO_FORMAT, HDF5_FORMAT, N5_FORMAT, ZARR_FORMAT})
	private String storageFormat = AUTO_FORMAT;

	@Parameter(
			label = "Chunk size",
			description = "The size of chunks. Comma separated, for example: \"64,32,16\".\n " +
					"ImageJ's axis order is X,Y,C,Z,T. The chunk size must be specified in this order. " +
					"You must skip any axis whose size is 1, e.g. a 2D time-series without channels " +
					"may have a chunk size of 1024,1024,1 (X,Y,T)." +
					"You may provide fewer values than the data dimension. In that case, the size will " +
					"be expanded to necessary size with the last value, for example \"64\", will expand " +
					"to \"64,64,64\" for 3D data.")
	private String chunkSizeArg;

	@Parameter(
			label = "Create Pyramid (if possible)",
			description = "Writes multiple resolutions if allowed by the choice of metadata (ImageJ and None do not).")
	private boolean createPyramidIfPossible = true;

	@Parameter(label = "Downsampling method", style = "listBox", choices = {DOWN_SAMPLE, DOWN_AVERAGE})
	private String downsampleMethod = DOWN_SAMPLE;

	@Parameter(
			label = "Compression",
			style = "listBox",
			choices = {
					GZIP_COMPRESSION,
					RAW_COMPRESSION,
					LZ4_COMPRESSION,
					XZ_COMPRESSION,
					BLOSC_COMPRESSION,
					ZSTD_COMPRESSION})
	private String compressionArg = GZIP_COMPRESSION;

	@Parameter(
			label = "metadata type",
			style = "listBox",
			description = "The style for metadata to be stored in the exported N5.",
			choices = {
					N5Importer.MetadataOmeZarrKey,
					N5Importer.MetadataImageJKey,
					N5Importer.MetadataN5ViewerKey,
					N5Importer.MetadataN5CosemKey,
					N5Importer.MetadataCustomKey,
					NONE})
	private String metadataStyle = N5Importer.MetadataOmeZarrKey;

	@Parameter(
			label = "Thread count",
			required = true,
			min = "1",
			max = "999")
	private int nThreads = 1;

	@Parameter(
			label = "Overwrite",
			description = "Allow this plugin to delete and overwrite any existing data before writing this image.",
			required = false)
	private boolean overwrite = false;

	private boolean overwriteSet = false;

	private int[] chunkSize;

	private long[] currentAbsoluteDownsampling;

	// the translation introduced by the downsampling method at the current
	// scale level
	private double[] currentTranslation;

	private N5DatasetMetadata currentChannelMetadata;

	private RandomAccessibleInterval<?> previousScaleImg;

	private ImageplusMetadata<?> impMeta;

	private N5MetadataSpecDialog metaSpecDialog;

	private final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

	private final Map<String, N5MetadataWriter<?>> styles;

	private final HashMap<Class<?>, N5MetadataWriter<?>> metadataWriters;

	// consider something like this eventually
	// private BiFunction<RandomAccessibleInterval<? extends
	// NumericType<?>>,long[],RandomAccessibleInterval<?>> downsampler;

	public N5ScalePyramidExporter() {

		styles = new HashMap<String, N5MetadataWriter<?>>();
		styles.put(N5Importer.MetadataOmeZarrKey, new OmeNgffMetadataParser());
		styles.put(N5Importer.MetadataN5ViewerKey, new N5SingleScaleMetadataParser());
		styles.put(N5Importer.MetadataN5CosemKey, new N5CosemMetadataParser());
		styles.put(N5Importer.MetadataImageJKey, new ImagePlusLegacyMetadataParser());

		metadataWriters = new HashMap<Class<?>, N5MetadataWriter<?>>();
		metadataWriters.put(OmeNgffMetadata.class, new OmeNgffMetadataParser());
		metadataWriters.put(N5SingleScaleMetadata.class, new N5SingleScaleMetadataParser());
		metadataWriters.put(N5CosemMetadata.class, new N5CosemMetadataParser());
		metadataWriters.put(NgffSingleScaleAxesMetadata.class, new OmeNgffMetadataSingleScaleParser());
		metadataWriters.put(N5ImagePlusMetadata.class, new ImagePlusLegacyMetadataParser());

		// default image plus metadata writers
		impMetaWriterTypes = new HashMap<Class<?>, ImageplusMetadata<?>>();
		impMetaWriterTypes.put(ImagePlusLegacyMetadataParser.class, new ImagePlusLegacyMetadataParser());
		impMetaWriterTypes.put(N5CosemMetadataParser.class, new CosemToImagePlus());
		impMetaWriterTypes.put(N5SingleScaleMetadataParser.class, new N5ViewerToImagePlus());
		impMetaWriterTypes.put(OmeNgffMetadataParser.class, new NgffToImagePlus());

	}

	public N5ScalePyramidExporter(final ImagePlus image,
			final String n5RootLocation,
			final String n5Dataset,
			final String storageFormat,
			final String chunkSizeArg,
			final boolean pyramidIfPossible,
			final String downsampleMethod,
			final String metadataStyle,
			final String compression) {

		this();
		setOptions(image, n5RootLocation, n5Dataset, storageFormat, chunkSizeArg, pyramidIfPossible, downsampleMethod, metadataStyle, compression);
	}

	public N5ScalePyramidExporter(final ImagePlus image,
			final String n5RootLocation,
			final String n5Dataset,
			final String storageFormat,
			final String chunkSizeArg,
			final boolean pyramidIfPossible,
			final DOWNSAMPLE_METHOD downsampleMethod,
			final String metadataStyle,
			final String compression) {

		this();
		setOptions(image, n5RootLocation, n5Dataset, storageFormat, chunkSizeArg, pyramidIfPossible, downsampleMethod.name(), metadataStyle, compression);
	}

	public void setOverwrite(final boolean overwrite) {

		this.overwrite = overwrite;
		this.overwriteSet = true;
	}

	public void clearOverwrite() {

		overwriteSet = false;
	}

	public void setOptions(
			final ImagePlus image,
			final String containerRoot,
			final String dataset,
			final String chunkSizeArg,
			final boolean pyramidIfPossible,
			final String downsampleMethod,
			final String metadataStyle,
			final String compression) {

		setOptions(image, containerRoot, dataset, AUTO_FORMAT, chunkSizeArg, pyramidIfPossible,
				downsampleMethod, metadataStyle, compression);
	}

	public void setOptions(
			final ImagePlus image,
			final String containerRoot,
			final String dataset,
			final String storageFormat,
			final String chunkSizeArg,
			final boolean pyramidIfPossible,
			final String downsampleMethod,
			final String metadataStyle,
			final String compression) {

		this.image = image;
		this.containerRoot = containerRoot;
		this.storageFormat = storageFormat;

		this.dataset = MetadataUtils.normalizeGroupPath(dataset);
		this.chunkSizeArg = chunkSizeArg;

		this.createPyramidIfPossible = pyramidIfPossible;
		this.downsampleMethod = downsampleMethod;
		this.metadataStyle = metadataStyle;
		this.compressionArg = compression;
	}

	/**
	 * Set the custom metadata mapper to use programmically.
	 *
	 * @param metadataMapper
	 *            the metadata template mapper
	 */
	public void setMetadataMapper(final MetadataTemplateMapper metadataMapper) {

		styles.put(N5Importer.MetadataCustomKey, metadataMapper);
		impMetaWriterTypes.put(MetadataTemplateMapper.class, new ImagePlusMetadataTemplate());
	}

	public static int[] parseBlockSize(final String chunkSizeArg, final long[] dims) {

		final int nd = dims.length;
		final String[] chunkArgList = chunkSizeArg.split(",");
		final int[] chunkSize = new int[nd];
		int i = 0;
		while (i < chunkArgList.length && i < nd) {
			chunkSize[i] = Integer.parseInt(chunkArgList[i]);
			i++;
		}
		final int N = chunkArgList.length - 1;

		while (i < nd) {
			if (chunkSize[N] > dims[i])
				chunkSize[i] = (int)dims[i];
			else
				chunkSize[i] = chunkSize[N];

			i++;
		}
		return chunkSize;
	}

	public void parseBlockSize(final long[] dims) {

		chunkSize = parseBlockSize(chunkSizeArg, dims);
	}

	public void parseBlockSize() {

		parseBlockSize(Intervals.dimensionsAsLongArray(ImageJFunctions.wrap(image)));
	}

	protected boolean metadataSupportsScales() {

		return metadataStyle.equals(N5Importer.MetadataN5ViewerKey) ||
				metadataStyle.equals(N5Importer.MetadataN5CosemKey) ||
				metadataStyle.equals(N5Importer.MetadataOmeZarrKey);
	}

	/**
	 * Returns the container path with an additional storage prefix if the
	 * format is defined by the plugin parameter. or null if the passed uri and
	 * storage format parameters conflict.
	 *
	 * @param containerRoot
	 *            A URI pointing to the root of a container, potentially with a
	 *            format suffix (e.g. zarr:)
	 * @param storageFormat
	 *            an explicit storage format, may be "Auto" to automatically
	 *            detect from the URI
	 * @param showWarning
	 *            whether show a warning if a conflict was detected
	 *
	 * @return a container root uri with format prefix, if possible
	 */
	public static String containerRootWithFormatPrefix(final String containerRoot, final String storageFormat, final boolean showWarning) {

		final StorageFormat uriFormat = StorageFormat.getStorageFromNestedScheme(containerRoot).getA();
		if (storageFormat.equals(AUTO_FORMAT)) {
			// "Auto" in dropdown means infer from uri, so return the uri as is
			return containerRoot;
		} else {

			// dropdown format will not be Auto
			final StorageFormat dropdownFormat = StorageFormat.valueOf(storageFormat.toUpperCase());
			if (uriFormat == null) {
				// add the format prefix to the uri
				return dropdownFormat.toString().toLowerCase() + ":" + containerRoot;
			}

			// check that uri format and dropdown format are consistent.
			// warn and exit if not
			if (uriFormat != null && !storageFormat.equals(AUTO_FORMAT) && uriFormat != dropdownFormat) {

				if (showWarning)
					IJ.showMessage("Warning", String.format("Selected format (%s) does not match format from url (%s)!",
							dropdownFormat.toString(), uriFormat.toString()));

				return null;
			}
			return containerRoot;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> void processMultiscale()
			throws IOException, InterruptedException, ExecutionException {

		final String rootWithFormatPrefix = containerRootWithFormatPrefix(containerRoot, storageFormat, true);
		if (rootWithFormatPrefix == null)
			return;

		final N5Writer n5 = new N5Factory()
				.zarrDimensionSeparator("/")
				.s3UseCredentials() // need credentials if writing to s3
				.openWriter(rootWithFormatPrefix);
		final Compression compression = getCompression();

		// TODO should have better behavior for chunk size parsing when splitting channels this might be done
		final boolean computeScales = createPyramidIfPossible && metadataSupportsScales();

		N5MetadataWriter<M> metadataWriter = null;
		if (!metadataStyle.equals(NONE)) {
			metadataWriter = (N5MetadataWriter<M>)styles.get(metadataStyle);
			if (metadataWriter != null) {
				impMeta = impMetaWriterTypes.get(metadataWriter.getClass());
			}
		}

		// get the image to save
		final RandomAccessibleInterval<T> baseImg = getBaseImage();

		final M baseMetadata;
		if (impMeta != null)
			baseMetadata = (M)impMeta.readMetadata(image);
		else
			baseMetadata = null;

		currentChannelMetadata = copyMetadata(baseMetadata);
		M currentMetadata;

		// channel splitting may modify currentBlockSize, currentAbsoluteDownsampling, and channelMetadata
		final List<RandomAccessibleInterval<T>> channelImgs = splitChannels(currentChannelMetadata, baseImg);
		for (int c = 0; c < channelImgs.size(); c++) {

			currentMetadata = copyMetadata((M)currentChannelMetadata);
			final String channelDataset = getChannelDatasetName(c);
			RandomAccessibleInterval<T> currentChannelImg = channelImgs.get(c);
			final int nd = currentChannelImg.numDimensions();

			// every channel starts at the original scale level reset downsampling factors to 1
			currentAbsoluteDownsampling = new long[nd];
			Arrays.fill(currentAbsoluteDownsampling, 1);

			final N multiscaleMetadata = initializeMultiscaleMetadata((M)currentMetadata, channelDataset);
			currentTranslation = new double[nd];

			// write scale levels
			// we will stop early even when maxNumScales != 1
			final int maxNumScales = computeScales ? 99 : 1;
			boolean anyScalesWritten = false;
			for (int s = 0; s < maxNumScales; s++) {

				final String dset = getScaleDatasetName(c, s);
				// downsample when relevant
				long[] relativeFactors = new long[nd];
				Arrays.fill(relativeFactors, 1);
				if (s > 0) {
					relativeFactors = getRelativeDownsampleFactors(currentMetadata, currentChannelImg.numDimensions(), s, currentAbsoluteDownsampling);

					// update absolute downsampling factors
					for (int i = 0; i < nd; i++)
						currentAbsoluteDownsampling[i] *= relativeFactors[i];

					currentChannelImg = downsampleMethod((RandomAccessibleInterval<T>)getPreviousScaleImage(c, s), relativeFactors);

					if (downsampleMethod.equals(DOWN_AVERAGE))
						Arrays.setAll(currentTranslation, i -> {
							if (currentAbsoluteDownsampling[i] > 1)
								return 0.5 * currentAbsoluteDownsampling[i] - 0.5;
							else
								return 0.0;
						});
				}

				// update metadata to reflect this scale level, returns new metadata instance

				// TODO needs to be relative downsampling
				currentMetadata = (M)metadataForThisScale(dset, currentMetadata, relativeFactors, currentTranslation);

				// write to the appropriate dataset
				// if dataset exists and not overwritten, don't write metadata
				if (!write(currentChannelImg, n5, dset, compression, currentMetadata))
					continue;

				storeScaleReference(c, s, currentChannelImg);

				updateMultiscaleMetadata(multiscaleMetadata, currentMetadata);
				anyScalesWritten = true;

				// chunkSize variable is updated by the write method
				if (lastScale(chunkSize, currentChannelImg))
					break;
			}

			if (anyScalesWritten)
				writeMetadata(
						// this returns null when not multiscale
						finalizeMultiscaleMetadata(channelDataset, multiscaleMetadata),
						n5,
						channelDataset);
		}
		n5.close();
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M defaultMetadata(final ImagePlus imp) {

		return (M)new AbstractN5DatasetMetadata("", null) {};
	}

	protected void storeScaleReference(final int channel, final int scale, final RandomAccessibleInterval<?> img) {

		previousScaleImg = img;
	}

	@SuppressWarnings("unchecked")
	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getPreviousScaleImage(final int channel, final int scale) {

		return (RandomAccessibleInterval<T>)previousScaleImg;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> N initializeMultiscaleMetadata(final M baseMetadata, final String path) {

		if (!metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return null;

		return ((N)new OmeNgffMultiScaleMetadataMutable(path));
	}

	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> void updateMultiscaleMetadata(final N multiscaleMetadata,
			final M scaleMetadata) {

		if (!metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return;

		if (multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable &&
				scaleMetadata instanceof NgffSingleScaleAxesMetadata) {

			final OmeNgffMultiScaleMetadataMutable ngffMs = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;
			ngffMs.addChild((NgffSingleScaleAxesMetadata)scaleMetadata);
		}
	}

	@SuppressWarnings("unchecked")
	protected <N extends SpatialMetadataGroup<?>> N finalizeMultiscaleMetadata(final String path, final N multiscaleMetadata) {

		if (!metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
			return multiscaleMetadata;

		if (multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable) {
			final OmeNgffMultiScaleMetadataMutable ms = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;

			final OmeNgffMultiScaleMetadata meta = new OmeNgffMultiScaleMetadata(ms.getAxes().length,
					path, path, downsampleMethod, "0.4",
					ms.getAxes(),
					ms.getDatasets(), null,
					ms.coordinateTransformations, ms.metadata, true);

			return ((N)new OmeNgffMetadata(path, new OmeNgffMultiScaleMetadata[]{meta}));
		}

		return multiscaleMetadata;
	}

	protected boolean lastScale(final int[] chunkSize, final Interval imageDimensions) {

		for (int i = 0; i < imageDimensions.numDimensions(); i++) {
			if (imageDimensions.dimension(i) <= chunkSize[i])
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M metadataForThisScale(final String newPath, final M baseMetadata, final long[] downsamplingFactors,
			final double[] translation) {

		if (baseMetadata instanceof SpatialModifiable) {
			return (M)(((SpatialModifiable<?>)baseMetadata).modifySpatialTransform(
					newPath,
					Arrays.stream(downsamplingFactors).mapToDouble(x -> (double)x).toArray(),
					translation));
		}

		// System.err.println("WARNING: metadata not spatial modifiable");
		return baseMetadata;
	}

	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> downsampleMethod(final RandomAccessibleInterval<T> img,
			final long[] factors) {

		if (downsampleMethod.equals(DOWN_AVERAGE))
			return downsampleAvgBy2(img, factors);
		else
			return downsample(img, factors);
	}

	protected <M extends N5Metadata> String getChannelDatasetName(final int channelIndex) {

		if (metadataStyle.equals(N5Importer.MetadataN5ViewerKey) ||
				(image.getNChannels() > 1 && metadataStyle.equals(N5Importer.MetadataN5CosemKey))) {

			return MetadataUtils.normalizeGroupPath(dataset + String.format("/c%d", channelIndex));
		} else
			return dataset;
	}

	protected <M extends N5Metadata> String getScaleDatasetName(final int channelIndex, final int scale) {

		if (metadataSupportsScales())
			return getChannelDatasetName(channelIndex) + String.format("/s%d", scale);
		else
			return getChannelDatasetName(channelIndex);
	}

	/**
	 * Intialize the downsampling factors as ones. The first (zeroth?) scale is
	 * always at the original resolution.
	 *
	 * @param nd
	 *            number of dimensions
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
		for (int i = 0; i < nd; i++) {

			// only downsample spatial dimensions
			if (axes[i].getType().equals(Axis.SPACE))
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
		for (int i = 0; i < nd; i++) {

			// only downsample spatial dimensions
			if (axes[i].getType().equals(Axis.SPACE))
				factors[i] = 2;
			else
				factors[i] = 1;
		}

		return factors;
	}

	protected <M extends N5Metadata> Axis[] getAxes(final M metadata, final int nd) {

		if (metadata instanceof AxisMetadata)
			return ((AxisMetadata)metadata).getAxes();
		else if (metadata instanceof N5SingleScaleMetadata)
			return AxisUtils.defaultN5ViewerAxes((N5SingleScaleMetadata)metadata).getAxes();
		else
			return AxisUtils.defaultAxes(nd);
	}

	// also extending NativeType causes build failures using maven, unclear why
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
	 * If relevant, according to the passed {@link N5DatasetMetadata} metadata
	 * instance, return a list containing the channels of the input image. A
	 * list containing the input image will be returned if there is exactly one
	 * channel.
	 *
	 * @param <T>
	 *            the image type
	 * @param <M>
	 *            the metadata type
	 * @param metadata
	 *            the metadata
	 * @param img
	 *            the image
	 * @return A list of images containing the channels of the input image.
	 */
	protected <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> List<RandomAccessibleInterval<T>> splitChannels(final M metadata,
			final RandomAccessibleInterval<T> img) {

		// TODO perhaps should return new metadata that is not
		// some metadata styles never split channels, return input image in that
		// case
		if (metadataStyle.equals(NONE) || metadataStyle.equals(N5Importer.MetadataCustomKey) ||
				metadataStyle.equals(N5Importer.MetadataOmeZarrKey) ||
				metadataStyle.equals(N5Importer.MetadataImageJKey)) {
			return Collections.singletonList(img);
		}

		// otherwise, split channels
		final ArrayList<RandomAccessibleInterval<T>> channels = new ArrayList<>();
		boolean slicedChannels = false;
		for (int c = 0; c < image.getNChannels(); c++) {

			RandomAccessibleInterval<T> channelImg;
			// If there is only one channel, img may be 3d, but we don't want to slice
			// so if we have a 3d image check that the image is multichannel
			if (image.getNChannels() > 1) {
				channelImg = Views.hyperSlice(img, 2, c);
				slicedChannels = true;
			} else {
				channelImg = img;
			}

			// make the image 4d and update the chunk size, if needed
			if (metadataStyle.equals(N5Importer.MetadataN5ViewerKey) && image.getNFrames() > 1 && image.getNSlices() == 1) {

				// make a 4d image in order XYZT
				channelImg = Views.permute(Views.addDimension(channelImg, 0, 0), 2, 3);
			}
			channels.add(channelImg);
		}

		if (slicedChannels) {
			// if we slice the image, appropriately slice the chunk size also
			currentChannelMetadata = sliceMetadata(metadata, 2);
		}

		return channels;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M copyMetadata(final M metadata) {

		if (metadata == null)
			return metadata;

		// Needs to be implemented for metadata types that split channels
		if (metadata instanceof N5CosemMetadata) {
			return ((M)new N5CosemMetadata(metadata.getPath(), ((N5CosemMetadata)metadata).getCosemTransform(),
					metadata.getAttributes()));
		} else if (metadata instanceof N5SingleScaleMetadata) {
			final N5SingleScaleMetadata ssm = (N5SingleScaleMetadata)metadata;
			return ((M)new N5SingleScaleMetadata(ssm.getPath(),
					ssm.spatialTransform3d(), ssm.getDownsamplingFactors(),
					ssm.getPixelResolution(), ssm.getOffset(), ssm.unit(),
					metadata.getAttributes(),
					ssm.minIntensity(),
					ssm.maxIntensity(),
					ssm.isLabelMultiset()));
		} else if (metadata instanceof NgffSingleScaleAxesMetadata) {
			final NgffSingleScaleAxesMetadata ngffMeta = (NgffSingleScaleAxesMetadata)metadata;
			return (M)new NgffSingleScaleAxesMetadata(ngffMeta.getPath(),
					ngffMeta.getScale(), ngffMeta.getTranslation(),
					ngffMeta.getAxes(),
					ngffMeta.getAttributes());
		} else if (metadata instanceof N5ImagePlusMetadata) {
			final N5ImagePlusMetadata ijmeta = (N5ImagePlusMetadata)metadata;
			return (M)new N5ImagePlusMetadata(ijmeta.getPath(), ijmeta.getAttributes(),
					ijmeta.getName(), ijmeta.fps, ijmeta.frameInterval, ijmeta.unit,
					ijmeta.pixelWidth, ijmeta.pixelHeight, ijmeta.pixelDepth,
					ijmeta.xOrigin, ijmeta.yOrigin, ijmeta.zOrigin,
					ijmeta.numChannels, ijmeta.numSlices, ijmeta.numFrames,
					ijmeta.type, ijmeta.properties);
		} else
			System.err.println("Encountered metadata of unexpected type.");

		return metadata;
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M sliceMetadata(final M metadata, final int i) {

		// Needs to be implemented for metadata types that split channels
		if (metadata instanceof N5CosemMetadata) {
			return ((M)new N5CosemMetadata(metadata.getPath(), ((N5CosemMetadata)metadata).getCosemTransform(),
					removeDimension(metadata.getAttributes(), i)));
		} else if (metadata instanceof N5SingleScaleMetadata) {
			final N5SingleScaleMetadata ssm = (N5SingleScaleMetadata)metadata;
			return ((M)new N5SingleScaleMetadata(ssm.getPath(),
					ssm.spatialTransform3d(), ssm.getDownsamplingFactors(),
					ssm.getPixelResolution(), ssm.getOffset(), ssm.unit(),
					removeDimension(metadata.getAttributes(), i),
					ssm.minIntensity(),
					ssm.maxIntensity(),
					ssm.isLabelMultiset()));
		} else if (metadata instanceof NgffSingleScaleAxesMetadata) {
			final NgffSingleScaleAxesMetadata ngffMeta = (NgffSingleScaleAxesMetadata)metadata;
			return (M)new NgffSingleScaleAxesMetadata(ngffMeta.getPath(),
					ngffMeta.getScale(), ngffMeta.getTranslation(),
					removeDimension(ngffMeta.getAttributes(), i));
		}
		return metadata;
	}

	protected DatasetAttributes removeDimension(final DatasetAttributes attributes, final int i) {

		return new DatasetAttributes(
				removeElement(attributes.getDimensions(), i),
				removeElement(attributes.getBlockSize(), i),
				attributes.getDataType(),
				attributes.getCompression());
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5Metadata> void writeMetadata(final M metadata, final N5Writer n5, final String dataset) {

		if (metadata != null)
			Optional.ofNullable(metadataWriters.get(metadata.getClass())).ifPresent(writer -> {
				try {
					((N5MetadataWriter<M>)writer).writeMetadata(metadata, n5, dataset);
				} catch (final Exception e) {}
			});
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T extends RealType & NativeType, M extends N5Metadata> boolean write(
			final RandomAccessibleInterval<T> image,
			final N5Writer n5,
			final String dataset,
			final Compression compression, final M metadata)
			throws IOException, InterruptedException, ExecutionException {

		final String deleteThisPathToOverwrite = needOverwrite(n5, dataset);
		if (deleteThisPathToOverwrite != null) {

			if (!overwrite && !overwriteSet) {
				if (ui != null)
					overwrite = promptOverwrite(dataset);
				else
					overwrite = promptOverwrite(dataset);
			}

			if (overwrite) {
				n5.remove(deleteThisPathToOverwrite);
			} else {
				return false; // data set exists but not overwriting
			}
		}

		parseBlockSize(image.dimensionsAsLongArray());

		// Here, either allowing overwrite, or not allowing, but the dataset does not exist.
		// use threadPool even for single threaded execution for progress monitoring
		final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		progressMonitor(threadPool);
		N5Utils.save(image,
				n5, dataset, chunkSize, compression,
				Executors.newFixedThreadPool(nThreads));

		writeMetadata(metadata, n5, dataset);
		return true;
	}

	private static String needOverwrite(final N5Reader n5, final String path) {

		// need to overwrite if path exists
		if (n5.exists(path))
			return path;

		// also need to overwrite if any parent of the path is a dataset
		// datasets must be leaf nodes of the container tree
		final String[] parts = path.split("/");
		if (n5.datasetExists(""))
			return "";

		String currentPath = "";
		for (final String p : parts) {
			currentPath += "/" + p;
			if (n5.datasetExists(currentPath))
				return currentPath;
		}

		return null;
	}

	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsample(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {

		return Views.subsample(img, downsampleFactors);
	}

	/**
	 * Downsamples an image by factors of 2 using averaging.
	 * <p>
	 * Not the most efficient when some dimensions are not downsampled.
	 * </p>
	 *
	 * @param <T>
	 *            the image data type
	 * @param img
	 *            the image
	 * @param downsampleFactors
	 *            the factors
	 * @return a downsampled image
	 */
	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsampleAvgBy2(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {

		// ensure downsampleFactors contains only 1's and 2's
		assert Arrays.stream(downsampleFactors).filter(x -> (x == 1) || (x == 2)).count() == downsampleFactors.length;

		final int nd = downsampleFactors.length;
		final double[] scale = new double[nd];
		final double[] translation = new double[nd];

		final long[] dims = new long[nd];

		for (int i = 0; i < nd; i++) {

			if (downsampleFactors[i] == 2) {
				scale[i] = 0.5;
				translation[i] = -0.25;
				dims[i] = (long)Math.ceil(img.dimension(i) / 2);
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

	private int[] sliceBlockSize(final int exclude) {

		return removeElement(chunkSize, exclude);
	}

	private long[] sliceDownsamplingFactors(final int exclude) {

		return removeElement(currentAbsoluteDownsampling, exclude);
	}

	private static int[] removeElement(final int[] arr, final int excludeIndex) {

		final int[] out = new int[arr.length - 1];
		int j = 0;
		for (int i = 0; i < arr.length; i++)
			if (i != excludeIndex) {
				out[j] = arr[i];
				j++;
			}

		return out;
	}

	private static long[] removeElement(final long[] arr, final int excludeIndex) {

		final long[] out = new long[arr.length - 1];
		int j = 0;
		for (int i = 0; i < arr.length; i++)
			if (i != excludeIndex) {
				out[j] = arr[i];
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

	private void progressMonitor(final ThreadPoolExecutor exec) {

		new Thread() {

			@Override
			public void run() {

				IJ.showProgress(0.01);
				try {
					Thread.sleep(333);
					boolean done = false;
					while (!done && !exec.isShutdown()) {
						final long i = exec.getCompletedTaskCount();
						final long N = exec.getTaskCount();
						done = i == N;
						IJ.showProgress((double)i / N);
						Thread.sleep(333);
					}
				} catch (final InterruptedException e) {}
				IJ.showProgress(1.0);
			}
		}.start();
		return;
	}

	private Compression getCompression() {

		return getCompression(compressionArg);
	}

	private final boolean promptOverwrite(final String dataset) {

		return JOptionPane.showConfirmDialog(null,
				String.format("Dataset (%s) already exists. Completely remove that dataa and overwrite?", dataset), "Warning",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	public static Compression getCompression(final String compressionArg) {

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
		case ZSTD_COMPRESSION:
			return new ZstandardCompression();
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
