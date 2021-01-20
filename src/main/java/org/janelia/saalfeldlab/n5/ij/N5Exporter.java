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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5Factory;
import org.janelia.saalfeldlab.n5.N5Factory.N5Options;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessException;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.ui.N5MetadataSpecDialog;
import org.scijava.ItemVisibility;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin(type = Command.class, menuPath = "File>Save As>Export N5")
public class N5Exporter implements Command, WindowListener {

	public static final String GZIP_COMPRESSION = "gzip";
	public static final String RAW_COMPRESSION = "raw";
	public static final String LZ4_COMPRESSION = "lz4";
	public static final String XZ_COMPRESSION = "xz";
	public static final String BLOSC_COMPRESSION = "blosc";

	public static final String NONE = "None";

	@Parameter(visibility = ItemVisibility.MESSAGE, required = false)
	private String message = "Export an ImagePlus to an N5 container.";

	@Parameter
	private LogService log;

	@Parameter
	private StatusService status;

	@Parameter(label = "Image")
	private ImagePlus image; // or use Dataset? - maybe later

	@Parameter(label = "N5 root" )
	private String n5RootLocation;

	@Parameter(
			label = "Dataset",
			required = false,
			description = "This argument is ignored if the N5ViewerMetadata style is selected")
	private String n5Dataset;

	@Parameter(label = "Block size")
	private String blockSizeArg;

	@Parameter(
			label = "Compresstion",
			choices = {GZIP_COMPRESSION, RAW_COMPRESSION, LZ4_COMPRESSION, XZ_COMPRESSION, BLOSC_COMPRESSION},
			style = "listBox")
	private String compressionArg = GZIP_COMPRESSION;

//    @Parameter( label = "Type",
//    			choices = { "Auto", "N5", "Zarr", "HDF5" },
//    			style="listBox" )
//	private String containerType = "Auto";

	@Parameter(
			label = "metadata type",
			description = "The style for metadata to be stored in the exported N5.",
			choices = {N5Importer.MetadataN5ViewerKey,
					N5Importer.MetadataN5CosemKey,
					N5Importer.MetadataImageJKey,
					N5Importer.MetadataCustomKey,
					NONE})
	private String metadataStyle = N5Importer.MetadataN5ViewerKey;

	@Parameter(label = "Thread count", required = false, min = "1", max = "64")
	private int nThreads = 1;

	private int[] blockSize;

	private Map<String, N5MetadataWriter<?>> styles;

	private ImageplusMetadata<?> impMeta;

	private N5MetadataSpecDialog metaSpecDialog;

	private HashMap<Class<?>, ImageplusMetadata<?>> impMetaWriterTypes;

	public N5Exporter()
	{
		styles = new HashMap<String, N5MetadataWriter<?>>();
		styles.put(N5Importer.MetadataN5ViewerKey, new N5SingleScaleMetadata());
		styles.put(N5Importer.MetadataN5CosemKey, new N5CosemMetadata("", null, null));
		styles.put(N5Importer.MetadataImageJKey, new N5ImagePlusMetadata(""));

		// default image plus metadata writers
		impMetaWriterTypes = new HashMap<Class<?>, ImageplusMetadata<?>>();
		impMetaWriterTypes.put(N5ImagePlusMetadata.class, new N5ImagePlusMetadata(""));
		impMetaWriterTypes.put(N5CosemMetadata.class, new N5CosemMetadata("", null, null));
		impMetaWriterTypes.put(N5SingleScaleMetadata.class, new N5SingleScaleMetadata());
		impMetaWriterTypes.put(DefaultMetadata.class, new DefaultMetadata("", 1));
	}

	public void setOptions(
			final ImagePlus image,
			final String n5RootLocation,
			final String n5Dataset,
			final String blockSizeArg,
			final String metadataStyle,
			final String compression)
	{
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
		impMetaWriterTypes.put(MetadataTemplateMapper.class, new ImagePlusMetadataTemplate(""));
	}

	@SuppressWarnings("unchecked")
	public <T extends RealType<T> & NativeType<T>, M extends N5Metadata> void process() throws IOException, DataAccessException, InterruptedException, ExecutionException {

		final Compression compression = getCompression();
		blockSize = Arrays.stream(blockSizeArg.split(",")).mapToInt(x -> Integer.parseInt(x)).toArray();

//		final N5Writer n5 = getWriter();
		N5Writer n5 = N5Factory.createN5Writer( new N5Options( n5RootLocation, blockSize ));

		N5MetadataWriter<M> writer = null;
		if (!metadataStyle.equals(NONE)) {
			writer = (N5MetadataWriter<M>)styles.get(metadataStyle);
			if( writer != null )
				impMeta = impMetaWriterTypes.get(writer.getClass());
		}

		if (metadataStyle.equals(NONE) ||
				metadataStyle.equals(N5Importer.MetadataImageJKey) ||
				metadataStyle.equals(N5Importer.MetadataCustomKey)) {
			write(n5, compression, writer);
		} else {
			writeSplitChannels(n5, compression, writer);
		}
		n5.close();
	}

	private <T extends RealType<T> & NativeType<T>, M extends N5Metadata> void write(
			final N5Writer n5,
			final Compression compression,
			final N5MetadataWriter<M> writer) throws IOException, InterruptedException, ExecutionException
	{
		N5IJUtils.save( image, n5, n5Dataset, blockSize, compression );
		writeMetadata( n5, n5Dataset, writer );
	}

	private <T extends RealType<T> & NativeType<T>, M extends N5Metadata> void writeSplitChannels(
			final N5Writer n5,
			final Compression compression,
			final N5MetadataWriter<M> writer) throws IOException, InterruptedException, ExecutionException
	{
		final Img<T> img = ImageJFunctions.wrap(image);
		String datasetString = "";
		for (int c = 0; c < image.getNChannels(); c++) {
			RandomAccessibleInterval<T> channelImg;
			if (img.numDimensions() >= 4) {
				channelImg = Views.hyperSlice(img, 2, c);
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

			if (nThreads > 1)
			{
				N5Utils.save( channelImg, n5, datasetString, blockSize, compression, Executors.newFixedThreadPool( nThreads ) );
			}
			else
			{
				N5Utils.save(channelImg, n5, datasetString, blockSize, compression);
			}

			writeMetadata(n5, datasetString, writer);
		}
	}

	private <M extends N5Metadata> void writeMetadata(
			final N5Writer n5,
			final String datasetString,
			final N5MetadataWriter<M> writer) {

		if (writer != null) {
			try {
				@SuppressWarnings("unchecked")
				final M meta = (M)impMeta.readMetadata(image);
				writer.writeMetadata(meta, n5, datasetString);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

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
			} catch (final DataAccessException e) {
				e.printStackTrace();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			} catch (final ExecutionException e) {
				e.printStackTrace();
			}
		}
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
		impMetaWriterTypes.put(MetadataTemplateMapper.class, new ImagePlusMetadataTemplate(""));
		try {
			process();
		} catch (final IOException e1) {
			e1.printStackTrace();
		} catch (final DataAccessException e1) {
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
