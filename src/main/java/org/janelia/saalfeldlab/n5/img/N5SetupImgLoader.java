package org.janelia.saalfeldlab.n5.img;


import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.bdv.N5ExportMetadata;
import org.janelia.saalfeldlab.n5.bdv.N5ExportMetadataReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.AbstractViewerSetupImgLoader;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * Assumes all channels use the same scales
 *
 */
public class N5SetupImgLoader<T extends NumericType<T> & NativeType<T>, V extends Volatile<T> & NativeType<V>> extends AbstractViewerSetupImgLoader<T, V> {

	private final N5Reader n5Reader;
	private final N5ExportMetadataReader metadata;
	private final int channelIndex;
	
	public N5SetupImgLoader( N5Reader n5Reader, T type, V volatileType, int channel ) {
		super(type, volatileType);
		this.n5Reader = n5Reader;
		metadata = N5ExportMetadata.openForReading( n5Reader );
		this.channelIndex = channel;
	}
	
	public N5SetupImgLoader( N5Reader n5Reader, T type, V volatileType) {
		this( n5Reader, type, volatileType, 0 );
	}

	@Override
	public RandomAccessibleInterval<V> getVolatileImage(int timepointId, int level, ImgLoaderHint... hints) {
		try {
			return N5Utils.openVolatile( n5Reader, N5ExportMetadata.getScaleLevelDatasetPath( channelIndex, level ), volatileType );
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public RandomAccessibleInterval<T> getImage(int timepointId, int level, ImgLoaderHint... hints) {
		try {
			return N5Utils.open( n5Reader, N5ExportMetadata.getScaleLevelDatasetPath( channelIndex, level ), type );
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public double[][] getMipmapResolutions() {
		try {
			return metadata.getScales(channelIndex);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public AffineTransform3D[] getMipmapTransforms() {
		//return metadata.getAffineTransform( channelIndex );
		return null;
	}

	@Override
	public int numMipmapLevels() {
		try {
			return metadata.getScales(channelIndex).length;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 1;
	}

}
