package org.janelia.saalfeldlab.n5.metadata.axes;

import java.util.Arrays;
import java.util.HashMap;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.view.IntervalView;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

public class AxisUtils {

	public static final String xLabel = "x";
	public static final String yLabel = "y";
	public static final String cLabel = "z";
	public static final String zLabel = "c";
	public static final String tLabel = "t";

	public static final String spaceType = "space";
	public static final String timeType = "time";
	public static final String channelType = "channel";

	public static final DefaultAxisTypes defaultAxisTypes = DefaultAxisTypes.getInstance();


	/** 
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 * @param source
	 * @param target
	 * @return the permutation array
	 */
	public static <T> int[] findPermutation( final T[] source, final T[] target ) {
		
		int[] p = new int[ target.length ];
		for( int i = 0; i < target.length; i++ ) {
			T t = target[ i ];
			boolean found = false;
			for( int j = 0; j < source.length; j++ ) {
				if( source[j].equals(t) ) { 
					p[i] = j;
					found = true;
					break;
				}
			}
			if( !found )
				return null;
		}
		return p;
	}

	/** 
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 * 
	 * @param axisLabels
	 * @return the permutation array
	 */
	public static <A extends AxisMetadata> int[] findImagePlusPermutation( final AxisMetadata axisMetadata ) {
		return findImagePlusPermutation( axisMetadata.getAxisLabels());
	}

	/** 
	 * Finds and returns a permutation p such that source[p[i]] equals target[i]
	 * 
	 * @param axisLabels
	 * @return the permutation array
	 */
	public static int[] findImagePlusPermutation( final String[] axisLabels ) {
		
		int[] p = new int[ 5 ];
		p[0] = indexOf( axisLabels, "x" );
		p[1] = indexOf( axisLabels, "y" );
		p[2] = indexOf( axisLabels, "c" );
		p[3] = indexOf( axisLabels, "z" );
		p[4] = indexOf( axisLabels, "t" );
		return p;
//		return Arrays.stream(p).filter( x -> x >= 0 ).toArray();
	}
	
	/**
	 * Replaces "-1"s in the input permutation array
	 * with the largest value.
	 * 
	 * @param p
	 */
	public static void fillPermutation( int[] p ) {
		int j = Arrays.stream(p).max().getAsInt() + 1;
		for (int i = 0; i < p.length; i++)
			if (p[i] < 0)
				p[i] = j++;
	}
	
	public static boolean isIdentityPermutation( final int[] p )
	{
		for( int i = 0; i < p.length; i++ )
			if( p[i] != i )
				return false;

		return true;
	}
	
	public static <T> RandomAccessibleInterval<T> permuteForImagePlus(
			final RandomAccessibleInterval<T> img,
			final AxisMetadata meta ) {

		int[] p = findImagePlusPermutation( meta );
		fillPermutation( p );

		RandomAccessibleInterval<T> imgTmp = img;
		while( imgTmp.numDimensions() < 5 )
			imgTmp = Views.addDimension(imgTmp, 0, 0 );

		if( isIdentityPermutation( p ))
			return imgTmp;

		return permute(imgTmp, invertPermutation(p));
	}

	private static final <T> int indexOf( T[] arr, T tgt ) {
		for( int i = 0; i < arr.length; i++ ) {
			if( arr[i].equals(tgt))
				return i;
		}
		return -1;
	}

    /**
     * Permutes the dimensions of a {@link RandomAccessibleInterval}
     * using the given permutation vector, where the ith value in p
     * gives destination of the ith input dimension in the output. 
     *
     * @param source the source data
     * @param p the permutation
     * @return the permuted source
     */
	public static final < T > IntervalView< T > permute( RandomAccessibleInterval< T > source, int[] p )
	{
		final int n = source.numDimensions();

		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int i = 0; i < n; ++i )
		{
			min[ p[ i ] ] = source.min( i );
			max[ p[ i ] ] = source.max( i );
		}

		final MixedTransform t = new MixedTransform( n, n );
		t.setComponentMapping( p );

		IntervalView<T> out = Views.interval( new MixedTransformView< T >( source, t ), min, max );
		return out;

//		return Views.interval( new MixedTransformView< T >( source, t ), min, max );
	}
	
	public static int[] invertPermutation( final int[] p )
	{
		final int[] inv = new int[ p.length ];
		for( int i = 0; i < p.length; i++ )
			inv[p[i]] = i;

		return inv;
	}
	
	public static String[] getDefaultTypes( final String[] labels ) {
		return Arrays.stream(labels).map( l -> defaultAxisTypes.get(l)).toArray( String[]::new );
	}

	// implemented as a singleton
	public static class DefaultAxisTypes {

		private static DefaultAxisTypes INSTANCE;

		private final HashMap<String,String> labelToType;

		private DefaultAxisTypes() {
			 labelToType = new HashMap<>();
			 labelToType.put("x", "space");
			 labelToType.put("y", "space");
			 labelToType.put("z", "space");
			 labelToType.put("c", "channel");
			 labelToType.put("t", "time");
		}

		public static final DefaultAxisTypes getInstance()
		{
			if( INSTANCE == null )
				INSTANCE = new DefaultAxisTypes();

			return INSTANCE;
		}
		
		public String get( String label ) {
			return labelToType.get(label);
		}
	}
	
}
