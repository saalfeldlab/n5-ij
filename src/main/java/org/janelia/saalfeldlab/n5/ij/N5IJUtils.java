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
import java.util.concurrent.ExecutorService;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.Img;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.img.imageplus.IntImagePlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

/**
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public class N5IJUtils {

	public static <T extends NativeType<T> & NumericType<T>> ImagePlus load(
			final N5Reader n5,
			final String dataset ) throws IOException, ImgLibException {
		return load( n5, dataset, null );
	}

	/**
	 * Loads and N5 dataset into an {@link ImagePlus}.  Other than
	 * {@link N5Utils#open(N5Reader, String)} which uses {@link LazyCellImg},
	 * the data is actually loaded completely into memory.
	 *
	 * @param <T> the image data type.
	 * @param <M> the metadata type
	 * @param <W> the metadata parser type
	 * @param n5 the reader
	 * @param dataset the dataset
	 * @param metaReader an optional metadata reader
	 * @return the ImagePlus
	 * @throws IOException io exception
	 * @throws ImgLibException imglib2 exception
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <T extends NativeType<T> & NumericType<T>, M extends N5Metadata, W extends N5MetadataParser< M > & ImageplusMetadata< M > > ImagePlus load(
			final N5Reader n5,
			final String dataset,
			final W metaReader ) throws IOException, ImgLibException
	{
		final RandomAccessibleInterval<T> rai = N5Utils.open(n5, dataset);
		final DatasetAttributes attributes = n5.getDatasetAttributes(dataset);
		final long[] dimensions = attributes.getDimensions();

		M metadata = null;
		if( metaReader != null && metaReader != null )
		{
			try
			{
				metadata = metaReader.parseMetadata( n5, dataset );
			}
			catch ( final Exception e ) { System.err.println( "Warning: could not read metadata." );}
		}

		final ImagePlusImg<T, ?> impImg;
		switch (attributes.getDataType()) {
		case UINT8:
			impImg = (ImagePlusImg)ImagePlusImgs.unsignedBytes(dimensions);
			break;
		case INT8:
			impImg = (ImagePlusImg)ImagePlusImgs.bytes(dimensions);
			break;
		case UINT16:
			impImg = (ImagePlusImg)ImagePlusImgs.unsignedShorts(dimensions);
			break;
		case INT16:
			impImg = (ImagePlusImg)ImagePlusImgs.shorts(dimensions);
			break;
		case UINT32:
			impImg = (ImagePlusImg)ImagePlusImgs.unsignedInts(dimensions);
			break;
		case INT32:
			impImg = (ImagePlusImg)ImagePlusImgs.ints(dimensions);
			break;
		case FLOAT32:
			impImg = (ImagePlusImg)ImagePlusImgs.floats(dimensions);
			break;
		default:
			System.err.println("Data type " + attributes.getDataType() + " not supported in ImageJ.");
			return null;
		}

		for (final Pair<T, T> pair : Views.flatIterable(Views.interval(Views.pair(rai, impImg), rai)))
			pair.getB().set(pair.getA());

		final ImagePlus imp = impImg.getImagePlus();
		if( metadata != null )
			metaReader.writeMetadata( metadata, imp );

		return imp;
	}

	/**
	 * Save an {@link ImagePlus} as an N5 dataset.
	 *
	 * @param imp the ImagePlus
	 * @param n5 the writer 
	 * @param datasetName the dataset name
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @throws IOException io exception
	 */
	public static void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression) throws IOException
	{
		final N5ImagePlusMetadata nullWriter = null;
		save( imp, n5, datasetName, blockSize, compression, nullWriter );
	}


	/**
	 * Save an {@link ImagePlus} as an N5 dataset.
	 *
	 * @param <T> the image data type
	 * @param <M> the image data type
	 * @param <W> the metadata writer type
	 * @param imp the ImagePlus
	 * @param n5 the writer
	 * @param datasetName the dataset name
	 * @param blockSize the block size
	 * @param compression the compression type 
	 * @param metaWriter (optional) metadata writer
	 * @throws IOException io exception
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends RealType<T> & NativeType<T>, M extends N5Metadata, W extends N5MetadataWriter< M > & ImageplusMetadata< M >>
		void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression,
			final W metaWriter ) throws IOException
	{
		final Img<T> rai;
		if( imp.getType() == ImagePlus.COLOR_RGB )
			rai = ( Img< T > ) wrapRgbAsInt( imp );
		else
			rai = ImageJFunctions.wrap(imp);

		N5Utils.save(
				rai,
				n5,
				datasetName,
				blockSize,
				compression);

		if( metaWriter != null && metaWriter !=null )
		{
			try
			{
				final M metadata = metaWriter.readMetadata( imp );
				metaWriter.writeMetadata( metadata, n5, datasetName );
			}
			catch ( final Exception e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Save and {@link ImagePlus} as an N5 dataset.  Parallelizes export using
	 * an {@link ExecutorService}.
	 *
	 * @param imp the ImagePlus
	 * @param n5 the writer
	 * @param datasetName the dataset name
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @param exec executor
	 * @throws IOException io exception
	 * @throws InterruptedException interrupted
	 * @throws ExecutionException execution
	 */
	public static void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression,
			final ExecutorService exec
			) throws IOException, InterruptedException, ExecutionException
	{
		save( imp, n5, datasetName, blockSize, compression, exec, null );
	}

	/**
	 * Save and {@link ImagePlus} as an N5 dataset.  Parallelizes export using
	 * an {@link ExecutorService}.
	 *
	 * @param <T> the image data type.
	 * @param <M> the metadata type.
	 * @param <W> the metadata writer type.
	 * @param imp the ImagePlus
	 * @param n5 the writer
	 * @param datasetName the dataset name
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @param exec the executor
	 * @param metaWriter (optional) metadata writer
	 * @throws IOException io
	 * @throws InterruptedException interrupted
	 * @throws ExecutionException execution
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends RealType<T> & NativeType<T>, M extends N5Metadata, W extends N5MetadataWriter< M > & ImageplusMetadata< M >> 
		void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression,
			final ExecutorService exec,
			final W metaWriter )
					throws IOException, InterruptedException, ExecutionException
	{
		final Img<T> rai;
		if( imp.getType() == ImagePlus.COLOR_RGB )
			rai = ( Img< T > ) wrapRgbAsInt( imp );
		else
			rai = ImageJFunctions.wrap(imp);

		N5Utils.save(
				rai,
				n5,
				datasetName,
				blockSize,
				compression,
				exec);

		if( metaWriter != null && metaWriter !=null )
		{
			try
			{
				final M metadata = metaWriter.readMetadata( imp );
				metaWriter.writeMetadata( metadata, n5, datasetName );
			}
			catch ( final Exception e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Save an ARGB image that contains gray scale data and uses inequality of
	 * the three colors as a mask channel into an N5 group with two uint8 datasets
	 * ('gray' and 'mask).  Mask is 1 where all color channels are equal and 0
	 * everywhere else.
	 *
	 * @param imp argb image
	 * @param n5 the writer
	 * @param groupName the base path of the datasets
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @throws IOException io
	 */
	public static void saveMaskedUnsignedByte(
			final ImagePlus imp,
			final N5Writer n5,
			final String groupName,
			final int[] blockSize,
			final Compression compression) throws IOException {

		final ImagePlusImg<ARGBType, ?> rai = ImagePlusImgs.from(imp);

		final RandomAccessibleInterval<UnsignedByteType> mask = Converters.convert(
				(RandomAccessibleInterval<ARGBType>)rai,
				(argb, uint8) -> {
					final int argbValue = argb.get();
					final int r = ARGBType.red(argbValue);
					final int g = ARGBType.green(argbValue);
					final int b = ARGBType.blue(argbValue);
					if (r == g && g == b)
						uint8.set(1);
					else
						uint8.set(0);
				},
				new UnsignedByteType());

		final RandomAccessibleInterval<UnsignedByteType> gray = Converters.convert(
				(RandomAccessibleInterval<ARGBType>)rai,
				(argb, uint8) -> {
					uint8.set(ARGBType.red(argb.get()));
				},
				new UnsignedByteType());

		n5.createGroup(groupName);
		N5Utils.save(
				mask,
				n5, groupName + "/mask",
				blockSize,
				compression);
		N5Utils.save(
				gray,
				n5, groupName + "/gray",
				blockSize,
				compression);
	}

	/**
	 * Save an ARGB image that contains gray scale data and uses inequality of
	 * the three colors as a mask channel into an N5 group with two uint8 datasets
	 * ('gray' and 'mask).  Mask is 1 where all color channels are equal and 0
	 * everywhere else.  Parallelizes export using an {@link ExecutorService}.
	 *
	 * @param imp ARGB image
	 * @param n5 the writer
	 * @param groupName the base path of the datasets
	 * @param blockSize the block size
	 * @param compression the compression type
	 * @param exec the executor service
	 * @throws IOException io
	 * @throws InterruptedException interrupted
	 * @throws ExecutionException execution
	 */
	public static void saveMaskedUnsignedByte(
			final ImagePlus imp,
			final N5Writer n5,
			final String groupName,
			final int[] blockSize,
			final Compression compression,
			final ExecutorService exec) throws IOException, InterruptedException, ExecutionException {

		final ImagePlusImg<ARGBType, ?> rai = ImagePlusImgs.from(imp);

		final RandomAccessibleInterval<UnsignedByteType> mask = Converters.convert(
				(RandomAccessibleInterval<ARGBType>)rai,
				(argb, uint8) -> {
					final int argbValue = argb.get();
					final int r = ARGBType.red(argbValue);
					final int g = ARGBType.green(argbValue);
					final int b = ARGBType.blue(argbValue);
					if (r == g && g == b)
						uint8.set(1);
					else
						uint8.set(0);
				},
				new UnsignedByteType());

		final RandomAccessibleInterval<UnsignedByteType> gray = Converters.convert(
				(RandomAccessibleInterval<ARGBType>)rai,
				(argb, uint8) -> {
					uint8.set(ARGBType.red(argb.get()));
				},
				new UnsignedByteType());

		n5.createGroup(groupName);
		N5Utils.save(
				mask,
				n5, groupName + "/mask",
				blockSize,
				compression,
				exec);
		N5Utils.save(
				gray,
				n5, groupName + "/gray",
				blockSize,
				compression,
				exec);
	}

	public static Img< UnsignedIntType > wrapRgbAsInt( final ImagePlus imp )
	{
		if ( imp.getType() != ImagePlus.COLOR_RGB )
			return null;

		final IntImagePlus< UnsignedIntType > container = new IntImagePlus<>( imp );

		// create a Type that is linked to the container
		final UnsignedIntType linkedType = new UnsignedIntType( container );

		// pass it to the DirectAccessContainer
		container.setLinkedType( linkedType );

		return container;
	}
}
