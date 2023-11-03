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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata.ArrayOrder;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.universe.metadata.N5SingleScaleMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadataMutable;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Plugin(type = Command.class, menuPath = "File>Save As>Export HDF5/N5/Zarr")
public class N5Exporter extends ContextCommand implements WindowListener {

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

  public static enum OVERWRITE_OPTIONS {NO_OVERWRITE, OVERWRITE, WRITE_SUBSET}

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
				  N5Importer.MetadataImageJKey,
				  N5Importer.MetadataCustomKey,
				  NONE})
  private String metadataStyle = N5Importer.MetadataOmeZarrKey;

  @Parameter(label = "Thread count", required = true, min = "1", max = "256")
  private int nThreads = 1;

  @Parameter(
		  label = "Overwrite options", required = true,
		  choices = {NO_OVERWRITE, OVERWRITE, DELETE_OVERWRITE, WRITE_SUBSET},
		  description = "Determines whether overwriting datasets allows, and how overwriting occurs."
				  + "If selected will overwrite values in an existing dataset if they exist.")
  private String overwriteChoices = NO_OVERWRITE;

  @Parameter(label = "Overwrite subset offset", required = false,
		  description = "The point in pixel units where the origin of this image will be written into the n5-dataset (comma-delimited)")
  private String subsetOffset;

  private int[] blockSize;

  private final Map<String, N5MetadataWriter<?>> styles;

  private ImageplusMetadata<?> impMeta;

  private N5MetadataSpecDialog metaSpecDialog;

  private final HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

  private String omeZarrMetadataPath;

  // consider something like this eventually
//  private BiFunction<RandomAccessibleInterval<? extends NumericType<?>>,long[],RandomAccessibleInterval<?>> downsampler;

  public N5Exporter() {

	styles = new HashMap<String, N5MetadataWriter<?>>();
	styles.put(N5Importer.MetadataOmeZarrKey, new OmeNgffMetadataParser());
	styles.put(N5Importer.MetadataN5ViewerKey, new N5SingleScaleMetadataParser());
	styles.put(N5Importer.MetadataN5CosemKey, new N5CosemMetadataParser());
	styles.put(N5Importer.MetadataImageJKey, new ImagePlusLegacyMetadataParser());

	// default image plus metadata writers
	impMetaWriterTypes = new HashMap<Class<?>, ImageplusMetadata<?>>();
	impMetaWriterTypes.put(ImagePlusLegacyMetadataParser.class, new ImagePlusLegacyMetadataParser());
	impMetaWriterTypes.put(N5CosemMetadataParser.class, new CosemToImagePlus());
	impMetaWriterTypes.put(N5SingleScaleMetadataParser.class, new N5ViewerToImagePlus());
	impMetaWriterTypes.put(NgffSingleScaleAxesMetadata.class, new NgffToImagePlus());
	impMetaWriterTypes.put(OmeNgffMetadataParser.class, new NgffToImagePlus());
  }

	public static void main(String[] args) {

		final ImageJ ij = new ImageJ();
//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mitosis-xyct.tif");

//		final ImagePlus imp = IJ.openImage("/home/john/tmp/mri-stack.tif");
//		final String root = "/home/john/tmp/mri-test.n5";

		final ImagePlus imp = IJ.openImage( "/home/john/tmp/mitosis.tif" );
		final String root = "/home/john/tmp/mitosis-test.n5";


//		final String metaType = N5Importer.MetadataN5ViewerKey;
//		final String metaType = N5Importer.MetadataN5CosemKey;
//		final String metaType = N5Importer.MetadataImageJKey;
		final String metaType = N5Importer.MetadataOmeZarrKey;

//		final String dset = String.format("%s_%d", metaType, nScales);
		final String dset = String.format("%s", metaType);

		final N5Exporter exp = new N5Exporter();
		exp.setOptions(imp, root, dset, "64,64,64", metaType,
				"gzip", NO_OVERWRITE, null);
		exp.run();

//		final String p = "/a";
//		final Path path = Paths.get("", p.split("/"));
//		System.out.println( "path:" );
//		System.out.println( path );
//		System.out.println( path.getNameCount() );
//		System.out.println( path.subpath(0, path.getNameCount() - 1) );

//		System.out.println( "normal path: " );
//		System.out.println( N5URI.normalizeGroupPath("/a"));
//		System.out.println( "##" );

//		final ImageJ ij = new ImageJ();
//		ij.ui().showUI();
//
//		final Dataset dataset;
////		try {
//
////			dataset = ij.scifio().datasetIO().open(
////			ij.ui().show(dataset);
//
//			final ImagePlus imp = IJ.openImage( "/home/john/tmp/mri-stack.tif");
//			ij.ui().show(imp);
//
//			ij.command().run(N5Exporter.class, true);
//
////		} catch (final IOException e) {
////			e.printStackTrace();
////		}
	}

//  public void setOptions(
//		  final ImagePlus image,
//		  final String n5RootLocation,
//		  final String n5Dataset,
//		  final String blockSizeArg,
//		  final String metadataStyle,
//		  final String compression,
//		  final String overwriteOption,
//		  final String subsetOffset) {
//
//	  setOptions( image, n5RootLocation, n5Dataset, blockSizeArg, metadataStyle,
//			  compression, overwriteOption, subsetOffset);
//  }

  public void setOptions(
		  final ImagePlus image,
		  final String n5RootLocation,
		  final String n5Dataset,
		  final String blockSizeArg,
		  final String metadataStyle,
		  final String compression,
		  final String overwriteOption,
		  final String subsetOffset) {

	this.image = image;
	this.n5RootLocation = n5RootLocation;

	this.n5Dataset = n5Dataset;

	this.blockSizeArg = blockSizeArg;
	this.metadataStyle = metadataStyle;
	this.compressionArg = compression;
//	this.computePyramid = computePyramid;

	this.overwriteChoices = overwriteOption;
	this.subsetOffset = subsetOffset;
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
  public <T extends RealType<T> & NativeType<T>, M extends N5DatasetMetadata> void process() throws IOException, InterruptedException, ExecutionException {

//	if ( metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
//	{
//	  impMeta = new NgffToImagePlus();
//	  writeOmeZarr(numScales);
//	  return;
//	}

	final N5Writer n5 = new N5Factory().openWriter(n5RootLocation);
	final Compression compression = getCompression();
	parseBlockSize();

	N5MetadataWriter<M> writer = null;
	M metadata = null;
	if (!metadataStyle.equals(NONE)) {
	  writer = (N5MetadataWriter<M>)styles.get(metadataStyle);
	  if (writer != null)
	  {
		impMeta = impMetaWriterTypes.get(writer.getClass());
		metadata = (M)impMeta.readMetadata(image);
	  }
	}

	// check and warn re: RGB image if relevant
	//	if (image.getType() == ImagePlus.COLOR_RGB && !(writer instanceof N5ImagePlusMetadata))
	//	  log.warn("RGB images are best saved using ImageJ metatadata. Other choices "
	//			  + "may lead to unexpected behavior.");


	if (metadataStyle.equals(NONE) ||
			metadataStyle.equals(N5Importer.MetadataImageJKey) ||
			metadataStyle.equals(N5Importer.MetadataOmeZarrKey) ||
			metadataStyle.equals(N5Importer.MetadataCustomKey)) {
	  write(n5, compression, metadata, writer);
	} else {
	  writeSplitChannels(n5, compression, metadata, writer);
	}
	n5.close();
  }

  	/*
  	 * If using Ome-Ngff metadata, this method
  	 * ensures that the n5Dataset parameter is not the root directory.
  	 * If so, it sets the parameter to a child "s0" of the root.
  	 *
  	 * It returns false if the parent of the n5Dataset parameter
  	 * already contains Ome-Ngff metadata, so that any existing
  	 * metadata are not written.
  	 */

  	/*
  	 * If using Ome-Ngff metadata, this method
  	 * sets the n5Dataset parameter (where the array will be written)
  	 * to a folder called "s0" that is a child of the passed
  	 * n5Dataset parameter.
  	 */
	private boolean enforceOmeArrayNotInRoot(final N5Reader n5) {
		// final String normalPath = N5URI.normalizeGroupPath(n5Dataset);
		// if( normalPath.isEmpty() || normalPath.equals("/"))
		// {
		// n5Dataset = "s0";
		// omeZarrMetadataPath = "";
		// }
		// else
		// {
		// final Path path = Paths.get("", n5Dataset.split("/"));
		// System.out.println( path );
		// omeZarrMetadataPath = path.getNameCount() == 1 ? "/" :
		// path.subpath(0, path.getNameCount() - 1).toString();
		// }

		omeZarrMetadataPath = n5Dataset;
		n5Dataset = omeZarrMetadataPath + "/s0";

		final Optional<OmeNgffMetadata> meta = new OmeNgffMetadataParser().parseMetadata(n5, omeZarrMetadataPath);
		if (meta.isPresent()) {
			System.err.println("Ome Zarr metadata already exists at: " + omeZarrMetadataPath);
			return false;
		}

		return true;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <T extends RealType & NativeType, M extends N5DatasetMetadata> void write(
			final N5Writer n5,
			final Compression compression,
			final M metadata,
			final N5MetadataWriter<M> writer) throws IOException, InterruptedException, ExecutionException {

		/*
		 * ensure that the array is not stored in the root, because
		 * metadata must be in parent of the array for ngff v0.4
		 */
		if( !enforceOmeArrayNotInRoot( n5 ) )
		{
			// something went wrong
			return;
		}

		if (overwriteChoices.equals(WRITE_SUBSET)) {
			final long[] offset = Arrays.stream(subsetOffset.split(","))
					.mapToLong(Long::parseLong)
					.toArray();

			if (!n5.datasetExists(n5Dataset)) {
				// details don't matter, saveRegions changes this value
				final long[] dimensions = new long[image.getNDimensions()];
				Arrays.fill(dimensions, 1);

				// find data type
				final int type = image.getType();
				DataType n5type;
				switch (type) {
				case ImagePlus.GRAY8:
					n5type = DataType.UINT8;
					break;
				case ImagePlus.GRAY16:
					n5type = DataType.UINT16;
					break;
				case ImagePlus.GRAY32:
					n5type = DataType.FLOAT32;
					break;
				case ImagePlus.COLOR_RGB:
					n5type = DataType.UINT32;
					break;
				default:
					n5type = null;
				}

				final DatasetAttributes attributes = new DatasetAttributes(dimensions, blockSize, n5type, compression);
				n5.createDataset(n5Dataset, attributes);

				try {
					writer.writeMetadata(metadata, n5, n5Dataset);
				} catch (final Exception e) { }
//				writeMetadata(n5, n5Dataset, writer);
			}

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
		else
		{
			if( overwriteChoices.equals( NO_OVERWRITE ) && n5.datasetExists( n5Dataset ))
			{
				if( ui != null )
					ui.showDialog( String.format("Dataset (%s) already exists, not writing.", n5Dataset ) );
				else
					System.out.println(String.format("Dataset (%s) already exists, not writing.", n5Dataset));

				return;
			}

			if( overwriteChoices.equals( DELETE_OVERWRITE ) && n5.datasetExists( n5Dataset ))
			{

				n5.remove(n5Dataset);
			}

			// Here, either allowing overwrite, or not allowing, but the dataset
			// does not exist

			// use threadPool even for single threaded execution for progress monitoring
			final int nd = blockSize.length;
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
			progressMonitor( threadPool );
			N5IJUtils.save( image, n5, n5Dataset, blockSize, compression, threadPool);
			threadPool.shutdown();

			if( metadata != null )
			{
				if (metadataStyle.equals(N5Importer.MetadataOmeZarrKey))
				{
					// Ome-Zarr metadata goes in the parent folder
					if( !(metadata instanceof NgffSingleScaleAxesMetadata ))
						System.err.println("Unexpected metadata type");
					else if( omeZarrMetadataPath == null )
					{
						if( !enforceOmeArrayNotInRoot(n5) )
							return; // something went wrong
					}
					else
					{
						NgffSingleScaleAxesMetadata ngffSingle = ( (NgffSingleScaleAxesMetadata)metadata );
						ngffSingle = ngffSingle.modifySpatialTransform(n5Dataset, new AffineTransform( nd ));
						final OmeNgffMultiScaleMetadataMutable tmp = new OmeNgffMultiScaleMetadataMutable();

						tmp.addChild( ngffSingle );
						final ArrayOrder byteOrder = n5 instanceof ZarrKeyValueWriter ? ArrayOrder.C : ArrayOrder.F;
						final OmeNgffMetadata msMeta = finalizeMultiscaleMetadata("", tmp, byteOrder );
						if( msMeta != null )
							try {
								new OmeNgffMetadataParser().writeMetadata(msMeta, n5, omeZarrMetadataPath );
							} catch (final Exception e) {
								e.printStackTrace();
							}
					}
				}
				else {
					try {
						writer.writeMetadata(metadata, n5, n5Dataset);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
//			writeMetadata(n5, n5Dataset, writer);
		}
	}

	protected OmeNgffMetadata finalizeMultiscaleMetadata( final String path, OmeNgffMultiScaleMetadataMutable multiscaleMetadata, final N5Metadata.ArrayOrder byteOrder ) {

		if( multiscaleMetadata instanceof OmeNgffMultiScaleMetadataMutable)
		{
			final OmeNgffMultiScaleMetadataMutable ms = (OmeNgffMultiScaleMetadataMutable)multiscaleMetadata;
			final OmeNgffMultiScaleMetadata meta = new OmeNgffMultiScaleMetadata( ms.getAxes().length,
					path, path, "sampling", "0.4",
					ms.getAxes(), ms.getDatasets(), null,
					ms.coordinateTransformations, ms.metadata, byteOrder );

			return (new OmeNgffMetadata(path, new OmeNgffMultiScaleMetadata[] { meta }));
		}

		return null;
	}

	@SuppressWarnings( "unused" )
	private static long[] getOffsetForSaveSubset3d( final ImagePlus imp )
	{
		final int nd = imp.getNDimensions();
		final long[] offset = new long[ nd ];

		offset[ 0 ] = (int)imp.getCalibration().xOrigin;
		offset[ 1 ] = (int)imp.getCalibration().yOrigin;

		int j = 2;
		if( imp.getNSlices() > 1 )
			offset[ j++ ] = (int)imp.getCalibration().zOrigin;

		return offset;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T extends RealType & NativeType, M extends N5Metadata> void writeSplitChannels(
			final N5Writer n5,
			final Compression compression,
			final M metadata,
			final N5MetadataWriter<M> writer) throws IOException, InterruptedException, ExecutionException
	{
		final Img<T> img;
		if( image.getType() == ImagePlus.COLOR_RGB )
			img = (( Img< T > ) N5IJUtils.wrapRgbAsInt( image ));
		else
			img = ImageJFunctions.wrap(image);

		String datasetString = "";
		int[] blkSz = blockSize;
		for (int c = 0; c < image.getNChannels(); c++) {
			RandomAccessibleInterval<T> channelImg;
			// If there is only one channel, img may be 3d, but we don't want to slice
			// so if we have a 3d image check that the image is multichannel
			if( image.getNChannels() > 1 )
			{
				channelImg = Views.hyperSlice(img, 2, c);

				// if we slice the image, appropriately slice the block size also
				blkSz = sliceBlockSize( 2 );
			} else {
				channelImg = img;
			}

			if (metadataStyle.equals(N5Importer.MetadataN5ViewerKey)) {
				datasetString = String.format("%s/c%d/s0", n5Dataset, c);
			} else if (image.getNChannels() > 1) {
				datasetString = String.format("%s/c%d", n5Dataset, c);
			} else {
				datasetString = n5Dataset;
			}

			if( metadataStyle.equals(N5Importer.MetadataN5ViewerKey) && image.getNFrames() > 1 && image.getNSlices() == 1 )
			{
				// make a 4d image in order XYZT
				channelImg = Views.permute(Views.addDimension(channelImg, 0, 0), 2, 3);
				// expand block size
				blkSz = new int[] { blkSz[0], blkSz[1], 1, blkSz[2] };
			}

			// use threadPool even for single threaded execution for progress monitoring
			final ThreadPoolExecutor threadPool = new ThreadPoolExecutor( nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()	);
			progressMonitor( threadPool );
			N5Utils.save( channelImg, n5, datasetString, blkSz, compression, threadPool );
			threadPool.shutdown();

//			writeMetadata(n5, datasetString, writer);
			try {
				writer.writeMetadata(metadata, n5, datasetString);
			} catch (final Exception e) { }
		}
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

//	private <M extends N5Metadata> void writeMetadata(
//			final N5Writer n5,
//			final String datasetString,
//			final N5MetadataWriter<M> writer) {
//
//		if (writer != null) {
//			try {
//				@SuppressWarnings("unchecked")
//				final M meta = (M)impMeta.readMetadata(image);
//				writer.writeMetadata(meta, n5, datasetString);
//			} catch (final Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

	@Override
	public void run() {

		// add more options
		if (metadataStyle.equals(N5Importer.MetadataCustomKey)) {
			metaSpecDialog = new N5MetadataSpecDialog(this);
			metaSpecDialog.show(MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER);
		} else {
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
		process();
	  } catch (final IOException e1) {
		e1.printStackTrace();
	  } catch (final InterruptedException e1) {
		e1.printStackTrace();
	  } catch (final ExecutionException e1) {
		e1.printStackTrace();
		}
	}

	@Override
	public void windowClosed(final WindowEvent e) {}

	@Override
	public void windowActivated(final WindowEvent e) {}

}
