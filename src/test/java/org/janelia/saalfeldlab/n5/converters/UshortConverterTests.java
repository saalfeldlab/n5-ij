package org.janelia.saalfeldlab.n5.converters;

import org.janelia.saalfeldlab.n5.converters.UnsignedShortLinearConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class UshortConverterTests
{

	private final long maxUshort = UnsignedShortLinearConverter.MAXUSHORT;

	private ArrayImg< LongType, LongArray > img;

	private ArrayImg< LongType, LongArray > imgBig;

	@Before
	public void setUp()
	{
		img = ArrayImgs.longs( new long[]{ maxUshort, maxUshort + 1000, maxUshort + 2000 }, 3 );
		imgBig = ArrayImgs.longs( new long[]{ maxUshort, 2 * maxUshort, 3 * maxUshort }, 3 );
	}

	@Test
	public void testLUTConvert()
	{
		LongType in = new LongType();
		UnsignedShortType v = new UnsignedShortType();
		UnsignedShortLUTConverter< LongType > conv = new UnsignedShortLUTConverter<>( img );

		in.set( maxUshort );
		conv.accept( in, v );
		Assert.assertEquals( "lut, min to zero", 0, v.getInteger() );

		in.set( maxUshort + 1000 );
		conv.accept( in, v );
		Assert.assertEquals( "lut, mid to one", 1, v.getInteger() );

		in.set( maxUshort + 2000 );
		conv.accept( in, v );
		Assert.assertEquals( "lut, max to two", 2, v.getInteger() );

		UnsignedShortLUTConverter< LongType > convBig = new UnsignedShortLUTConverter<>( imgBig );

		in.set( maxUshort );
		convBig.accept( in, v );
		Assert.assertEquals( "big lut, min to zero", 0, v.getInteger() );

		in.set( 2 * maxUshort );
		convBig.accept( in, v );
		Assert.assertEquals( "big lut, mid to one", 1, v.getInteger() );

		in.set( 3 * maxUshort );
		convBig.accept( in, v );
		Assert.assertEquals( "big lut, max to two", 2, v.getInteger() );
	}

	@Test
	public void testLinearConvert()
	{
		UnsignedShortLinearConverter< LongType > conv = new UnsignedShortLinearConverter<>( img );
		
		LongType in = new LongType();
		UnsignedShortType v = new UnsignedShortType();
		in.set( maxUshort );
		conv.accept( in, v );
		Assert.assertEquals( "small range, min to zero", 0, v.getInteger() );

		in.set( maxUshort + 2000 );
		conv.accept( in, v );
		Assert.assertEquals( "small range, max to 2k", 2000, v.getInteger() );

		
		UnsignedShortLinearConverter< LongType > convBig = new UnsignedShortLinearConverter<>( imgBig );

		in.set( maxUshort );
		convBig.accept( in, v );
		Assert.assertEquals( "big range, min to zero", 0, v.getInteger() );

		in.set( 3 * maxUshort );
		convBig.accept( in, v );
		Assert.assertEquals( "big range, max to shortMax", maxUshort, v.getInteger() );

	}

}
