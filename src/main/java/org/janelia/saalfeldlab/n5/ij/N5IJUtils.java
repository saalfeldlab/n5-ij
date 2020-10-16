/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
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
	 * @param n5
	 * @param dataset
	 * @param metadata
	 * @return
	 * @throws IOException
	 * @throws ImgLibException
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

		ImagePlus imp = impImg.getImagePlus();
		if( metaReader != null && metaReader != null )
		{
			try
			{
				M metadata = metaReader.parseMetadata( n5, dataset );
				metaReader.writeMetadata( metadata, imp );
			}
			catch ( Exception e ) { System.err.println( "Warning: could not read metadata." );}
		}

		return imp;
	}

	/**
	 * Save an {@link ImagePlus} as an N5 dataset.
	 *
	 * @param imp
	 * @param n5
	 * @param datasetName
	 * @param blockSize
	 * @param compression
	 * @throws IOException
	 */
	public static void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression) throws IOException
	{
		N5ImagePlusMetadata nullWriter = null;
		save( imp, n5, datasetName, blockSize, compression, nullWriter );
	}


	/**
	 * Save an {@link ImagePlus} as an N5 dataset.
	 *
	 * @param imp
	 * @param n5
	 * @param datasetName
	 * @param blockSize
	 * @param compression
	 * @param metadata
	 * @throws IOException
	 */
	public static <T extends N5Metadata, W extends N5MetadataWriter< T > & ImageplusMetadata< T >> void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression,
			final W metaWriter ) throws IOException
	{
		final ImagePlusImg<ARGBType, ?> rai = ImagePlusImgs.from(imp);

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
				T metadata = metaWriter.readMetadata( imp );
				metaWriter.writeMetadata( metadata, n5, datasetName );
			}
			catch ( Exception e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Save and {@link ImagePlus} as an N5 dataset.  Parallelizes export using
	 * an {@link ExecutorService}.
	 *
	 * @param imp
	 * @param n5
	 * @param datasetName
	 * @param blockSize
	 * @param compression
	 * @param exec
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
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
	 * @param imp
	 * @param n5
	 * @param datasetName
	 * @param blockSize
	 * @param compression
	 * @param exec
	 * @param metadata
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static <T extends N5Metadata, W extends N5MetadataWriter< T > & ImageplusMetadata< T >> void save(
			final ImagePlus imp,
			final N5Writer n5,
			final String datasetName,
			final int[] blockSize,
			final Compression compression,
			final ExecutorService exec,
			final W metaWriter ) 
					throws IOException, InterruptedException, ExecutionException
	{
		final ImagePlusImg<ARGBType, ?> rai = ImagePlusImgs.from(imp);

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
				T metadata = metaWriter.readMetadata( imp );
				metaWriter.writeMetadata( metadata, n5, datasetName );
			}
			catch ( Exception e ) { e.printStackTrace(); }
		}
	}

	/**
	 * Save an ARGB image that contains gray scale data and uses inequality of
	 * the three colors as a mask channel into an N5 group with two uint8 datasets
	 * ('gray' and 'mask).  Mask is 1 where all color channels are equal and 0
	 * everywhere else.
	 *
	 * @param imp argb image
	 * @param n5
	 * @param groupName
	 * @param blockSize
	 * @param compression
	 * @throws IOException
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
	 * @param n5
	 * @param groupName
	 * @param blockSize
	 * @param compression
	 * @param exec
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
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
}
