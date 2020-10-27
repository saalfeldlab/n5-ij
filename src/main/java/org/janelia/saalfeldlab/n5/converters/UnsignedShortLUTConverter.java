package org.janelia.saalfeldlab.n5.converters;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Simple sorted look-up-table {@link Converter} from an {@link AbstractIntegerType} to an {@link UnsignedShortType}.
 * When constructed from an {@link IterableInterval}, this will map the unique values found to [0,N-1] such that ordering is preserved.
 *
 * 
 *  
 * Performs no error checking. Calling convert with a value not in the look-up-table
 * will throw an error.
 * 
 * @author John Bogovic
 *
 * @param <T> input type
 */
public class UnsignedShortLUTConverter< T extends AbstractIntegerType< T > > implements Converter< T, UnsignedShortType >, BiConsumer< T, UnsignedShortType >
{

	private Map< T, Integer > lut;

	public UnsignedShortLUTConverter( Map< T, Integer > lut )
	{
		this.lut = lut;
	}

	public UnsignedShortLUTConverter( final IterableInterval< T > img )
	{
		TreeSet< T > values = uniqueValues( img );
		lut = new HashMap<>();

		int i = 0;
		for( T t : values )
			lut.put( t, i++ );
	}
	
	/**
	 * Returns the unique values in the {@link IterableInterval}, sorted as a {@link TreeSet}.
	 * 
	 * @param img the iterable
	 * @return 
	 */
	public static < T extends AbstractIntegerType< T > > TreeSet<T> uniqueValues( final IterableInterval<T> img )
	{
		TreeSet< T > uniqueValues = new TreeSet<>();
		Cursor< T > c = img.cursor();
		while( c.hasNext() )
			uniqueValues.add( c.next().copy() );

		return uniqueValues;
	}

	@Override
	public void accept( T t, UnsignedShortType out )
	{
		convert( t, out );
	}

	@Override
	public void convert( T t, UnsignedShortType output )
	{
		output.setInteger( lut.get( t ));
	}

}
