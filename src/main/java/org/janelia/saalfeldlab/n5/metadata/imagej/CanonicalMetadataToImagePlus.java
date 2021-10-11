package org.janelia.saalfeldlab.n5.metadata.imagej;

import ij.ImagePlus;
import ij.measure.Calibration;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;

import java.io.IOException;
import java.util.Arrays;

public class CanonicalMetadataToImagePlus implements ImageplusMetadata<CanonicalDatasetMetadata> {

	private final boolean setDims;

	public CanonicalMetadataToImagePlus( boolean setDims ) {
		this.setDims = setDims;
	}

	public CanonicalMetadataToImagePlus() {
		this(false);
	}

	@Override
	public void writeMetadata(final CanonicalDatasetMetadata t, final ImagePlus ip) throws IOException {
		if( t instanceof CanonicalSpatialDatasetMetadata )
			writeSpatialMetadata( (CanonicalSpatialDatasetMetadata) t, ip );

		ip.setDisplayRange( t.minIntensity(), t.maxIntensity());
	}

	public void writeSpatialMetadata(final CanonicalSpatialDatasetMetadata t, final ImagePlus ip) throws IOException {

		final int nd = t.getAttributes().getNumDimensions();
		final AffineGet xfm = t.getSpatialTransform().spatialTransform();
		final String unit = t.getSpatialTransform().unit();

		ip.setTitle(t.getPath());
		final Calibration cal = ip.getCalibration();
		cal.pixelWidth = xfm.get(0, 0);
		cal.xOrigin = xfm.get(0, nd);
		cal.pixelHeight = xfm.get(1, 1);
		cal.yOrigin = xfm.get(1, nd);

		cal.setUnit(unit);

		final Axis[] axes = t.getSpatialTransform().getAxes();
		final long[] dims = t.getAttributes().getDimensions();

		// TODO setDimensions is not needed because 
		// a permutation is applied elsewhere
		if( axes != null ) {
			ip.getCalibration().setXUnit( axes[0].getUnit() );
			ip.getCalibration().setYUnit( axes[1].getUnit() );

			int i = 2;
			int nz = 0;
			int nc = 0;
			int nt = 0;
			while( i < axes.length ) {
				// anything that is not space or time goes into channels
				// could be wrong if multiple dimensions are neither space nor time
				// if so, flatten all those dimensions into channels
				// (I think adding to dimensions accomplishes this - JB) 
				// TODO reconsider if these defaults are what we want
				if( axes[i].getType().equals("space")) {
					nz  += (int)dims[i];
					cal.pixelDepth = xfm.get(i, i);
					cal.zOrigin = xfm.get(i, i+1);
				}
				else if( axes[i].getType().equals("time")) {
					nt  += (int)dims[i];
					cal.frameInterval = xfm.get(i, i);
				}
				else
					nc  += (int)dims[i];

				i++;
			}
			nc = nc == 0 ? 1 : nc;
			nz = nz == 0 ? 1 : nz;
			nt = nt == 0 ? 1 : nt;

			if( setDims )
				ip.setDimensions(nc, nz, nt);
		}
		else if( setDims ){
			// if axes are not specified, assume xyz for 3d 
			// and assume xyzc for 4d
			if (nd == 3) {
				ip.setDimensions(1, (int) dims[2], 1);
				cal.pixelDepth = xfm.get(2, 2);
				cal.zOrigin = xfm.get(2, nd);
			}
			else if (nd == 4) {
				ip.setDimensions((int) dims[3], (int) dims[2], 1);
				cal.pixelDepth = xfm.get(3, 3);
				cal.zOrigin = xfm.get(3, nd);
			}
		}

	}

	public Axis[] axesFromImageplus(final ImagePlus imp) {

		int nd = imp.getNDimensions();
		Axis[] axes = new Axis[ nd ];

		axes[ 0 ] = new Axis( "X", "space", imp.getCalibration().getXUnit());
		axes[ 1 ] = new Axis( "Y", "space", imp.getCalibration().getYUnit());

		int i = 2;
		if( imp.getNChannels() > 1 )
			axes[ i++ ] = new Axis( "channels", "C", "null" );

		if( imp.getNSlices() > 1 )
			axes[ i++ ] = new Axis( "space", "Z", imp.getCalibration().getZUnit());

		if( imp.getNFrames() > 1 )
			axes[ i++ ] = new Axis( "time", "T", imp.getCalibration().getTimeUnit() );

		return axes;
	}

	public AffineTransform getAffine( final ImagePlus imp ) {
		int nd = imp.getNDimensions();
		AffineTransform affine = new AffineTransform( nd );

		affine.set( imp.getCalibration().pixelWidth, 0, 0);
		affine.set( imp.getCalibration().xOrigin, 0, nd);

		affine.set( imp.getCalibration().pixelHeight, 1, 1);
		affine.set( imp.getCalibration().yOrigin, 1, nd);

		int i = 2;
		if( imp.getNChannels() > 1 ) {
			// channels dont have spacing information
			i++;
		}

		if( imp.getNSlices() > 1 ) {
			affine.set( imp.getCalibration().pixelDepth, i, i);
			affine.set( imp.getCalibration().zOrigin, i, nd);
			i++;
		}

		if( imp.getNFrames() > 1 ) {
			affine.set( imp.getCalibration().frameInterval, i, i);
			i++;
		}

		return affine;
	}

	@Override
	public CanonicalSpatialDatasetMetadata readMetadata(final ImagePlus imp) throws IOException {
		int nd = imp.getNDimensions();

		double[] params = getAffine( imp ).getRowPackedCopy();
//		double[] params;
//		if (nd == 2)
//			params = new double[] { 
//					imp.getCalibration().pixelWidth, 0, imp.getCalibration().xOrigin, 
//					0, imp.getCalibration().pixelHeight, imp.getCalibration().yOrigin };
//		else if (nd == 3)
//			params = new double[]{
//					imp.getCalibration().pixelWidth, 0, 0, imp.getCalibration().xOrigin, 
//					0, imp.getCalibration().pixelHeight, 0, imp.getCalibration().yOrigin, 
//					0, 0, imp.getCalibration().pixelDepth, imp.getCalibration().zOrigin };
//		else
//			return null;

		// TODO what to about attrs?
		final int[] impDims = Arrays.stream(imp.getDimensions()).filter( x -> x > 1 ).toArray();
		final long[] dims = Arrays.stream(impDims).mapToLong( x -> x ).toArray();
		final DatasetAttributes attributes = new DatasetAttributes( dims, impDims, DataType.FLOAT32, new GzipCompression());

		final SpatialMetadataCanonical spatialMeta = new SpatialMetadataCanonical("",
				new AffineSpatialTransform(params), imp.getCalibration().getUnit(),
				axesFromImageplus(imp));

		return new CanonicalSpatialDatasetMetadata("", spatialMeta, attributes);
	}

}
