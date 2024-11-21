/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JTree;

import org.apache.commons.lang.ArrayUtils;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.converters.LabelMultisetLongConverter;
import org.janelia.saalfeldlab.n5.converters.UnsignedShortLUTConverter;
import org.janelia.saalfeldlab.n5.imglib2.N5LabelMultisets;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.imagej.CanonicalMetadataToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusLegacyMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ViewerToImagePlus;
import org.janelia.saalfeldlab.n5.metadata.imagej.NgffToImagePlus;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;
import org.janelia.saalfeldlab.n5.ui.N5SwingTreeNode;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DefaultSingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5GenericSingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueReader;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.process.ImageStatistics;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.parallel.DefaultTaskExecutor;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class N5Importer implements PlugIn {

	private static final String[] axisNames = new String[]{"dim1", "dim2", "dim3", "dim4", "dim5"};

	public static final String n5PathKey = "url";
	public static final String virtualKey = "virtual";
	public static final String hideKey = "hide";
	public static final String minKey = "min";
	public static final String maxKey = "max";
	public static final String COMMAND_NAME = "HDF5/N5/Zarr/OME-NGFF ... ";

	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	public static final String MetadataAutoKey = "Auto-detect";
	public static final String MetadataOmeZarrKey = "OME-NGFF";
	public static final String MetadataImageJKey = "ImageJ";
	public static final String MetadataN5CosemKey = "COSEM";
	public static final String MetadataN5ViewerKey = "N5Viewer";
	public static final String MetadataCustomKey = "Custom";
	public static final String MetadataDefaultKey = "Default";

	public static final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
			new ImagePlusLegacyMetadataParser(),
			new N5CosemMetadataParser(),
			new N5SingleScaleMetadataParser(),
			new CanonicalMetadataParser(),
			new N5GenericSingleScaleMetadataParser()
	};

	public static final N5MetadataParser<?>[] GROUP_PARSERS = new N5MetadataParser[]{
			new OmeNgffMetadataParser(),
			new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
			new OmeNgffMetadataParser(),
			new N5ViewerMultiscaleMetadataParser(),
			new CanonicalMetadataParser(),
	};

	private static final Predicate<N5Metadata> ALL_PASS = x -> { return true; };

	private N5Reader n5;

	private DatasetSelectorDialog selectionDialog;

	private DataSelection selection;

	private final Map<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

	private ImageplusMetadata<?> impMeta;

	private Interval cropInterval;

	private boolean asVirtual;

	private boolean show = true;

	private boolean cropOption;

	private Thread loaderThread;

	private final ExecutorService exec;

	private boolean initialRecorderState;

	private int numDimensionsForCrop;

	private long[] initMaxValuesForCrop;

	private List<ImagePlus> lastResult;

	private static String lastOpenedContainer = "";

	public N5Importer() {

		// store value of record
		// necessary to skip initial opening of this dialog
		initialRecorderState = Recorder.record;
		Recorder.record = false;

		// default image plus metadata parsers
		impMetaWriterTypes = defaultImagePlusMetadataWriters();
		numDimensionsForCrop = 5;
		initMaxValuesForCrop = new long[numDimensionsForCrop];
		Arrays.fill(initMaxValuesForCrop, Long.MAX_VALUE);

		exec = Executors.newFixedThreadPool(Prefs.getThreads());
	}

	private static HashMap<Class<?>, ImageplusMetadata<?>> defaultImagePlusMetadataWriters()
	{
		final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes = new HashMap<>();
		impMetaWriterTypes.put(N5ImagePlusMetadata.class, new ImagePlusLegacyMetadataParser());
		impMetaWriterTypes.put(NgffSingleScaleAxesMetadata.class, new NgffToImagePlus());
		impMetaWriterTypes.put(N5CosemMetadata.class, new CosemToImagePlus());
		impMetaWriterTypes.put(N5SingleScaleMetadata.class, new N5ViewerToImagePlus());
		impMetaWriterTypes.put(CanonicalDatasetMetadata.class, new CanonicalMetadataToImagePlus());
		impMetaWriterTypes.put(CanonicalSpatialDatasetMetadata.class, new CanonicalMetadataToImagePlus());
		return impMetaWriterTypes;
	}

	public N5Reader getN5() {

		return n5;
	}

	public List<ImagePlus> getResult() {

		return lastResult;
	}

	public Map<Class<?>, ImageplusMetadata<?>> getImagePlusMetadataWriterMap() {

		return impMetaWriterTypes;
	}

	public void setNumDimensionsForCropDialog(final int numDimensionsForCrop) {

		this.numDimensionsForCrop = numDimensionsForCrop;
	}

	/**
	 * Set a flag determining whether the process method calls show on the
	 * resulting ImagePlus.
	 *
	 * @param show
	 *            the flag
	 */
	public void setShow(final boolean show) {

		this.show = show;
	}

	public void runWithDialog(final String pathToContainer, final List<String> selectThisSubPath) {
		lastOpenedContainer = pathToContainer;
		selectionDialog = null;
		run(null);
		if (selectionDialog == null) {
			throw new RuntimeException("The \"Open N5\" didn't come up when it should.");
		} else {
			selectionDialog.detectDatasets();
			if (selectThisSubPath != null) {
				boolean isDiscoveryFinished = selectionDialog.waitUntilDiscoveryIsFinished(60000);
				if (isDiscoveryFinished) selectTreeItem(selectThisSubPath);
			}
		}
	}

	private void selectTreeItem(final List<String> itemPath) {
		final JTree t = selectionDialog.getJTree();
		int currRow = 0;
		for (String subPath : itemPath) {
			for (int r = currRow; r < t.getRowCount(); ++r, ++currRow) {
				N5SwingTreeNode n = (N5SwingTreeNode)t.getPathForRow(r).getLastPathComponent();
				if (n.getNodeName().equals(subPath)) {
					t.expandRow(r);
					t.setSelectionRow(r);
					++currRow;
					break;
				}
			}
		}
	}

	@Override
	public void run(final String args) {

		final String macroOptions = Macro.getOptions();
		String options = args;
		if (options == null || options.isEmpty())
			options = macroOptions;

		final boolean isMacro = (options != null && !options.isEmpty());
		final boolean isCrop = options != null && options.contains("cropDialog");

		if (!isMacro && !isCrop) {
			// the fancy selector dialog
			selectionDialog = new DatasetSelectorDialog(
					new N5ViewerReaderFun(),
					new N5BasePathFun(),
					lastOpenedContainer,
					new N5MetadataParser[]{ new OmeNgffMetadataParser() }, // need the ngff parser because it's where the metadata are
					PARSERS);

			selectionDialog.setLoaderExecutor(exec);
			selectionDialog.setTreeRenderer(new N5DatasetTreeCellRenderer(true));

			// restrict canonical metadata to those with spatial metadata, but
			// without
			// multiscale
			selectionDialog.getTranslationPanel().setFilter(
					x -> (x instanceof CanonicalDatasetMetadata));

			selectionDialog.setSelectionFilter(
					x -> (x instanceof N5DatasetMetadata));

			selectionDialog.setContainerPathUpdateCallback(x -> {
				if (x != null)
					lastOpenedContainer = x;
			});

			selectionDialog.setCancelCallback(x -> {
				// set back recorder state if canceled
				Recorder.record = initialRecorderState;
			});

			selectionDialog.setVirtualOption(true);
			selectionDialog.setCropOption(true);
			selectionDialog.run(this::datasetSelectorCallBack);
		} else {
			// disable recorder
			initialRecorderState = Recorder.record;
			Recorder.record = false;

			// parameters
			String n5Path = Macro.getValue(options, n5PathKey, "");
			Interval thisDatasetCropInterval = null;
			boolean openAsVirtual = options.contains(" " + virtualKey);

			// we don't always know ahead of time the dimensionality
			if (isCrop) {
				final GenericDialog gd = new GenericDialog("Import N5");
				gd.addStringField("N5 path", n5Path);
				gd.addCheckbox("Virtual", openAsVirtual);

				gd.addMessage(" ");
				gd.addMessage("Crop parameters.");
				gd.addMessage("[0,Infinity] loads the whole volume.");
				gd.addMessage("Min:");
				for (int i = 0; i < numDimensionsForCrop; i++)
					gd.addNumericField("min_" + axisNames[i], 0);

				gd.addMessage("Max:");
				for (int i = 0; i < numDimensionsForCrop; i++) {
					if (initMaxValuesForCrop != null)
						gd.addNumericField("max_" + axisNames[i], initMaxValuesForCrop[i]);
					else
						gd.addNumericField("max_" + axisNames[i], Double.POSITIVE_INFINITY);
				}

				gd.showDialog();
				if (gd.wasCanceled()) {
					// set back recorder state if canceled
					Recorder.record = initialRecorderState;
					return;
				}

				n5Path = gd.getNextString();
				openAsVirtual = gd.getNextBoolean();

				final long[] cropMin = new long[numDimensionsForCrop];
				final long[] cropMax = new long[numDimensionsForCrop];

				for (int i = 0; i < numDimensionsForCrop; i++)
					cropMin[i] = Math.max(0, (long)Math.floor(gd.getNextNumber()));

				for (int i = 0; i < numDimensionsForCrop; i++) {
					final double v = gd.getNextNumber();
					cropMax[i] = Double.isInfinite(v) ? Long.MAX_VALUE : (long)Math.ceil(v);
				}

				thisDatasetCropInterval = new FinalInterval(cropMin, cropMax);
			} else {
				final String minString = Macro.getValue(options, minKey, "");
				final String maxString = Macro.getValue(options, maxKey, "");
				if (minString != null && !minString.isEmpty()) {
					thisDatasetCropInterval = parseCropParameters(minString, maxString);
				}
				show = !options.contains(" " + hideKey);
			}

			// set recorder back
			Recorder.record = initialRecorderState;

			final N5Reader n5ForThisDataset = new N5ViewerReaderFun().apply(n5Path);
			final String rootPath = n5ForThisDataset.getURI().toString();
			final String dset = new N5BasePathFun().apply(n5Path);

			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5ForThisDataset, N5DatasetDiscoverer.fromParsers(PARSERS),
					Collections.singletonList(new OmeNgffMetadataParser()));

			N5Metadata meta = null;
			try {
				final N5TreeNode root = discoverer.discoverAndParseRecursive("");
				final Optional<N5Metadata> metaOpt = root.getDescendant(dset)
						.filter(x -> {
							return x.getMetadata() != null;
						}).map(N5TreeNode::getMetadata);

				if (metaOpt.isPresent())
					meta = metaOpt.get();

			} catch (final Exception e) {
				throw new N5Exception("Failure to parse or find data at " + dset, e);
			}

			if (meta != null && meta instanceof N5DatasetMetadata)
				lastResult = process(n5ForThisDataset, rootPath, exec, Collections.singletonList((N5DatasetMetadata)meta), openAsVirtual, thisDatasetCropInterval,
						show, impMetaWriterTypes);
			else
				System.err.println("not a dataset : " + n5Path);
		}
	}

	public static boolean isTypeOpenable(final N5DatasetMetadata meta, final boolean showMessage) {

		final DataType type = meta.getAttributes().getDataType();
		if (type != DataType.FLOAT32 &&
				type != DataType.UINT8 &&
				type != DataType.UINT16 &&
				type != DataType.UINT32) {
			if (showMessage) {
				IJ.error("Cannot open datasets of type (" + type + ").\n"
						+ "ImageJ supports uint8, uint16, uint32(rgb), or float32.");
			}
			return false;
		}
		return true;
	}

	private void datasetSelectorCallBack(final DataSelection selection) {

		// set the recorder back to its original value
		Recorder.record = initialRecorderState;

		this.selection = selection;
		this.n5 = selection.n5;
		this.asVirtual = selectionDialog.isVirtual();
		this.cropOption = selectionDialog.isCropSelected();

		if (cropOption)
			processWithCrops();
		else
			processThread();
	}

	public static String generateAndStoreOptions(final String n5RootAndDataset, final boolean virtual, final Interval cropInterval) {

		return generateAndStoreOptions(n5RootAndDataset, virtual, cropInterval, false);
	}

	public static String generateAndStoreOptions(final String n5RootAndDataset, final boolean virtual, final Interval cropInterval,
			final boolean hide) {

		Recorder.resetCommandOptions();
		Recorder.recordOption(n5PathKey, n5RootAndDataset);

		if (virtual)
			Recorder.recordOption(virtualKey);

		if (hide)
			Recorder.recordOption(hideKey);

		if (cropInterval != null) {
			final String[] cropParams = minMaxStrings(cropInterval);
			Recorder.recordOption(minKey, cropParams[0]);
			Recorder.recordOption(maxKey, cropParams[1]);
		}
		return Recorder.getCommandOptions();
	}

	private static String[] minMaxStrings(final Interval interval) {

		final long[] tmp = new long[interval.numDimensions()];

		interval.min(tmp);
		final String minString = Arrays.stream(tmp).mapToObj(Long::toString).collect(Collectors.joining(","));

		interval.max(tmp);
		final String maxString = Arrays.stream(tmp).mapToObj(Long::toString).collect(Collectors.joining(","));

		return new String[]{minString, maxString};
	}

	private static Interval parseCropParameters(final String minParam, final String maxParam) {

		return new FinalInterval(
				Arrays.stream(minParam.split(",")).mapToLong(Long::parseLong).toArray(),
				Arrays.stream(maxParam.split(",")).mapToLong(Long::parseLong).toArray());
	}

	public static void record(final String n5RootAndDataset, final boolean virtual, final Interval cropInterval) {

		if (!Recorder.record)
			return;

		Recorder.setCommand(COMMAND_NAME);
		generateAndStoreOptions(n5RootAndDataset, virtual, cropInterval);

		Recorder.saveCommand();
	}

	/**
	 * Read a single N5 dataset into a ImagePlus and show it
	 *
	 * @param <T>
	 *            the image data type
	 * @param <M>
	 *            the metadata type
	 * @param n5
	 *            the n5Reader
	 * @param exec
	 *            an ExecutorService to manage parallel reading
	 * @param datasetMetaArg
	 *            datasetMetadata containing the path
	 * @param cropIntervalIn
	 *            optional crop interval
	 * @param asVirtual
	 *            whether to open virtually
	 * @param ipMeta
	 *            metadata
	 * @return the ImagePlus
	 * @throws IOException
	 *             io
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T extends NumericType<T> & NativeType<T>, M extends N5DatasetMetadata, A extends AxisMetadata & N5Metadata> ImagePlus read(
			final N5Reader n5,
			final ExecutorService exec,
			final N5DatasetMetadata datasetMetaArg, final Interval cropIntervalIn, final boolean asVirtual,
			final ImageplusMetadata<M> ipMeta) throws IOException {

		final String d = datasetMetaArg.getPath();
		final CachedCellImg imgRaw = N5Utils.open(n5, d);

		RandomAccessibleInterval imgNorm;
		if (OmeNgffMultiScaleMetadata.fOrder(datasetMetaArg.getAttributes())) {
			imgNorm = AxisUtils.reverseDimensions(imgRaw);
			ArrayUtils.reverse(datasetMetaArg.getAttributes().getDimensions());
		}
		else
			imgNorm = imgRaw;

		// crop if necessary
		final RandomAccessibleInterval imgC;
		Interval cropInterval = null;
		if (cropIntervalIn != null) {
			cropInterval = processCropInterval(imgNorm, cropIntervalIn);
			imgC = Views.interval(imgNorm, cropInterval);
		} else
			imgC = imgNorm;

		final RandomAccessibleInterval img;
		final M datasetMeta;
		if (datasetMetaArg != null && datasetMetaArg instanceof AxisMetadata) {

			// this permutation will be applied to the image whose dimensions
			// are padded to 5d with a canonical axis order
			final int[] p = AxisUtils.findImagePlusPermutation((AxisMetadata)datasetMetaArg);

			final Pair<RandomAccessibleInterval<T>, M> res = AxisUtils.permuteImageAndMetadataForImagePlus(p, imgC, datasetMetaArg);
			img = res.getA();
			datasetMeta = res.getB();
		} else {
			img = imgC;
			datasetMeta = (M)datasetMetaArg;
		}

		RandomAccessibleInterval<T> convImg;
		final DataType type = datasetMeta.getAttributes().getDataType();

		final boolean isRGB = (datasetMeta instanceof N5ImagePlusMetadata) && ((N5ImagePlusMetadata)datasetMeta).getType() == ImagePlus.COLOR_RGB;

		// convert label multisets to ulong, then converts to ushort with LUT
		if (N5LabelMultisets.isLabelMultisetType(n5, datasetMeta.getPath())) {

			// why is this cast necessary?
			convImg = (RandomAccessibleInterval<T>)convertToUShortLUT(
					Converters.convert2(
							img,
							new LabelMultisetLongConverter(),
							UnsignedLongType::new));
		} else {

			// Compute LUT after crop
			if (type == DataType.FLOAT64) {
				convImg = convertDouble(img);
			} else if (isRGB && type == DataType.UINT32) {
				convImg = convertToRGB(img);
			} else if ( type == DataType.INT32 || type == DataType.UINT32 ||
					    type == DataType.INT64 || type == DataType.UINT64) {
				convImg = convertToUShortLUT(img);
			} else {
				// this covers int8 -> uint8 and int16 -> uint16
				convImg = img;
			}
		}

		ImagePlus imp;
		if (asVirtual) {
			imp = ImageJFunctions.wrap(convImg, d, exec);
		} else {
			final ImagePlusImg<T, ?> ipImg = new ImagePlusImgFactory<>(Util.getTypeFromInterval(convImg)).create(convImg);
			LoopBuilder.setImages(convImg, ipImg)
					.multiThreaded(new DefaultTaskExecutor(exec))
					.forEachPixel((x, y) -> y.set(x));

			imp = ipImg.getImagePlus();
		}

		if (ipMeta != null) {
			try {
				ipMeta.writeMetadata((M)datasetMeta, imp);
			} catch (final Exception e) {
				System.err.println("Failed to convert metadata to Imageplus for " + d);
			}
		}

		if (cropInterval != null) {
			imp.getCalibration().xOrigin -= cropInterval.min(0);
			imp.getCalibration().yOrigin -= cropInterval.min(1);

			if (cropInterval.numDimensions() >= 3)
				imp.getCalibration().zOrigin -= cropInterval.min(2);
		}

		return imp;
	}

	private static boolean zarrFOrderAndEmptyMetadata(final N5Reader n5, N5Metadata meta) {

		if (n5 instanceof ZarrKeyValueReader && meta instanceof N5DefaultSingleScaleMetadata) {
			final ZarrDatasetAttributes zattrs = ((ZarrKeyValueReader)n5).getDatasetAttributes(meta.getPath());
			return !zattrs.isRowMajor();
		}

		return false;
	}

	public static RandomAccessibleInterval<FloatType> convertDouble(
			final RandomAccessibleInterval<DoubleType> img) {

		return Converters.convert(
				img,
				new RealFloatConverter<DoubleType>(),
				new FloatType());
	}

	public static RandomAccessibleInterval<ARGBType> convertToRGB(final RandomAccessibleInterval<UnsignedIntType> img) {

		return Converters.convert(
				img,
				new IntegerToaRGBConverter(),
				new ARGBType());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <T extends NumericType<T> & NativeType<T>> RandomAccessibleInterval<UnsignedShortType> convertToUShortLUT(
			final RandomAccessibleInterval<T> img) {

		return Converters.convert(
				img,
				new UnsignedShortLUTConverter(Views.flatIterable(img)),
				new UnsignedShortType());
	}

	private static Interval processCropInterval(final RandomAccessibleInterval<?> img, final Interval cropInterval) {

		assert img.numDimensions() == cropInterval.numDimensions();

		final int nd = img.numDimensions();
		final long[] min = new long[nd];
		final long[] max = new long[nd];

		for (int i = 0; i < nd; i++) {
			min[i] = Math.max(img.min(i), cropInterval.min(i));
			max[i] = Math.min(img.max(i), cropInterval.max(i));
		}

		return new FinalInterval(min, max);
	}

	/**
	 * Read one or more N5 dataset into ImagePlus object(s), first prompting the
	 * user to specify crop parameters.
	 */
	public void processWithCrops() {

		asVirtual = selectionDialog.isVirtual();
		final String rootPath = selectionDialog.getN5RootPath();
		for (final N5Metadata datasetMeta : selection.metadata) {
			// Macro.getOptions() does not return what I'd expect after this
			// call. why?
			// Macro.setOptions( String.format( "n5=%s", datasetMeta.getPath()
			// ));

			final String datasetPath = datasetMeta.getPath();
			final String pathToN5Dataset = datasetPath.isEmpty() ? rootPath : rootPath + File.separator + datasetPath;

			numDimensionsForCrop = ((N5DatasetMetadata)datasetMeta).getAttributes().getNumDimensions();
			initMaxValuesForCrop = Arrays.stream(((N5DatasetMetadata)datasetMeta).getAttributes().getDimensions())
					.map(x -> x - 1)
					.toArray();

			this.run("cropDialog " + generateAndStoreOptions(pathToN5Dataset, asVirtual, null, !show));
		}
	}

	public static ImagePlus open(final String uri) {

		return open(uri, true);
	}

	public static ImagePlus open(final String uri, final boolean show) {

		try {
			final N5URI n5uri = new N5URI(uri);
			final String grp = N5URI.normalizeGroupPath(n5uri.getGroupPath());
			if (!grp.isEmpty()) {
				return open(uri, grp, show);
			}
		} catch (final URISyntaxException e) {}

		return open(uri, ALL_PASS);
	}

	public static ImagePlus open(final String uri, final String dataset) {

		return open(uri, dataset, true);
	}

	public static ImagePlus open(final String uri, final String dataset, final boolean show) {

		return open(uri,
				x -> {
					return norm(x.getPath()).equals(norm(dataset));
				},
				show);
	}

	public static ImagePlus open(final String uri, final Predicate<N5Metadata> filter ) {

		return open(uri, filter, true);
	}

	public static ImagePlus open(final String uri, final Predicate<N5Metadata> filter, final boolean show) {

		final N5Reader n5;
		try {
			n5 = new N5ViewerReaderFun().apply(uri);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
		final N5TreeNode node = N5DatasetDiscoverer.discover(n5);

		final Predicate<N5Metadata> datasetFilter = x -> { return x instanceof N5DatasetMetadata; };
		final Predicate<N5Metadata> totalFilter = filter == null || filter == ALL_PASS
				? datasetFilter : datasetFilter.and(filter);

		Stream<N5DatasetMetadata> metaStream = N5TreeNode.flattenN5Tree(node)
			.filter( x -> totalFilter.test(x.getMetadata()) )
				.map(x -> {
					return (N5DatasetMetadata)x.getMetadata();
				});

		N5URI n5uri;
		try {
			n5uri = new N5URI(uri);
			final String grp = N5URI.normalizeGroupPath(n5uri.getGroupPath());
			if (!grp.isEmpty()) {
				metaStream = metaStream.filter(x -> N5URI.normalizeGroupPath(x.getPath()).equals(grp));
			}
		} catch (final URISyntaxException e) {}

		final Optional<N5DatasetMetadata> meta = metaStream.findFirst();
		if (meta.isPresent()) {
			return open(n5, uri, meta.get(), show);
		} else {
			System.err.println("No arrays matching criteria found in container at: " + uri);
			return null;
		}
	}

	public static ImagePlus open(final N5Reader n5, final String uri, final N5DatasetMetadata metadata) {

		return open(n5, uri, metadata, true);
	}

	public static ImagePlus open(final N5Reader n5, final String uri, final N5DatasetMetadata metadata, final boolean show) {

		final ExecutorService exec = Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() / 2);

		return N5Importer.process(n5, uri,
				exec,
				Collections.singletonList(metadata),
				false, show, null).get(0);
	}

	private static String norm(final String groupPath) {

		return groupPath.equals("/") ? groupPath : groupPath.replaceAll("^/", "");
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them.
	 */
	public static List<ImagePlus> process(final N5Reader n5,
			final String rootPath,
			final ExecutorService exec,
			final List<N5DatasetMetadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval) {

		return process(n5, rootPath, exec, datasetMetadataList, asVirtual, cropInterval, true,
				defaultImagePlusMetadataWriters());
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them.
	 */
	public static List<ImagePlus> process(final N5Reader n5,
			final String rootPath,
			final ExecutorService exec,
			final List<N5DatasetMetadata> datasetMetadataList,
			final boolean asVirtual,
			final boolean show,
			final Interval cropInterval) {

		return process(n5, rootPath, exec, datasetMetadataList, asVirtual, cropInterval, show,
				defaultImagePlusMetadataWriters());
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them.
	 */
	public static List<ImagePlus> process(final N5Reader n5,
			final String rootPath,
			final ExecutorService exec,
			final List<N5DatasetMetadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval,
			final Map<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes) {

		return process(n5, rootPath, exec, datasetMetadataList, asVirtual, cropInterval, true, impMetaWriterTypes);
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them, if
	 * requested.
	 */
	public static List<ImagePlus> process(final N5Reader n5,
			final String rootPathArg,
			final ExecutorService exec,
			final List<N5DatasetMetadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval,
			final boolean show,
			final Map<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes) {

		// determine if the root path contains a query
		final String rootPath = rootPathArg;
		final ArrayList<ImagePlus> imgList = new ArrayList<>();
		for (final N5DatasetMetadata datasetMeta : datasetMetadataList) {
			// is this check necessary?
			if (datasetMeta == null)
				continue;

			final String d = normalPathName(datasetMeta.getPath(), n5.getGroupSeparator());
			try {

				final StorageFormat fmt = N5Factory.StorageFormat.guessStorageFromUri(URI.create(rootPathArg));
				final String fmtPrefix = fmt == null ? "" : fmt.toString().toLowerCase() + "://";

				final String n5Url = fmtPrefix + N5URI.from(n5.getURI().toString(), d, null).toString();
				final ImageplusMetadata<?> impMeta = impMetaWriterTypes.get(datasetMeta.getClass());

				// datasetMeta must have absolute path
				ImagePlus imp;
				imp = N5Importer.read(n5, exec, datasetMeta, cropInterval, asVirtual, impMeta);

				FileInfo fileInfo = imp.getOriginalFileInfo();
				if (fileInfo == null)
					fileInfo = new FileInfo();

				fileInfo.url = n5Url;
				imp.setFileInfo(fileInfo);

				record(n5Url, asVirtual, cropInterval);
				imgList.add(imp);
				if (show) {
					// set the display min and max with a heuristic:
					// set the min of the range to the min value and the max range to the 98th
					// percentile
					final ImageStatistics stats = ImageStatistics.getStatistics(imp.getProcessor());
					final double[] hist = stats.histogram();
					toCumulativeHistogram(hist);
					final double min = stats.histMin;
					final double max = min + (stats.binSize * nthPercentile(hist, 0.98));
					imp.setDisplayRange(min, max);
					imp.show();
				}

			} catch (final IOException e) {
				IJ.error("failed to read n5");
			} catch (final URISyntaxException e1) {
				IJ.error("unable to parse url: " + rootPath + "?" + d );
			}
		}
		return imgList;
	}

	/**
	 * Turns a histogram into a cumulative histogram, in place and returns the total sum.
	 * <p>
	 * After running this method, the ith element of the array will contain the sum of the elements
	 * of the 0th through ith elements of the input array.
	 *
	 * @param histogram
	 *            a histogram
	 * @return the total sum
	 */
	private static double toCumulativeHistogram(final double[] histogram) {

		double total = 0;
		for (int i = 0; i < histogram.length; i++) {
			total += histogram[i];
			histogram[i] = total;
		}

		return total;
	}

	/**
	 *
	 *
	 * @param cumulativeHistogram
	 *            a cumulative histogram
	 * @param percentile
	 *            a percentile in the range [0,1]
	 * @return the bin corresponding the the given percentile
	 *
	 */
	private static int nthPercentile(final double[] cumulativeHistogram, final double percentile) {

		final int N = cumulativeHistogram.length - 1;
		final double total = cumulativeHistogram[N];

		for (int i = N; i >= 0; i--) {
			if (cumulativeHistogram[i] <= percentile * total)
				return i + 1;
		}

		return 0;
	}


	/*
	 * Convenience method to process using the current state of this object. Can
	 * not be used directly when this plugin shows the crop dialog.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void process() {

		process(n5, selectionDialog.getN5RootPath(), exec, (List)selection.metadata, asVirtual, cropInterval, impMetaWriterTypes);
	}

	public List<ImagePlus> process(final String n5FullPath, final boolean asVirtual) {

		return process(n5FullPath, asVirtual, null);
	}

	public List<ImagePlus> process(final String n5FullPath, final boolean asVirtual, final Interval cropInterval) {
		return process( n5FullPath, asVirtual, cropInterval, true );
	}

	public List<ImagePlus> process(final String n5FullPath, final boolean asVirtual, final Interval cropInterval,
			final boolean parseAllMetadata) {

		n5 = new N5ViewerReaderFun().apply(n5FullPath);
		final String dataset = new N5BasePathFun().apply(n5FullPath);
		N5DatasetMetadata metadata;
		N5TreeNode root;
		try {
			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5,
					N5DatasetDiscoverer.fromParsers(PARSERS),
					Collections.singletonList(new OmeNgffMetadataParser()));
			if( parseAllMetadata )
			{
				root = discoverer.discoverAndParseRecursive("");
				metadata = (N5DatasetMetadata)root.getDescendant(dataset).get().getMetadata();
			}
			else {
				metadata = (N5DatasetMetadata)discoverer.parse(dataset).getMetadata();
			}
		} catch (final Exception e) {
			System.err.println("Could not parse metadata.");
			return null;
		}

		final List<ImagePlus> result = process(n5, dataset, exec, Collections.singletonList(metadata),
				asVirtual, cropInterval, show, getImagePlusMetadataWriterMap());

		n5.close();

		return result;
	}

	public List<ImagePlus> process(final String n5FullPath, final List<N5DatasetMetadata> metadataList, final boolean asVirtual, final Interval cropInterval) {

		n5 = new N5ViewerReaderFun().apply(n5FullPath);
		final String dataset = new N5BasePathFun().apply(n5FullPath);
		if (metadataList == null || metadataList.size() < 1)
			return null;

		final List<ImagePlus> result = process(n5, dataset, exec, metadataList,
				asVirtual, cropInterval, show, getImagePlusMetadataWriterMap());

		n5.close();
		return result;
	}

	public void processThread() {

		loaderThread = new Thread() {

			@Override
			public void run() {

				process();
			}
		};
		loaderThread.run();
	}

	public static class N5ViewerReaderFun implements Function<String, N5Reader> {

		public String message;

		@Override
		public N5Reader apply(final String n5UriOrPath) {

			N5Reader n5;
			if (n5UriOrPath == null || n5UriOrPath.isEmpty())
				return null;

			String rootPath = null;
			if (n5UriOrPath.contains("?")) {

				try {
					// need to strip off storage format for n5uri to correctly remove query;
					final Pair<StorageFormat, URI> fmtUri = N5Factory.StorageFormat.parseUri(n5UriOrPath);
					final StorageFormat format = fmtUri.getA();

					final N5URI n5uri = new N5URI(URI.create(fmtUri.getB().toString()));
					// add the format prefix back if it was present
					rootPath = format == null ? n5uri.getContainerPath() : format.toString().toLowerCase() + ":" + n5uri.getContainerPath();
				} catch (final URISyntaxException e) {}
			}

			if (rootPath == null)
				rootPath = upToLastExtension(n5UriOrPath);

			final N5Factory factory = new N5Factory().cacheAttributes(true);
			try {
				n5 = factory.openReader(rootPath);
			} catch (final N5Exception e) {
				IJ.handleException(e);
				return null;
			}
			return n5;
		}
	}

	private static String upToLastExtension(final String path) {

		final int i = path.lastIndexOf('.');
		if (i >= 0) {
			final int j = path.substring(i).indexOf('/');
			if (j >= 0)
				return path.substring(0, i + j);
			else
				return path;
		} else
			return path;
	}

	private static String afterLastExtension(final String path) {

		final int i = path.lastIndexOf('.');
		if (i >= 0) {
			final int j = path.substring(i).indexOf('/');
			if (j >= 0)
				return path.substring(i + j);
			else
				return "";
		} else
			return "";
	}

	private static String lastExtension(final String path) {

		final int i = path.lastIndexOf('.');
		if (i >= 0)
			return path.substring(i);
		else
			return "";
	}

	public static class N5BasePathFun implements Function<String, String> {

		public String message;

		@Override
		public String apply(final String n5UriOrPath) {

			if (n5UriOrPath.contains("?")) {
				try {
					// need to strip off storage format for n5uri to correctly remove query;
					// but can ignore the format here
					final Pair<StorageFormat, URI> fmtUri = N5Factory.StorageFormat.parseUri(n5UriOrPath);
					final N5URI n5uri = new N5URI(URI.create(fmtUri.getB().toString()));
					return n5uri.getGroupPath();
				} catch (final URISyntaxException e) {}
			}

			if (n5UriOrPath.contains(".h5") || n5UriOrPath.contains(".hdf5") || n5UriOrPath.contains(".hdf"))
				return h5DatasetPath(n5UriOrPath);
			else
				return afterLastExtension(n5UriOrPath);
		}
	}

	public static String h5DatasetPath(final String h5PathAndDataset) {

		return h5DatasetPath(h5PathAndDataset, false);
	}

	public static String h5DatasetPath(final String h5PathAndDataset, final boolean getFilePath) {

		int len = 3;
		int i = h5PathAndDataset.lastIndexOf(".h5");

		if (i < 0) {
			i = h5PathAndDataset.lastIndexOf(".hdf5");
			len = 5;
		}

		if (i < 0) {
			i = h5PathAndDataset.lastIndexOf(".hdf");
			len = 4;
		}

		if (i < 0)
			return "";
		else if (getFilePath)
			return h5PathAndDataset.substring(0, i + len);
		else
			return h5PathAndDataset.substring(i + len);
	}

	public static class IntegerToaRGBConverter implements Converter<UnsignedIntType, ARGBType> {

		@Override
		public void convert(final UnsignedIntType input, final ARGBType output) {

			output.set(input.getInt());
		}
	}

	private static String normalPathName(final String fullPath, final String groupSeparator) {

		return fullPath.replaceAll("(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "");
	}

}
