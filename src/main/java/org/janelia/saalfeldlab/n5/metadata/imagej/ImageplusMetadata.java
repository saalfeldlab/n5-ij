package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.util.Util;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;

/**
 * A interface for reading and writing metadata to an {@link ImagePlus}.
 *
 * @param <T>
 *            the metadata type
 * @author John Bogovic
 */
public interface ImageplusMetadata<T extends N5DatasetMetadata> {

	/**
	 * Modify the metadata of the {@link ImagePlus} according to the given
	 * metadata.
	 *
	 * @param t
	 *            metadata
	 * @param ip
	 *            ImagePlus
	 * @throws IOException
	 *             the io exception
	 */
	public void writeMetadata(T t, ImagePlus ip) throws IOException;

	/**
	 * Create and return a new metadata object from the given {@link ImagePlus}.
	 *
	 * @param ip the ImagePlus
	 * @return the metadata extracted from the ImagePlus
	 * @throws IOException the io exception
	 */
	public T readMetadata(ImagePlus ip) throws IOException;

	public static <T extends NativeType<T>> DatasetAttributes datasetAttributes( final ImagePlus imp )
	{
		@SuppressWarnings("unchecked")
		final Img<T> img = (Img<T>)ImageJFunctions.wrap(imp);
		final DataType dtype = N5Utils.dataType(Util.getTypeFromInterval(img));
		final long[] dims = img.dimensionsAsLongArray();

		return new DatasetAttributes(
				dims,
				Arrays.stream(dims).mapToInt(x -> (int)x).toArray(),
				dtype,
				new RawCompression());
	}
}
