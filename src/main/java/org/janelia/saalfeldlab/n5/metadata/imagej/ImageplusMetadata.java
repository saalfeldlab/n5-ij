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

	/**
	 * The {@link net.imglib2.type.numeric.ARGBType} components an RGB
	 * {@link ImagePlus} is split into when a format needs a real channel axis: red,
	 * green and blue. Alpha (index 0) is deliberately omitted - ImageJ RGB images
	 * carry no meaningful alpha, and a constant fourth channel only adds noise.
	 */
	public static final int[] RGB_CHANNELS = new int[]{1, 2, 3};

	/**
	 * The number of channels the given {@link ImagePlus} has, counting the colour
	 * components of an RGB image.
	 * <p>
	 * ImageJ stores RGB as a single packed 32-bit channel, so
	 * {@link ImagePlus#getNChannels()} reports 1. Formats that give the components
	 * their own axis need the split count instead, and it must agree everywhere -
	 * the pixels and the axis metadata are built independently.
	 *
	 * @param imp the ImagePlus
	 * @return the number of channels
	 */
	public static int numChannels( final ImagePlus imp )
	{
		return imp.getType() == ImagePlus.COLOR_RGB ? RGB_CHANNELS.length : imp.getNChannels();
	}

	public static <T extends NativeType<T>> DatasetAttributes datasetAttributes( final ImagePlus imp )
	{
		@SuppressWarnings("unchecked")
		final Img<T> img = (Img<T>)ImageJFunctions.wrap(imp);

		final long[] dims;
		final DataType dtype;
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			// the packed ARGB value has no n5 data type; it is stored as one uint8
			// channel per colour component, so report the split form here too
			dtype = DataType.UINT8;
			dims = withChannelDimension(img.dimensionsAsLongArray(), imp);
		} else {
			dtype = N5Utils.dataType(Util.getTypeFromInterval(img));
			dims = img.dimensionsAsLongArray();

			if (dtype == null)
				throw new IllegalArgumentException("No n5 DataType for imglib2 type "
						+ Util.getTypeFromInterval(img).getClass().getName());
		}

		return new DatasetAttributes(
				dims,
				Arrays.stream(dims).mapToInt(x -> (int)x).toArray(),
				dtype,
				new RawCompression());
	}

	/**
	 * Inserts the channel dimension an RGB image gains when split, in the same
	 * XY[Z]C[T] position the exporter and the axis metadata put it.
	 */
	private static long[] withChannelDimension( final long[] dims, final ImagePlus imp )
	{
		final boolean hasT = imp.getNFrames() > 1;
		final int c = hasT ? dims.length - 1 : dims.length;

		final long[] out = new long[dims.length + 1];
		System.arraycopy(dims, 0, out, 0, c);
		out[c] = RGB_CHANNELS.length;
		System.arraycopy(dims, c, out, c + 1, dims.length - c);
		return out;
	}
}
