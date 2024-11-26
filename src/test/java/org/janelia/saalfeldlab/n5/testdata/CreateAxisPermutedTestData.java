package org.janelia.saalfeldlab.n5.testdata;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.ScaleCoordinateTransformation;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.coordinateTransformations.TranslationCoordinateTransformation;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.saalfeldlab.n5.zarr.ZarrKeyValueWriter;

import com.google.gson.JsonElement;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class CreateAxisPermutedTestData {

	public static void main(String[] args) {

		final String root = args[0];
		final String dsetC = "xyczt_c";
		final String dsetF = "xyczt_f";

		create(root, dsetC);
		toFOrder(root, dsetC, dsetF);

		permuteCandForder(root, "yx");
		permuteCandForder(root, "xyz");
		permuteCandForder(root, "xyt");
		permuteCandForder(root, "txcyz");
		permuteCandForder(root, "zytx");

		System.out.println("done");
	}

	public static void permuteCandForder(final String root, final String dstNoOrder) {

		final String dstCOrder = dstNoOrder + "_c";
		final String dstFOrder = dstNoOrder + "_f";
		permute(root, "xyczt_c", dstCOrder);
		toFOrder(root, dstCOrder, dstFOrder);
	}

	public static void create(final String root, final String dset) {

		final ImagePlus imp = IJ.openImage("/home/john/tmp/mitosis.tif");

		final N5ScalePyramidExporter exporter = new N5ScalePyramidExporter(imp, root, dset,
				N5ScalePyramidExporter.ZARR_FORMAT, "128", true,
				N5ScalePyramidExporter.DOWN_SAMPLE, N5Importer.MetadataOmeZarrKey,
				N5ScalePyramidExporter.GZIP_COMPRESSION);
		exporter.run();
	}

	/**
	 * Infer permutation from destinationDset
	 */
	public static <T extends NumericType<T> & NativeType<T>> void permuteMetadata(final ZarrKeyValueWriter zarr, final String sourceDset, final String destinationDset) {

		permuteMetadata(zarr, sourceDset, destinationDset, permutationFromName(destinationDset));
	}

	/**
	 * Infer permutation from destinationDset
	 */
	public static <T extends NumericType<T> & NativeType<T>> void permuteMetadata(final ZarrKeyValueWriter zarr, final String sourceDset, final String destinationDset, final int[] permutation) {

		final int nd = permutation.length;

		final Optional<N5TreeNode> node = N5DatasetDiscoverer.discover(zarr).getDescendant(sourceDset);
		final OmeNgffMetadata meta = (OmeNgffMetadata)node.get().getMetadata();

		final OmeNgffMultiScaleMetadata ms = meta.multiscales[0];

		final Axis[] axesOrig = ms.axes;
		final Axis[] axesNew = new Axis[permutation.length];
		AxisUtils.permute(axesOrig, axesNew, permutation);
		ArrayUtils.reverse(axesNew);

		final int nScales = ms.datasets.length;
		final String[] scalePaths = IntStream.range(0, nScales).mapToObj(i -> {
			return String.format("s%d", i);
		}).toArray(n -> {
			return new String[n];
		});

		final double[][] newScales = new double[nScales][];
		final double[][] newTranslations = new double[nScales][];

		// in this case, we'll assume to know that coordinateTransformations[0] is a scale
		// and that coordinateTransformations[1] is a translation
		for (int s = 0; s < nScales; s++)
		{
			final OmeNgffDataset dset = ms.datasets[s];
			final double[] scaleOrig = ((ScaleCoordinateTransformation)(dset.coordinateTransformations[0])).getScale();
			final double[] translationOrig = ((TranslationCoordinateTransformation)(dset.coordinateTransformations[1])).getTranslation();

			newScales[s] = permuteAndReverse(scaleOrig, permutation);
			newTranslations[s] = permuteAndReverse(translationOrig, permutation);
		}

		final OmeNgffMetadata newMeta = OmeNgffMetadata.buildForWriting(nd, destinationDset, axesNew, scalePaths, newScales, newTranslations);
		zarr.setAttribute(destinationDset, "multiscales", newMeta.multiscales);
	}

	public static double[] permuteAndReverse(double[] arr, int[] permutation) {

		final double[] out = AxisUtils.permute(arr, permutation);
		ArrayUtils.reverse(out);
		return out;
	}

	/**
	 * Infer permutation from destinationDset
	 */
	public static <T extends NumericType<T> & NativeType<T>> void permute(final String root, final String sourceDset, final String destinationDset) {

		permute(root, sourceDset, destinationDset, permutationFromName(destinationDset));
	}

	public static <T extends NumericType<T> & NativeType<T>> void permute(final String root, final String sourceDset, final String destinationDset,
			final int[] permutation) {

		final ZarrKeyValueWriter zarr = (ZarrKeyValueWriter)new N5Factory().openWriter(StorageFormat.ZARR, root);

		final String sourceScale = sourceDset + "/s0";
		final String destinationScale = destinationDset + "/s0";

		final DatasetAttributes attrs = zarr.getDatasetAttributes(sourceScale);
		final int[] blockSize = ArrayUtils.clone( attrs.getBlockSize());
		final int[] blockSizePermuted = AxisUtils.permute(blockSize, permutation);

		final CachedCellImg<T, ?> img = N5Utils.open(zarr, sourceScale);

		// removes and permutes dimensions
		final RandomAccessibleInterval<T> imgPermuted = AxisUtils.permute(
				sliceUnusedDimensions(img, permutation),
				AxisUtils.invertPermutation(AxisUtils.normalizeIndexes(permutation)));

		N5Utils.save(imgPermuted, zarr, destinationScale, blockSizePermuted, attrs.getCompression());

		permuteMetadata(zarr, sourceDset, destinationDset);
	}

	public static <T extends NumericType<T> & NativeType<T>> void toFOrder(final String root, final String sourceDset, final String destinationDset) {

		final String sourceScale = sourceDset + "/s0";
		final String destinationScale = destinationDset + "/s0";

		final ZarrKeyValueWriter zarr = (ZarrKeyValueWriter)new N5Factory().openWriter(StorageFormat.ZARR, root);
		createDataset(zarr, false, destinationScale, zarr.getDatasetAttributes(sourceScale));

		final CachedCellImg<T, ?> img = N5Utils.open(zarr, sourceScale);
		final RandomAccessibleInterval<T> imgRev = AxisUtils.reverseDimensions(img);
		try {
			N5Utils.saveRegion(imgRev, zarr, destinationScale);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}

		final JsonElement meta = zarr.getAttribute(sourceDset, "/", JsonElement.class);
		zarr.setAttribute(destinationDset, "/", meta);
	}

	public static void createDataset(
			final N5Writer zarr,
			final boolean cOrder,
			final String datasetPath,
			final DatasetAttributes attrs) throws N5Exception {

		createDataset(zarr, cOrder, datasetPath,
				attrs.getDimensions(), attrs.getBlockSize(),
				attrs.getDataType(), attrs.getCompression());
	}

	public static void createDataset(
			final N5Writer zarr,
			final boolean cOrder,
			final String datasetPath,
			final long[] dimensions,
			final int[] blockSize,
			final DataType dataType,
			final Compression compression) throws N5Exception {

		assert zarr instanceof ZarrKeyValueWriter;

		if (cOrder) {
			zarr.createDataset(datasetPath, dimensions, blockSize, dataType, compression);
		} else {
			final long[] dimsRev = ArrayUtils.clone(dimensions);
			ArrayUtils.reverse(dimsRev);

			final int[] blkSizeRev = ArrayUtils.clone(blockSize);
			ArrayUtils.reverse(blkSizeRev);

			zarr.createDataset(datasetPath, dimsRev, blkSizeRev, dataType, compression);
			zarr.setAttribute(datasetPath, "order", "F");
		}

	}

	public static final char[] DEFAULT_AXES = new char[]{'x', 'y', 'c', 'z', 't'};

	public static int[] permutationFromName(final String name) {

		final String nameNorm = name.toLowerCase();
		final String[] axesOrder = nameNorm.split("_");
		final char[] axes = axesOrder[0].toCharArray();

		final int[] p = new int[axes.length];
		for (int i = 0; i < axes.length; i++) {
			p[i] = ArrayUtils.indexOf(DEFAULT_AXES, axes[i]);
		}
		return p;
	}

	public static <T> RandomAccessibleInterval<T> sliceUnusedDimensions(RandomAccessibleInterval<T> img, int[] p) {

		RandomAccessibleInterval<T> out = img;
		// work backwards so index i and dimensions of out img stay aligned after slicing
		for (int i = img.numDimensions() - 1; i >= 0; i--) {
			if (!ArrayUtils.contains(p, i))
				out = Views.hyperSlice(out, i, 0);
		}

		return out;
	}
}
