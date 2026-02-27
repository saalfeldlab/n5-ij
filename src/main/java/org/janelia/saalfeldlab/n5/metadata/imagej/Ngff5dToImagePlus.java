package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Unit;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.NgffSingleScaleAxesMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata.OmeNgffDataset;

import ij.ImagePlus;

public class Ngff5dToImagePlus extends SpatialMetadataToImagePlus<NgffSingleScaleAxesMetadata> {
	
	// 01234
	// XYZCT
	static final int X = 0;
	static final int Y = 1;
	static final int Z = 2;
	static final int C = 3;
	static final int T = 4;

	@Override
	public void writeMetadata(final NgffSingleScaleAxesMetadata t, final ImagePlus ip) throws IOException {

		ip.setTitle(t.getPath());

		final String spaceUnit = t.getAxis(X).getUnit();
		if (spaceUnit != null && !spaceUnit.isEmpty())
			ip.getCalibration().setUnit(spaceUnit);

		ip.getCalibration().pixelWidth = t.getScale()[X];
		ip.getCalibration().xOrigin = t.getTranslation()[X];

		ip.getCalibration().pixelHeight = t.getScale()[Y];
		ip.getCalibration().yOrigin = t.getTranslation()[Y];
	
		ip.getCalibration().pixelDepth = t.getScale()[Z];
		ip.getCalibration().zOrigin = t.getTranslation()[Z];

		ip.getCalibration().frameInterval = t.getScale()[T];
		final String timeUnit = t.getAxis(T).getUnit();
		if (timeUnit != null && !timeUnit.isEmpty())
			ip.getCalibration().setTimeUnit(timeUnit);
	}

	@Override
	public NgffSingleScaleAxesMetadata readMetadata(final ImagePlus ip) throws IOException {

		final int N = 5;
		final Axis[] axes = new Axis[N];
		final double[] scale = new double[N];
		final double[] offset = new double[N];

		final String spaceUnit = parseUnitWithWarning(ip.getCalibration().getUnit());
		axes[X] = new Axis(Axis.SPACE, "x", spaceUnit);
		scale[X] = ip.getCalibration().pixelWidth;
		offset[X] = ip.getCalibration().xOrigin;

		axes[Y] = new Axis(Axis.SPACE, "y", spaceUnit);
		scale[Y] = ip.getCalibration().pixelHeight;
		offset[Y] = ip.getCalibration().yOrigin;
		
		// space z
		axes[Z] = new Axis(Axis.SPACE, "z", spaceUnit);
		scale[Z] = ip.getCalibration().pixelDepth;
		offset[Z] = ip.getCalibration().zOrigin;

		// channels
		axes[C] = new Axis(Axis.CHANNEL, "c", null);
		scale[C] = 1;
		offset[C] = 0;


		// time
		final String timeUnit = parseUnitWithWarning(ip.getCalibration().getTimeUnit());
		axes[T] = new Axis(Axis.TIME, "t", timeUnit);
		scale[T] = ip.getCalibration().frameInterval;
		if (scale[T] == 0.0) {
			scale[T] = 1.0; // to avoid singular affine matrix
		}
		offset[T] = 0;

		final boolean noOffset = Arrays.stream(offset).allMatch( x -> x == 0.0 );
		if (noOffset)
			return new NgffSingleScaleAxesMetadata("", scale, null, axes, ImageplusMetadata.datasetAttributes(ip));
		else
			return new NgffSingleScaleAxesMetadata("", scale, offset, axes, ImageplusMetadata.datasetAttributes(ip));
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

		// need to reverse the axes if the arrays are in C order
		final Axis[] axesToWrite;
		if( dsetAttrs != null )
			axesToWrite = OmeNgffMultiScaleMetadata.reverseIfCorder( dsetAttrs[0], axes );
		else
			axesToWrite = axes;

		final String name = image.getTitle();
		final String type = "sampling";
		final String version = "0.4";

		return new OmeNgffMultiScaleMetadata(
			N, path, name, type, version, axesToWrite,
			datasets, dsetAttrs,
			null, null); // no global coordinate transforms of downsampling metadata
	}

	public static OmeNgffMultiScaleMetadata buildMetadata(final NgffSingleScaleAxesMetadata meta, final String name, final String path, final DatasetAttributes[] dsetAttrs,
			final OmeNgffDataset[] datasets) {

		final int N = meta.getScale().length;

		// need to reverse the axes if the arrays are in C order
		final String type = "sampling";
		final String version = "0.4";

		return new OmeNgffMultiScaleMetadata(
			N, path, name, type, version, meta.getAxes(),
			datasets, dsetAttrs,
			null, null); // no global coordinate transforms of downsampling metadata
	}

}
