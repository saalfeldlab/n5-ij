/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5;

import java.util.HashMap;
import java.util.List;

import org.janelia.saalfeldlab.n5.ij.N5Exporter;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class RunImportExportTest
{
	final ImagePlus imp;

	final String outputPath;

	final String dataset;

	private String metadataType;

	private String compressionType;

	private String blockSizeString;

	/*
	 * "Outputs"
	 */
	private boolean singletonListOut;

	private boolean resEqual;

	private boolean unitsEqual;

	private boolean imagesEqual;

	public RunImportExportTest(
			final ImagePlus imp,
			final String outputPath,
			final String dataset,
			final String metadataType,
			final String compressionType,
			final String blockSizeString )
	{
		this.imp = imp;
		this.outputPath = outputPath;
		this.dataset = dataset;

		this.metadataType = metadataType;
		this.compressionType = compressionType;
		this.blockSizeString = blockSizeString;
	}

	public void run()
	{
		final N5Exporter writer = new N5Exporter();
		writer.setOptions( imp, outputPath, dataset,
				blockSizeString, metadataType, compressionType );
		writer.run();

		final String n5PathAndDataset = outputPath +  dataset;
		final N5Importer reader = new N5Importer();
		final List< ImagePlus > impList = reader.process( n5PathAndDataset, false );

		singletonListOut = impList.size() == 1;

		final ImagePlus impRead = impList.get( 0 );

		resEqual = impRead.getCalibration().pixelWidth == imp.getCalibration().pixelWidth &&
			impRead.getCalibration().pixelHeight == imp.getCalibration().pixelHeight &&
			impRead.getCalibration().pixelDepth == imp.getCalibration().pixelDepth;

		unitsEqual = impRead.getCalibration().getUnit().equals( imp.getCalibration().getUnit() );

		imagesEqual = equal( imp, impRead );

		impRead.close();
	}

	public static void main( final String[] args )
	{
		final ImagePlus imp = IJ.openImage( "/home/john/tmp/blobs.tif" );

		final String[] metadataTypes = new String[]{
				N5Importer.MetadataImageJKey,
				N5Importer.MetadataN5CosemKey,
				N5Importer.MetadataN5ViewerKey
		};

		//String[] containerTypes = new String[] { "FILESYSTEM", "ZARR" };
		final String[] containerTypes = new String[] { "HDF5" };

		final HashMap<String,String> typeToExtension = new HashMap<>();
		typeToExtension.put( "FILESYSTEM", "n5" );
		typeToExtension.put( "ZARR", "zarr" );
		typeToExtension.put( "HDF5", "h5" );

		for( final String containerType : containerTypes )
		{
			for( final String metatype : metadataTypes )
			{
				final String n5RootPath = "/home/john/tmp/test." + typeToExtension.get( containerType );
				final RunImportExportTest testRunner = new RunImportExportTest(
						imp, n5RootPath, "/blobs",
						metatype, "gzip", "32,32" );

				testRunner.run();
				System.out.println( "metadata type: " + metatype );
				final boolean allPassed = testRunner.singletonListOut &&
						testRunner.resEqual &&
						testRunner.unitsEqual  &&
						testRunner.imagesEqual ;
				System.out.println( " " + allPassed );
				if( ! allPassed )
				{
					System.out.println( "   " +
						testRunner.singletonListOut + " " +
						testRunner.resEqual + " " +
						testRunner.unitsEqual  + " " +
						testRunner.imagesEqual );
				}
				System.out.println( " " );
			}
		}
	}

	public static < T extends RealType< T > & NativeType< T > > boolean equal( final ImagePlus a, final ImagePlus b )
	{
		try {
			final Img<T> imgA = ImageJFunctions.wrapRealNative( a );
			final Img<T> imgB = ImageJFunctions.wrapRealNative( a );

			final Cursor< T > c = imgA.cursor();
			final RandomAccess< T > r = imgB.randomAccess();

			while( c.hasNext() )
			{
				c.fwd();
				r.setPosition( c );
				if( c.get().getRealDouble() != r.get().getRealDouble() )
					return false;

			}

			return true;

		}catch( final Exception e )
		{
			return false;
		}
	}

}
