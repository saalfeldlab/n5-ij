package org.janelia.saalfeldlab.n5;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.ij.N5SubsetExporter;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.junit.Test;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

public class TestRegionExport {

	private static String tempN5PathName() {

		try {
			final File tmpFile = Files.createTempDirectory("n5-region-test-").toFile();
			tmpFile.deleteOnExit();
			return tmpFile.getCanonicalPath();
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testCreate() {

		long[] trueDims = new long[]{8, 6, 2};
		final ImagePlus imp = NewImage.createImage("test",
				(int)trueDims[0], (int)trueDims[1], (int)trueDims[2],
				16, NewImage.FILL_NOISE);

		String baseDir = tempN5PathName();
		System.out.println(baseDir);

		final String rootPath = baseDir + "/test_create.n5";
		final String blockSizeString = "32";
		final String compressionString = N5ScalePyramidExporter.RAW_COMPRESSION;

		final String dsetZeroOffset = "/zeroOffset";
		final String zeroOffsetString = "0,0,0";

		// should create a dataset
		// a zero offset should write an array of the same size as the input
		final N5SubsetExporter writerZero = new N5SubsetExporter();
		writerZero.setOptions(imp, rootPath, dsetZeroOffset, zeroOffsetString, blockSizeString, compressionString);
		writerZero.run();

		final N5Reader n5 = new N5FSReader(rootPath);
		final long[] dims = n5.getDatasetAttributes(dsetZeroOffset).getDimensions();
		assertArrayEquals("zero-offset", trueDims, dims);

		// should create a dataset
		// a non-zero offset should write an array of size larger than the input
		final String dsetOffset = "/offset";
		final int[] offset = new int[]{10, 20, 30};
		final String offsetString = Arrays.stream(offset).mapToObj(Integer::toString).collect(Collectors.joining(","));

		final N5SubsetExporter writerOffset = new N5SubsetExporter();
		writerOffset.setOptions(imp, rootPath, dsetOffset, offsetString, blockSizeString, compressionString);
		writerOffset.run();

		final long[] trueOffsetDims = new long[3];
		for (int i = 0; i < 3; i++)
			trueOffsetDims[i] = trueDims[i] + offset[i];

		final long[] dimsOffset = n5.getDatasetAttributes(dsetOffset).getDimensions();
		assertArrayEquals("offset", trueOffsetDims, dimsOffset);

		n5.close();
	}

	@Test
	public void testOverwrite() {

		final long[] origDims = new long[]{16, 16, 16};
		final ImagePlus impBase = NewImage.createImage("test",
				(int)origDims[0], (int)origDims[1], (int)origDims[2],
				8, NewImage.FILL_BLACK);

		final long[] patchDims = new long[]{3, 3, 3};
		final ImagePlus impFill = NewImage.createImage("test",
				(int)patchDims[0], (int)patchDims[1], (int)patchDims[2],
				8, NewImage.FILL_WHITE);

		String baseDir = tempN5PathName();
		System.out.println(baseDir);

		final String rootPath = baseDir + "/test_patch.n5";
		final String blockSizeString = "32";
		final String compressionString = N5ScalePyramidExporter.RAW_COMPRESSION;

		final String dset = "/patch";
		final String zeroOffsetString = "0,0,0";

		// should create a dataset
		// a zero offset should write an array of the same size as the input
		final N5SubsetExporter writerZero = new N5SubsetExporter();
		writerZero.setOptions(impBase, rootPath, dset, zeroOffsetString, blockSizeString, compressionString);
		writerZero.run();

		final N5Reader n5 = new N5FSReader(rootPath);
		final CachedCellImg<UnsignedByteType, ?> origImg = N5Utils.open(n5, dset);
		final byte[] dataBefore	 = copyToArray(origImg);

		final byte[] zeros = new byte[(int)Intervals.numElements(origImg)];
		assertArrayEquals("orig data", zeros, dataBefore);


		// should create a dataset
		// a non-zero offset should write an array of size larger than the input
		final long[] offset = new long[]{1,2,3};
		final String offsetString = Arrays.stream(offset).mapToObj(Long::toString).collect(Collectors.joining(","));

		final N5SubsetExporter writerOffset = new N5SubsetExporter();
		writerOffset.setOptions(impFill, rootPath, dset, offsetString, blockSizeString, compressionString);
		writerOffset.run();

		final long[] dimsOffset = n5.getDatasetAttributes(dset).getDimensions();
		assertArrayEquals("dims unchanged", origDims, dimsOffset);

		final CachedCellImg<UnsignedByteType, ?> patchedImg = N5Utils.open(n5, dset);
		final byte[] dataPatched = copyToArray(patchedImg);

		// '-1' when represented as a signed byte
		final byte UBYTEMAX = new UnsignedByteType(255).getByte();

		// check that every value is either 0 or 255
		int numZero = 0;
		int num255 = 0;
		for( int i = 0; i < dataPatched.length; i++ )
			if( dataPatched[i] == 0)
				numZero++;
			else if( dataPatched[i] == UBYTEMAX)
				num255++;

		assertEquals("all values must be 0 or 255", dataPatched.length, numZero + num255);

		// check that every value in the patch is 255
		final long[] min = offset;
		final long[] max = new long[ min.length ];
		for( int i = 0; i < min.length; i++ )
			max[i] = min[i] + patchDims[i] - 1;

		final FinalInterval patchInterval = new FinalInterval(min, max);
		final byte[] dataInPatch = copyToArray(Views.interval(patchedImg, patchInterval));
		final byte[] data255 = new byte[dataInPatch.length];
		Arrays.fill(data255, UBYTEMAX);
		assertArrayEquals("patched data", data255, dataInPatch);

		n5.close();
	}

	private static final byte[] copyToArray( final RandomAccessibleInterval<UnsignedByteType> img ) {

		final byte[] data = new byte[(int)Intervals.numElements(img)];
		ArrayImg<UnsignedByteType, ByteArray> imgCopy = ArrayImgs.unsignedBytes(data, img.dimensionsAsLongArray());
		LoopBuilder.setImages(img, imgCopy).forEachPixel((x, y) -> {
			y.set(x.get());
		});
		return data;
	}

}
