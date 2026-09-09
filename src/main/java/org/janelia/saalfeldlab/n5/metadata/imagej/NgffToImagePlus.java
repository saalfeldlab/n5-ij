package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import ij.ImagePlus;

/**
 * The implementation of {@link ImageplusMetadata} for Ngff (i.e. OME-Zarr).
 * <p>
 * An important implementation detail is that this class converts between the
 * "pixel origin" and the "physical translation" representations used by ImageJ
 * and OME-Zarr respectively.
 * <p>
 * The origin reported by ImageJ should be interpreted as "which pixel maps to
 * the origin", or the pixel coordinate that corresponds to origin of the
 * physical coordinate system. Given pixel width/height/depth in s, and the
 * origin as o, physical = s * (pixel - o) where physical and pixel are
 * coordinates in their respective coordinate systems.
 * <p>
 * OME-Zarr reports a translation, which should be interpreted as the
 * translation in physical units of the origin pixel. Or equivalently, the
 * position in physical units of the origin pixel. Given pixel
 * width/height/depth in s, and the translation as t, physical = s * pixel + t
 */
public class NgffToImagePlus extends SpatialMetadataToImagePlus<NgffSingleScaleAxesMetadata> {

	@Override
	public void writeMetadata(final NgffSingleScaleAxesMetadata t, final ImagePlus ip) throws IOException {

		ip.setTitle(t.getPath());
		int numChannels = 0;
		int numTimes = 0;
		int numZ = 0;
		int numSpace = 0;

		int xIdx = -1, yIdx = -1, cIdx = -1, zIdx = -1, tIdx = -1;
		for (int i = 0; i < t.getAxes().length; i++) {

			final Axis axis = t.getAxis(i);
			if (axis.getType().equals(Axis.TIME)) {
				numTimes = (int) t.getAttributes().getDimensions()[i];
				tIdx = i;

				final String timeUnit = axis.getUnit();
				if( timeUnit != null && !timeUnit.isEmpty())
					ip.getCalibration().setTimeUnit(axis.getUnit());
			}

			if (axis.getType().equals(Axis.CHANNEL)) {
				numChannels = (int) t.getAttributes().getDimensions()[i];
				cIdx = i;
			}

			if( axis.getType().equals(Axis.SPACE))
			{
				numSpace++;

				final String spaceUnit = axis.getUnit();
				if( spaceUnit != null && !spaceUnit.isEmpty())
					ip.getCalibration().setUnit(spaceUnit);

				if( numSpace == 1 )
					xIdx = i;
				else if ( numSpace == 2 )
					yIdx = i;
				else if ( numSpace == 3 )
					zIdx = i;

				if( numSpace > 2 )
					numZ = (int)t.getAttributes().getDimensions()[i];
			}
		}


		// permuting data if axes are in non-standard order
		// must happen before calling this method
		
		double[] scale = t.getScale();
		double[] translation = t.getTranslation();
		double[] origin = originFromTranslation(translation, scale);
		System.out.println(Arrays.toString(origin));
		// setDimensions can't handle zeros, so set these to one if they're zero
		numChannels = numChannels == 0 ? 1 : numChannels;
		numZ = numZ == 0 ? 1 : numZ;
		numTimes = numTimes == 0 ? 1 : numTimes;
		if( xIdx >= 0 ) {
			ip.getCalibration().pixelWidth = t.getScale()[xIdx];
			ip.getCalibration().xOrigin = origin[xIdx];
		}

		if( yIdx >= 0 ) {
			ip.getCalibration().pixelHeight = t.getScale()[yIdx];
			ip.getCalibration().yOrigin = origin[yIdx];
		}

		if( zIdx >= 0 ) {
			ip.getCalibration().pixelDepth = t.getScale()[zIdx];
			ip.getCalibration().zOrigin = origin[zIdx];
		}

		if( tIdx > 0 )
			ip.getCalibration().frameInterval = t.getScale()[tIdx];

	}

	@Override
	public NgffSingleScaleAxesMetadata readMetadata(final ImagePlus ip) throws IOException {

		final int nc = ip.getNChannels();
		final int nz = ip.getNSlices();
		final int nt = ip.getNFrames();

		int N = 2;
		if (nz > 1)
			N++;

		if (nc > 1)
			N++;

		if (nt > 1)
			N++;

		final Axis[] axes = new Axis[N];
		final double[] scale = new double[N];
		final double[] origin = new double[N];

		final String spaceUnit = parseUnitWithWarning(ip.getCalibration().getUnit());
		axes[0] = new Axis(Axis.SPACE, "x", spaceUnit);
		scale[0] = ip.getCalibration().pixelWidth;
		origin[0] = ip.getCalibration().xOrigin;

		axes[1] = new Axis(Axis.SPACE, "y", spaceUnit);
		scale[1] = ip.getCalibration().pixelHeight;
		origin[1] = ip.getCalibration().yOrigin;

		int k = 2;
		// channels
		if (nc > 1) {
			axes[k] = new Axis(Axis.CHANNEL, "c", null);
			scale[k] = 1;
			origin[k] = 0;
			k++;
		}

		// space z
		if (nz > 1) {
			axes[k] = new Axis(Axis.SPACE, "z", spaceUnit);
			scale[k] = ip.getCalibration().pixelDepth;
			origin[k] = ip.getCalibration().zOrigin;
			k++;
		}

		// time
		if (nt > 1) {
			final String timeUnit = parseUnitWithWarning(ip.getCalibration().getTimeUnit());
			axes[k] = new Axis(Axis.TIME, "t", timeUnit);
			scale[k] = ip.getCalibration().frameInterval;
			if( scale[k] == 0.0 )
				scale[k] = 1.0;

			origin[k] = 0;
			k++;
		}

		final double[] translation =  translationFromOrigin(origin, scale);
		System.out.println(Arrays.toString(translation));
		final boolean noOffset = Arrays.stream(origin).allMatch( x -> x == 0.0 );
		if( noOffset )
			return new NgffSingleScaleAxesMetadata("", scale, null, axes, ImageplusMetadata.datasetAttributes(ip));
		else
			return new NgffSingleScaleAxesMetadata("", scale, translation, axes, ImageplusMetadata.datasetAttributes(ip));
	}
	
	private String parseUnitWithWarning(final String unitString) {
		final Unit unit = Unit.fromString(unitString);
		final String normalUnit;
		if( unit == null ) {
			System.err.println("WARNING: could not infer unit from (" + unitString + 
					"). Will use it as the unit directly, but may be invalid.");
			normalUnit = unitString;
		}
		else {
			normalUnit = unit.toString();
		}	
		return normalUnit;
	}

	public static OmeNgffMultiScaleMetadata buildMetadata(final ImagePlus image, final String path, final DatasetAttributes[] dsetAttrs,
			final OmeNgffDataset[] datasets) {

		final int nc = image.getNChannels();
		final int nz = image.getNSlices();
		final int nt = image.getNFrames();
		final String unit = image.getCalibration().getUnit();

		int N = 2;
		if (nc > 1) {
			N++;
		}
		if (nz > 1) {
			N++;
		}
		if (nt > 1) {
			N++;
		}
		final Axis[] axes = new Axis[N];
		final double[] pixelSpacing = new double[N];

		axes[0] = new Axis(Axis.SPACE, "x", unit);
		pixelSpacing[0] = image.getCalibration().pixelWidth;

		axes[1] = new Axis(Axis.SPACE, "y", unit);
		pixelSpacing[1] = image.getCalibration().pixelHeight;

		int d = 2;
		if (nc > 1) {
			axes[d] = new Axis(Axis.CHANNEL, "c", "");
			pixelSpacing[d] = 1.0;
			d++;
		}

		if (nz > 1) {
			axes[d] = new Axis(Axis.SPACE, "z", unit);
			pixelSpacing[d] = image.getCalibration().pixelDepth;
			d++;
		}

		if (nt > 1) {
			axes[d] = new Axis(Axis.TIME, "t", image.getCalibration().getTimeUnit());
			pixelSpacing[d] = image.getCalibration().frameInterval;
			d++;
		}

		final String name = image.getTitle();
		final String type = "sampling";
		final String version = "0.4";

		return new OmeNgffMultiScaleMetadata(
			N, path, name, type, version, axes,
			datasets, null, dsetAttrs,
			null); // no global coordinate transforms of downsampling metadata
	}

	public static OmeNgffMultiScaleMetadata buildMetadata(final NgffSingleScaleAxesMetadata meta, final String name, final String path, final DatasetAttributes[] dsetAttrs,
			final OmeNgffDataset[] datasets) {

		final int N = meta.getScale().length;

		// need to reverse the axes if the arrays are in C order
		final String type = "sampling";
		final String version = "0.4";

		return new OmeNgffMultiScaleMetadata(
			N, path, name, type, version, meta.getAxes(),
			datasets, null,  dsetAttrs,
			null); // no global coordinate transforms of downsampling metadata
	}

	/**
	 * Returns a pixel origin from a physical translation.
	 * <p>
	 * See class level javadoc for details.
	 * 
	 * @param translation the physical translation (NGFF)
	 * @param scale       the pixel scale
	 * @return the pixel origin (ImageJ)
	 */
	public static double[] originFromTranslation(double[] translation, double[] scale) {

		final double[] origin = new double[translation.length];
		for (int i = 0; i < translation.length; i++) {
			origin[i] = -translation[i] / scale[i];
		}
		return origin;
	}

	/**
	 * Returns a physical translation from a pixel origin.
	 * <p>
	 * See class level javadoc for details.
	 * 
	 * @param origin the pixel origin (ImageJ)
	 * @param scale  the pixel scale
	 * @return the physical translation (NGFF)
	 */
	public static double[] translationFromOrigin(double[] origin, double[] scale) {
		
		final double[] translation = new double[origin.length];
		for (int i = 0; i < origin.length; i++) {
			translation[i] = -(scale[i] * origin[i]);
		}
		return translation;
	}

}
