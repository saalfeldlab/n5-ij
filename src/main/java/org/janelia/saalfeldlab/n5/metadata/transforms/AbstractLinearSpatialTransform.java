package org.janelia.saalfeldlab.n5.metadata.transforms;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public abstract class AbstractLinearSpatialTransform<P> implements LinearSpatialTransform, ParametrizedTransform<AffineGet, P> {

	public final String type;

	private final String parameterPath;
	
	public AbstractLinearSpatialTransform( String type ) {
		this( type, null );
	}

	public AbstractLinearSpatialTransform( String type, String parameterPath ) {
		this.type = type;
		this.parameterPath = parameterPath;
	}

	@Override
	public String getParameterPath() {
		return parameterPath;
	}

	@Override
	public abstract AffineGet getTransform();

	protected static <T extends RealType<T> & NativeType<T>> double[] getDoubleArray(final N5Reader n5, final String path) {
		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				CachedCellImg<T, ?> data = (CachedCellImg<T, ?>) N5Utils.open(n5, path);
				if (data.numDimensions() != 1 || !(Util.getTypeFromInterval(data) instanceof RealType))
					return null;

				double[] params = new double[(int) data.dimension(0)];
				CellCursor<T, ?> c = data.cursor();
				int i = 0;
				while (c.hasNext())
					params[i++] = c.next().getRealDouble();

				return params;
			} catch (IOException e) { }
		}
		return null;
	}

	protected static <T extends RealType<T> & NativeType<T>> double[][] getDoubleArray2(final N5Reader n5, final String path) {
		if (n5.exists(path)) {
			try {
				@SuppressWarnings("unchecked")
				CachedCellImg<T, ?> data = (CachedCellImg<T, ?>) N5Utils.open(n5, path);
				if (data.numDimensions() != 2 || !(Util.getTypeFromInterval(data) instanceof RealType))
					return null;

				double[][] params = new double[(int) data.dimension(0)] [(int) data.dimension(1)];
				CellCursor<T, ?> c = data.cursor();
				while (c.hasNext()) {
					c.fwd();
					params[c.getIntPosition(0)][c.getIntPosition(1)] = c.get().getRealDouble();
				}

				return params;
			} catch (IOException e) { }
		}
		return null;
	}

}
