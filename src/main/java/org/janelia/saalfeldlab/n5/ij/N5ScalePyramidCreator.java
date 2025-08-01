package org.janelia.saalfeldlab.n5.ij;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.MetadataUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.SpatialMetadataGroup;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataSingleScaleParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadataMutable;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.blocks.BlockAlgoUtils;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.downsample.Downsample;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.ScaleAndTranslation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import org.scijava.command.Command;
import org.scijava.command.ContextCommand;


@Plugin(type = Command.class, menuPath = "File>Save As>HDF5/N5/Zarr/OME-NGFF ...", description = "Save the current image as a new dataset or multi-scale pyramid.")
public class N5ScalePyramidCreator extends ContextCommand implements Runnable {

	@Parameter
	private LogService log;

	@Parameter
	private PrefService prefs;

	@Parameter
	private StatusService status;

	@Option(names = "-i", description = "", required = true)
	@Parameter(
			label = "Root url",
			description = "The location of the container that will store the data. May be a folder on your filesystem,\n"
					+ "an HDF5 file, or a url to cloud storage. The storage type is inferred\n"
					+ "from the extension of the url (use \".h5\", \".n5\", or \".zarr\").")
	private String containerRoot;

	@Option(names = "-d", description = "Dataset of Ngff multiscales")
	@Parameter(
			label = "Dataset",
			required = false,
			persist = false,
			initializer = "initializeDataset",
			description = "The location in the container to write data.\n"
					+ "If a pyramid is requested, arrays will be written to\n"
					+ "child paths of the given dataset, with particulars depending\n"
					+ "on the selected metadata type.")
	private String dataset = "";

	@Option(names = {"-s", "--scale-pattern"}, description = "Scale level pattern")
	@Parameter(label = "Pattern", required = false, persist = false)
	private String pattern = "s%d";

	@Option(names = {"-f", "--factors"}, description = "Downsampling factors per dimension")
	@Parameter(label = "Downsampling factors", required = false, persist = false)
	private String factors = "2";

	@Parameter(label = "Downsampling method", style = "listBox",
			choices = {N5ScalePyramidExporter.DOWN_SAMPLE, N5ScalePyramidExporter.DOWN_AVERAGE})
	private String downsampleMethod = N5ScalePyramidExporter.DOWN_AVERAGE;

	@Option(names = "-n", description = "Maximum number of scales")
	@Parameter(
			label = "Maxiumum number of scales",
			required = true,
			min = "1",
			max = "999")
	private int maxNumScales = 99;

	@Option(names = "-j", description = "Thread count")
	@Parameter(
			label = "Thread count",
			required = true,
			min = "1",
			max = "999")
	private int nThreads = 1;

	private long[] currentAbsoluteDownsampling;

	private double[] currentTranslation;

	private RandomAccessibleInterval<?> previousScaleImg;

	private N5DatasetMetadata currentChannelMetadata;

	private long[] nominalDownsamplingFactors;

	private ThreadPoolExecutor threadPool;

	@Override
	public void run() {
		processMultiscale();
	}

	@SuppressWarnings("unchecked")
	public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> void processMultiscale() {

		try( final N5Writer n5 = new N5Factory()
				.zarrDimensionSeparator("/")
				.s3UseCredentials() // need credentials if writing to s3
				.openWriter(containerRoot)) {

			final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, 
					N5DatasetDiscoverer.fromParsers(N5Importer.PARSERS),
					Collections.singletonList(new OmeNgffMetadataParser()));

			N meta = null;
			try {
				final N5TreeNode root = discoverer.discoverAndParseRecursive("");
				final Optional<N5Metadata> metaOpt = root.getDescendant(dataset)
						.filter(x -> {
							return x.getMetadata() != null;
						}).map(N5TreeNode::getMetadata);

				if (metaOpt.isPresent())
					meta = (N)metaOpt.get();

			} catch (final Exception e) {
				throw new N5Exception("Failure to parse or find data at " + dataset, e);
			}

			// Determine if we have a multiscale group or a single dataset
			List<N5DatasetMetadata> datasetsToProcess = new ArrayList<>();

			String baseGroupPath = dataset;

			if (meta instanceof SpatialMetadataGroup) {
				final SpatialMetadataGroup<?> grpMeta = (SpatialMetadataGroup<?>)meta;
				datasetsToProcess.add((N5DatasetMetadata)grpMeta.getChildrenMetadata()[0]);

			} else if (meta instanceof N5DatasetMetadata) {
				datasetsToProcess.add((N5DatasetMetadata)meta);
				// For single dataset, use parent as base group
				int lastSlash = dataset.lastIndexOf('/');
				if (lastSlash > 0) {
					baseGroupPath = dataset.substring(0, lastSlash);
				} else {
					baseGroupPath = "";
				}
			}

			if (datasetsToProcess.isEmpty()) {
				throw new N5Exception("No datasets found to process at " + dataset);
			}


			// Process each dataset (channel)
			for (int c = 0; c < datasetsToProcess.size(); c++) {

				N5DatasetMetadata datasetMeta = datasetsToProcess.get(c);
				currentChannelMetadata = copyMetadata(datasetMeta);

				// Read the base image
				RandomAccessibleInterval<T> baseImg = (RandomAccessibleInterval<T>)N5Utils.open(n5, datasetMeta.getPath());

				if (baseImg == null) {
//					log.error("Failed to open dataset: " + datasetMeta.getPath());
					System.out.println("Failed to open dataset: " + datasetMeta.getPath());
					continue;
				}

				nominalDownsamplingFactors = parseFactors(baseImg.numDimensions(), factors);

//				log.info("Processing dataset: " + datasetMeta.getPath());
				System.out.println("Processing dataset: " + datasetMeta.getPath());

				// Initialize multiscale metadata if applicable
				String channelGroupPath = getChannelGroupPath(baseGroupPath, c, datasetsToProcess.size());
				N multiscaleMetadata = initializeMultiscaleMetadata(meta, channelGroupPath);

				// Initialize downsampling tracking
				final int nd = baseImg.numDimensions();
				currentAbsoluteDownsampling = new long[nd];
				Arrays.fill(currentAbsoluteDownsampling, 1);

				final double[] baseResolution = new double[nd];
				fillResolution(datasetMeta, baseResolution);

				final double[] currentResolution = new double[nd];
				System.arraycopy(baseResolution, 0, currentResolution, 0, nd);

				currentTranslation = new double[nd];

				// Get the compression from the base dataset
				final DatasetAttributes attributes = datasetMeta.getAttributes();
				final Compression compression = attributes.getCompression();
				final int[] chunkSize = attributes.getBlockSize();

				// Start from scale 1 (0 already exists)
				RandomAccessibleInterval<T> currentImg = baseImg;
				M currentMetadata = (M)currentChannelMetadata;

				for (int s = 1; s < maxNumScales; s++) {

					// Create dataset path for this scale
					String scaleDset = getScaleDatasetName(channelGroupPath, s);
					if (n5.datasetExists(scaleDset)) {
						System.out.println("scale level " + s + " for channel " + c + " exists. Continuing...");
						continue;
					}

//					log.info("Creating scale level " + s + " for channel " + c);
					System.out.println("Creating scale level " + s + " for channel " + c);

					// Calculate relative downsampling factors
					long[] relativeFactors = getRelativeDownsampleFactors(currentMetadata, currentImg, s, currentAbsoluteDownsampling);

					// Update absolute downsampling factors
					for (int i = 0; i < nd; i++) {
						currentAbsoluteDownsampling[i] *= relativeFactors[i];
					}

					// Downsample the image
					currentImg = downsampleMethod(currentImg, relativeFactors);

					// Update resolution
					Arrays.setAll(currentResolution, i -> currentAbsoluteDownsampling[i] * baseResolution[i]);

					if (downsampleMethod.equals(N5ScalePyramidExporter.DOWN_AVERAGE)) {
						Arrays.setAll(currentTranslation, i -> {
							if (currentAbsoluteDownsampling[i] > 1)
								return baseResolution[i] * (0.5 * currentAbsoluteDownsampling[i] - 0.5);
							else
								return 0.0;
						});
					}

					// Update metadata for this scale
					currentMetadata = metadataForThisScale(scaleDset, currentMetadata, downsampleMethod,
							baseResolution, currentAbsoluteDownsampling, currentResolution, currentTranslation);

					// Write the downsampled image
					write(currentImg, n5, scaleDset, compression, chunkSize, currentMetadata);

					// Update multiscale metadata if applicable
					updateMultiscaleMetadata(multiscaleMetadata, currentMetadata);

					// Check if we should stop creating scales
					if (lastScale(chunkSize, currentImg, currentMetadata)) {
//						log.info("Stopping at scale " + s + " - reached minimum size");
						System.out.println("Stopping at scale " + s + " - reached minimum size");
						break;
					}
				}


				// Write multiscale metadata if applicable
				writeMetadata(finalizeMultiscaleMetadata(channelGroupPath, multiscaleMetadata), n5, channelGroupPath);
			}

		} catch (final IOException | InterruptedException | ExecutionException e) {
			throw new N5Exception("Failed to process multiscale pyramid", e);
		}
	}

	private static <M extends N5Metadata> boolean validateMetadata(final M meta) {

		if( meta == null )
			return true;

		if( meta instanceof SpatialMetadataGroup ) {
			SpatialMetadataGroup grpMeta = (SpatialMetadataGroup)meta;
			final int N = grpMeta.getChildrenMetadata().length;
			if (N != 1)
				return false;
		}

		if( meta instanceof N5DatasetMetadata )
			return true;

		return true;
	}

	private long[] parseFactors( int nd, String str ) {

		final long[] factors = new long[nd];
		final long[] base = Arrays.stream(str.split(",")).mapToLong(Long::parseLong).toArray();
		for( int i = 0; i < nd; i++ ) {
			if (i < base.length)
				factors[i] = base[i];
			else
				factors[i] = base[base.length - 1];
		}

		return factors;
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

	protected String getChannelGroupPath(String baseGroupPath, int channelIndex, int totalChannels) {
		if (totalChannels > 1) {
			return MetadataUtils.normalizeGroupPath(baseGroupPath + "/c" + channelIndex);
		}
		return baseGroupPath;
	}
	
	protected String getScaleDatasetName(String channelGroupPath, int scale) {
		return channelGroupPath + "/" + String.format(pattern, scale);
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> N initializeMultiscaleMetadata(final M baseMetadata, final String path) {
		// For now, only support OME-NGFF multiscale metadata
		if (baseMetadata instanceof NgffSingleScaleAxesMetadata) {
			return ((N) new OmeNgffMultiScaleMetadataMutable(path));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected static <M extends N5Metadata, N extends SpatialMetadataGroup<?>> N initializeMultiscaleMetadata(
			final M baseMultiscaleMetadata,
			final String path) {

		// For now, only support OME-NGFF multiscale metadata
		if (baseMultiscaleMetadata instanceof NgffSingleScaleAxesMetadata) {
			final OmeNgffMultiScaleMetadataMutable meta = new OmeNgffMultiScaleMetadataMutable(path);
			meta.addChild((NgffSingleScaleAxesMetadata)baseMultiscaleMetadata);
			return (N)meta;
		} else if (baseMultiscaleMetadata instanceof OmeNgffMultiScaleMetadata) {
			final OmeNgffMultiScaleMetadata base = (OmeNgffMultiScaleMetadata)baseMultiscaleMetadata;
			return (N)initializeNgffMulti(base, path);
		} else if (baseMultiscaleMetadata instanceof OmeNgffMetadata) {
			final OmeNgffMetadata base = (OmeNgffMetadata)baseMultiscaleMetadata;
			return (N)initializeNgffMulti(base.multiscales[0], path);
		}
		return null;
	}

	protected static OmeNgffMultiScaleMetadataMutable initializeNgffMulti(
			final OmeNgffMultiScaleMetadata baseMultiscaleMetadata,
			final String path) {

		final OmeNgffMultiScaleMetadataMutable meta = new OmeNgffMultiScaleMetadataMutable(path);
		final OmeNgffMultiScaleMetadata base = (OmeNgffMultiScaleMetadata)baseMultiscaleMetadata;
		for (final NgffSingleScaleAxesMetadata child : base.getChildrenMetadata()) {
			meta.addChild(child);
		}
		return meta;
	}

	protected <M extends N5DatasetMetadata> void fillResolution(final M baseMetadata, final double[] resolution) {
		if (baseMetadata == null) {
			Arrays.fill(resolution, 1);
			return;
		}

		if (baseMetadata.getClass().equals(N5SingleScaleMetadata.class)) {
			final double[] res = ((N5SingleScaleMetadata) baseMetadata).getPixelResolution();
			final int nd = Math.min(res.length, resolution.length);
			System.arraycopy(res, 0, resolution, 0, nd);
		} else if (baseMetadata instanceof N5CosemMetadata) {
			final double[] res = ((N5CosemMetadata) baseMetadata).getCosemTransform().scale;
			final int nd = Math.min(res.length, resolution.length);
			System.arraycopy(res, 0, resolution, 0, nd);
		} else if (baseMetadata instanceof NgffSingleScaleAxesMetadata) {
			final double[] res = ((NgffSingleScaleAxesMetadata) baseMetadata).getScale();
			final int nd = Math.min(res.length, resolution.length);
			System.arraycopy(res, 0, resolution, 0, nd);
		} else if (baseMetadata instanceof SpatialMetadata) {
			final AffineGet affine = ((SpatialMetadata) baseMetadata).spatialTransform();
			final int nd = affine.numTargetDimensions();
			for (int i = 0; i < nd; i++)
				resolution[i] = affine.get(i, i);
		} else {
			Arrays.fill(resolution, 1);
		}
	}

	protected <M extends N5Metadata> long[] getRelativeDownsampleFactors(final M metadata, final Interval img, final int scale,
			final long[] downsampleFactors) {

		int nd = img.numDimensions();
		final Axis[] axes = getAxes(metadata, nd);

		final long[] factors = new long[axes.length];
		for (int i = 0; i < nd; i++) {
			// only downsample spatial dimensions
			if (axes[i].getType().equals(Axis.SPACE) && img.dimension(i) > 1)
				factors[i] = nominalDownsamplingFactors[i];
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

	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> downsampleMethod(final RandomAccessibleInterval<T> img,
			final long[] factors) {

		if (downsampleMethod.equals(N5ScalePyramidExporter.DOWN_AVERAGE))
			return downsampleAvgBy2(img, factors);
		else
			return downsample(img, factors);
	}

	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsample(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {

		return Views.subsample(img, downsampleFactors);
	}

	private static <T extends NumericType<T>> RandomAccessibleInterval<T> downsampleAvgBy2(
			final RandomAccessibleInterval<T> img, final long[] downsampleFactors) {

		// ensure downsampleFactors contains only 1's and 2's
		assert Arrays.stream(downsampleFactors).allMatch(x -> (x == 1) || (x == 2));

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

		if (img.getType() instanceof NativeType) {
			return downsampleAvgBy2NativeType((RandomAccessibleInterval)img, Util.long2int(downsampleFactors), dims);
		}

		final RealRandomAccessible<T> imgE = Views.interpolate(Views.extendBorder(img), new NLinearInterpolatorFactory());
		return Views.interval(RealViews.transform(imgE, new ScaleAndTranslation(scale, translation)),
				new FinalInterval(dims));
	}

	private static <T extends NativeType<T>> RandomAccessibleInterval<T> downsampleAvgBy2NativeType(
			final RandomAccessibleInterval<T> img, final int[] downsampleFactors, final long[] dimensions) {

		final int[] cellDimensions = new int[]{32};
		final BlockSupplier<T> blocks = BlockSupplier
				.of(img.view().extend(Extension.border()))
				.andThen(Downsample.downsample(downsampleFactors));
		return BlockAlgoUtils.cellImg(blocks, dimensions, cellDimensions);
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5DatasetMetadata> M metadataForThisScale(final String newPath,
			final M baseMetadata,
			final String downsampleMethod,
			final double[] baseResolution,
			final long[] absoluteDownsamplingFactors,
			final double[] scale,
			final double[] translation) {

		if (baseMetadata == null)
			return null;

		if (baseMetadata.getClass().equals(N5SingleScaleMetadata.class)) {
			return (M)buildN5VMetadata(newPath, (N5SingleScaleMetadata)baseMetadata, downsampleMethod, baseResolution,
					Arrays.stream(absoluteDownsamplingFactors).mapToDouble(x -> (double)x).toArray());
		} else if (baseMetadata instanceof N5CosemMetadata) {
			return (M)buildCosemMetadata(newPath, (N5CosemMetadata)baseMetadata, scale, translation);
		} else if (baseMetadata instanceof NgffSingleScaleAxesMetadata) {
			return (M)buildNgffMetadata(newPath, (NgffSingleScaleAxesMetadata)baseMetadata, scale, translation);
		} else {
			return baseMetadata;
		}
	}

	protected N5SingleScaleMetadata buildN5VMetadata(
			final String path,
			final N5SingleScaleMetadata baseMetadata,
			final String downsampleMethod,
			final double[] baseResolution,
			final double[] downsamplingFactors) {

		final int nd = Math.min(baseResolution.length, 3);
		final double[] resolution = new double[nd];
		final double[] factors = new double[nd];

		if (downsampleMethod.equals(N5ScalePyramidExporter.DOWN_AVERAGE)) {
			System.arraycopy(baseResolution, 0, resolution, 0, nd);
			System.arraycopy(downsamplingFactors, 0, factors, 0, nd);
		} else {
			for (int i = 0; i < nd; i++)
				resolution[i] = baseResolution[i] * downsamplingFactors[i];

			Arrays.fill(factors, 1);
		}

		final AffineTransform3D transform = new AffineTransform3D();
		for (int i = 0; i < nd; i++)
			transform.set(resolution[i], i, i);

		return new N5SingleScaleMetadata(
				path,
				transform,
				factors,
				resolution,
				baseMetadata.getOffset(),
				baseMetadata.unit(),
				baseMetadata.getAttributes(),
				baseMetadata.minIntensity(),
				baseMetadata.maxIntensity(),
				baseMetadata.isLabelMultiset());
	}

	protected N5CosemMetadata buildCosemMetadata(
			final String path,
			final N5CosemMetadata baseMetadata,
			final double[] absoluteResolution,
			final double[] absoluteTranslation) {

		final double[] resolution = new double[absoluteResolution.length];
		System.arraycopy(absoluteResolution, 0, resolution, 0, absoluteResolution.length);

		final double[] translation = new double[absoluteTranslation.length];
		System.arraycopy(absoluteTranslation, 0, translation, 0, absoluteTranslation.length);

		return new N5CosemMetadata(
				path,
				new CosemTransform(
						baseMetadata.getCosemTransform().axes,
						resolution,
						translation,
						baseMetadata.getCosemTransform().units),
				baseMetadata.getAttributes());
	}

	protected NgffSingleScaleAxesMetadata buildNgffMetadata(
			final String path,
			final NgffSingleScaleAxesMetadata baseMetadata,
			final double[] absoluteResolution,
			final double[] absoluteTranslation) {

		final double[] resolution = new double[absoluteResolution.length];
		System.arraycopy(absoluteResolution, 0, resolution, 0, absoluteResolution.length);

		final double[] translation = new double[absoluteTranslation.length];
		System.arraycopy(absoluteTranslation, 0, translation, 0, absoluteTranslation.length);

		return new NgffSingleScaleAxesMetadata(
				path,
				resolution,
				translation,
				baseMetadata.getAxes(),
				baseMetadata.getAttributes());
	}

	protected <M extends N5DatasetMetadata, N extends SpatialMetadataGroup<?>> void updateMultiscaleMetadata(final N multiscaleMetadata,
			final M scaleMetadata) {

		if (multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable &&
				scaleMetadata instanceof NgffSingleScaleAxesMetadata) {

			final OmeNgffMultiScaleMetadataMutable ngffMs = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;
			ngffMs.addChild((NgffSingleScaleAxesMetadata)scaleMetadata);
		}
	}

	@SuppressWarnings("unchecked")
	protected <N extends SpatialMetadataGroup<?>> N finalizeMultiscaleMetadata(final String path, final N multiscaleMetadata) {

		System.out.println("finalize meta");
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T extends RealType & NativeType, M extends N5Metadata> void write(
			final RandomAccessibleInterval<T> image,
			final N5Writer n5,
			final String dataset,
			final Compression compression,
			final int[] chunkSize,
			final M metadata)
			throws IOException, InterruptedException, ExecutionException {

		// Use threadPool for parallel execution
		threadPool = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		progressMonitor(threadPool);

		N5Utils.save(image, n5, dataset, chunkSize, compression, threadPool);
		threadPool.shutdown();
		writeMetadata(metadata, n5, dataset);
	}

	@SuppressWarnings("unchecked")
	protected <M extends N5Metadata> void writeMetadata(final M metadata, final N5Writer n5, final String dataset) {

		if (metadata != null) {
			N5MetadataWriter<?> writer = null;

			if (metadata instanceof OmeNgffMetadata) {
				writer = new OmeNgffMetadataParser();
			} else if (metadata instanceof N5SingleScaleMetadata) {
				writer = new N5SingleScaleMetadataParser();
			} else if (metadata instanceof N5CosemMetadata) {
				writer = new N5CosemMetadataParser();
			} else if (metadata instanceof NgffSingleScaleAxesMetadata) {
				writer = new OmeNgffMetadataSingleScaleParser();
			}

			if (writer != null) {
				try {
					((N5MetadataWriter<M>)writer).writeMetadata(metadata, n5, dataset);
				} catch (final Exception e) {
//					log.error("Failed to write metadata for " + dataset, e);
				}
			}
		}
	}

	protected <M extends N5Metadata> boolean lastScale(final int[] chunkSize, final Interval imageDimensions,
			final M metadata) {

		// Use aggressive policy by default
		return lastScaleAggressive(chunkSize, imageDimensions, metadata);
	}

	protected <M extends N5Metadata> boolean lastScaleAggressive(final int[] chunkSize, final Interval imageDimensions, final M metadata) {

		final Axis[] axes = getAxes(metadata, imageDimensions.numDimensions());
		final int nd = axes.length;
		for (int i = 0; i < nd; i++) {
			if (axes[i].getType().equals(Axis.SPACE) && imageDimensions.dimension(i) > chunkSize[i])
				return false;
		}
		return true;
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
	}

	public static void main(final String[] args) {

		System.exit(new CommandLine(new N5ScalePyramidCreator()).execute(args));
	}

}
