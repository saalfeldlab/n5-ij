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

public class Ngff5dToImagePlus extends SpatialMetadataToImagePlus<NgffSingleScaleAxesMetadata> {
	
	// 01234
	// XYZCT
	static final int X = 0;
	static final int Y = 1;
	static final int Z = 2;
	static final int C = 3;
	static final int T = 4;

	private boolean force5d = false;

	public void setForce5d(final boolean force5d) {

		this.force5d = force5d;
	}

	@Override
	public void writeMetadata(final NgffSingleScaleAxesMetadata t, final ImagePlus ip) throws IOException {

		ip.setTitle(t.getPath());

		final Axis[] axes = t.getAxes();
		final double[] scale = t.getScale();
		final double[] translation = t.getTranslation();

		for (int i = 0; i < axes.length; i++) {
			switch (axes[i].getName()) {
			case "x":
				final String spaceUnit = axes[i].getUnit();
				if (spaceUnit != null && !spaceUnit.isEmpty())
					ip.getCalibration().setUnit(spaceUnit);
				ip.getCalibration().pixelWidth = scale[i];
				ip.getCalibration().xOrigin = translation[i];
				break;
			case "y":
				ip.getCalibration().pixelHeight = scale[i];
				ip.getCalibration().yOrigin = translation[i];
				break;
			case "z":
				ip.getCalibration().pixelDepth = scale[i];
				ip.getCalibration().zOrigin = translation[i];
				break;
			case "t":
				ip.getCalibration().frameInterval = scale[i];
				final String timeUnit = axes[i].getUnit();
				if (timeUnit != null && !timeUnit.isEmpty())
					ip.getCalibration().setTimeUnit(timeUnit);
				break;
			}
		}
	}

	@Override
	public NgffSingleScaleAxesMetadata readMetadata(final ImagePlus ip) throws IOException {

		return force5d ? readMetadata5d(ip) : readMetadataReducedDims(ip);
	}

	// Always produces 5D metadata with all axes present, regardless of singleton dims.
	public NgffSingleScaleAxesMetadata readMetadata5d(final ImagePlus ip) throws IOException {

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

		axes[Z] = new Axis(Axis.SPACE, "z", spaceUnit);
		scale[Z] = ip.getCalibration().pixelDepth;
		offset[Z] = ip.getCalibration().zOrigin;

		axes[C] = new Axis(Axis.CHANNEL, "c", null);
		scale[C] = 1;
		offset[C] = 0;

		final String timeUnit = parseUnitWithWarning(ip.getCalibration().getTimeUnit());
		axes[T] = new Axis(Axis.TIME, "t", timeUnit);
		scale[T] = ip.getCalibration().frameInterval;
		if (scale[T] == 0.0)
			scale[T] = 1.0;
		offset[T] = 0;

		final boolean noOffset = Arrays.stream(offset).allMatch(x -> x == 0.0);
		if (noOffset)
			return new NgffSingleScaleAxesMetadata("", scale, null, axes, ImageplusMetadata.datasetAttributes(ip));
		else
			return new NgffSingleScaleAxesMetadata("", scale, offset, axes, ImageplusMetadata.datasetAttributes(ip));
	}

	// Produces metadata whose axes and scale only include non-singleton C/Z/T dims.
	public NgffSingleScaleAxesMetadata readMetadataReducedDims(final ImagePlus ip) throws IOException {

		final String spaceUnit = parseUnitWithWarning(ip.getCalibration().getUnit());
		final String timeUnit = parseUnitWithWarning(ip.getCalibration().getTimeUnit());
		final boolean hasC = ip.getNChannels() > 1;
		final boolean hasZ = ip.getNSlices() > 1;
		final boolean hasT = ip.getNFrames() > 1;

		final int N = 2 + (hasC ? 1 : 0) + (hasZ ? 1 : 0) + (hasT ? 1 : 0);
		final Axis[] axes = new Axis[N];
		final double[] scale = new double[N];
		final double[] offset = new double[N];

		// We write OME-Zarr metadata in XYZCT order
		// as recommended by the specification (v0.4 and v0.5)
		axes[0] = new Axis(Axis.SPACE, "x", spaceUnit);
		scale[0] = ip.getCalibration().pixelWidth;
		offset[0] = ip.getCalibration().xOrigin;

		axes[1] = new Axis(Axis.SPACE, "y", spaceUnit);
		scale[1] = ip.getCalibration().pixelHeight;
		offset[1] = ip.getCalibration().yOrigin;

		int d = 2;
		if (hasZ) {
			axes[d] = new Axis(Axis.SPACE, "z", spaceUnit);
			scale[d] = ip.getCalibration().pixelDepth;
			offset[d] = ip.getCalibration().zOrigin;
			d++;
		}
		if (hasC) {
			axes[d] = new Axis(Axis.CHANNEL, "c", null);
			scale[d] = 1;
			offset[d] = 0;
			d++;
		}
		if (hasT) {
			axes[d] = new Axis(Axis.TIME, "t", timeUnit);
			scale[d] = ip.getCalibration().frameInterval;
			if (scale[d] == 0.0)
				scale[d] = 1.0;
			offset[d] = 0;
		}

		final boolean noOffset = Arrays.stream(offset).allMatch(x -> x == 0.0);
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

		final String name = image.getTitle();
		final String type = "";
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
			datasets, null, dsetAttrs,
			null); // no global coordinate transforms of downsampling metadata
	}

}
