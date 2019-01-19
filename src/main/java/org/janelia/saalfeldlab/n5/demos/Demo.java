package org.janelia.saalfeldlab.n5.demos;

import java.io.IOException;
import java.util.Random;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import bdv.util.BdvFunctions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;

public class Demo {

	public static void main(String[] args) {

		/**
		 * Saving and loading individual blocks from an n5 dataset
		 */
		String n5Base = "/Users/bogovicj/demo.n5";
		String n5dataset = "raw";
		ByteType type = new ByteType();
		
		generateNoisyN5( n5Base, n5dataset );
		
		writeSingleBlock( n5Base, n5dataset );
		
		getMetaDataAndReadSingleBlock( n5Base, n5dataset, type );
	
		readAndShow( n5Base, n5dataset );
	}
	
	public static void readAndShow( String n5Base, String n5dataset ) {

		// Read the n5 and look at it
		N5Reader n5reader;
		RandomAccessibleInterval<ByteType> img = null;
		try {

			n5reader = new N5FSReader( n5Base );
			img = N5Utils.open( n5reader, n5dataset, new ByteType() );

		} catch (IOException e) {
			e.printStackTrace();
		}
	
		if( img != null )
			BdvFunctions.show( img, "n5 image" );
	}

	public static <T extends NativeType<T> & RealType<T>> void getMetaDataAndReadSingleBlock( String n5Base, String n5dataset, T defaultType ) {

		N5Reader n5reader;
		RandomAccessibleInterval<ByteType> img = null;
		try {

			n5reader = new N5FSReader( n5Base );
			RandomAccessibleInterval<T> block = getIthBlock( n5reader, n5dataset, defaultType, 5 );
			BdvFunctions.show( block, "block 5" );

		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public static <T extends NativeType<T> & RealType<T>> RandomAccessibleInterval<T> getIthBlock( N5Reader n5reader, String n5dataset, 
			T defaultType, int i ) throws IOException
	{
		DatasetAttributes attributes = n5reader.getDatasetAttributes( n5dataset );
		int[] blockSize = attributes.getBlockSize();
		long[] dimensions = attributes.getDimensions();

		final CellGrid outputBlockGrid = new CellGrid( dimensions, blockSize );
		final long[] outputBlockGridPosition = new long[ outputBlockGrid.numDimensions() ];
		outputBlockGrid.getCellGridPositionFlat( i, outputBlockGridPosition );

		// generate interval
		final long[] outputBlockMin = new long[ outputBlockGrid.numDimensions() ];
		final long[] outputBlockMax = new long[ outputBlockGrid.numDimensions() ];
		final int[] outputBlockDimensions = new int[ outputBlockGrid.numDimensions() ];

		outputBlockGrid.getCellDimensions( outputBlockGridPosition, outputBlockMin, outputBlockDimensions );
		for ( int d = 0; d < outputBlockGrid.numDimensions(); ++d )
			outputBlockMax[ d ] = outputBlockMin[ d ] + outputBlockDimensions[ d ] - 1;

		final Interval blockInterval = new FinalInterval( outputBlockMin, outputBlockMax );

		return Views.interval( N5Utils.open( n5reader, n5dataset, defaultType ),
				blockInterval );
	}
	
	public static void writeSingleBlock( String n5Base, String n5dataset ) {
		
		// make a block of 255s
		ArrayImg<ByteType, ByteArray> block = ArrayImgs.bytes( 32, 32, 32 );
		LoopBuilder.setImages( block ).forEachPixel( x -> x.set((byte)255) );


		// save the block of ones in the grid position (1,1,1)
		// -> the physical position starting at index  (32,32,32)
		try {

			N5Writer n5 = new N5FSWriter( n5Base );
			N5Utils.saveBlock( block, n5, n5dataset, new long[]{1, 1, 1});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void generateNoisyN5( final String n5Base, final String n5dataset ) {

		int[] blockSize = new int[]{ 32, 32, 32 };
		GzipCompression compression = new GzipCompression();

		// make an array full of one's and write it
		Random rand = new Random();
		byte[] blist = new byte[ 1 ];

		ArrayImg<ByteType, ByteArray> wholeImage = ArrayImgs.bytes( 256, 256, 256 );
		LoopBuilder.setImages(wholeImage).forEachPixel( x -> {
			rand.nextBytes(blist);
			x.setByte(blist[0]);
		});

		// make a block of 255s
		ArrayImg<ByteType, ByteArray> block = ArrayImgs.bytes( 32, 32, 32 );
		LoopBuilder.setImages( block ).forEachPixel( x -> x.set((byte)255) );

		try {

			N5Writer n5 = new N5FSWriter( n5Base );
			N5Utils.save( wholeImage, n5, n5dataset, blockSize, compression);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
