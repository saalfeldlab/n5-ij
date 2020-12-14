package org.janelia.saalfeldlab.n5.converters;

import java.util.TreeSet;
import java.util.function.BiConsumer;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class UnsignedShortLinearConverter< T extends AbstractIntegerType< T > > implements Converter< T, UnsignedShortType >, BiConsumer< T, UnsignedShortType >
{
	public final static long MAXUSHORT = 65535;

	private final double m;

	private final long b;

	private boolean isIdentity = false;

	public UnsignedShortLinearConverter()
	{
		m = 1;
		b = 0;
		isIdentity = true;
	}

	public UnsignedShortLinearConverter( final IterableInterval<T> img )
	{
		TreeSet< Long > values = uniqueValues( img );
		long min = values.first();
		long max = values.last();

		if( min > 0 && max <= MAXUSHORT )
		{
			isIdentity = true;
			m = 1;
			b = 0;
		}
		else
		{
			b = -min;
			long diff = ( max - min );
			if( diff <= MAXUSHORT )
				m = 1;
			else
				m = (double)MAXUSHORT / diff;
		}
	}
	
	/**
	 * Returns the unique values in the {@link IterableInterval}, sorted as a {@link TreeSet}.
	 * 
	 * @param <T> the image data type
	 * @param img the iterable
	 * @return the unique values
	 */
	public static < T extends AbstractIntegerType< T > > TreeSet<Long> uniqueValues( final IterableInterval<T> img )
	{
		TreeSet< Long > uniqueValues = new TreeSet<>();
		Cursor< T > c = img.cursor();
		while( c.hasNext() )
			uniqueValues.add( c.next().getIntegerLong() );

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
		if( isIdentity )
			output.setInteger( t.getInteger() );
		else
			output.setInteger( Math.round( m * ( b + t.getIntegerLong() ))); 
	}


}
