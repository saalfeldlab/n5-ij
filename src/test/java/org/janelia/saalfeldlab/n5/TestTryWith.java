package org.janelia.saalfeldlab.n5;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import ij.ImagePlus;
import ij.gui.NewImage;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class TestTryWith
{

	/*
	 * Run to observe errors.
	 * Comment in the try( n5 ) blocks and observe this runs error-free.
	 */
	public static <T extends RealType<T> & NativeType<T>> void main( String[] args )
	{
		URL configUrl = RunImportExportTest.class.getResource( "/plugins.config" );
		File baseDir = new File( configUrl.getFile()).getParentFile();
		String path = baseDir.getAbsolutePath() + File.separator + "breaking.h5";
	
		final ImagePlus imp = NewImage.createImage( "test", 32, 32, 32, 8, NewImage.FILL_NOISE );
		RandomAccessibleInterval<T> img = ImageJFunctions.wrap( imp );

		N5HDF5Writer n5w;
		try
		{
			n5w = new N5HDF5Writer( path, 8, 8, 8 );
//			try( n5w )
//			{
				N5Utils.save( img, n5w, "/first", new int[] {8,8,8}, new GzipCompression());
				System.out.println( "writing 1 done");
//			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		
		N5HDF5Reader n5r;
		try
		{
			n5r = new N5HDF5Reader( path, 8, 8, 8 );
//			try( n5r )
//			{
				Img<?> imgr1 = N5Utils.open( n5r, "/first" );
				System.out.println( "read: " + imgr1 );
//			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		N5HDF5Writer n5w2;
		try
		{
			n5w2 = new N5HDF5Writer( path, 8, 8, 8 );
//			try (n5w2)
//			{
				N5Utils.save( img, n5w2, "/second", new int[] { 8, 8, 8 }, new GzipCompression() );
				System.out.println( "writing 2 done");
//			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

	}
	 

}
