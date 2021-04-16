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
package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.converters.UnsignedShortLUTConverter;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessFactory;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMultiScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5GroupParser;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleLegacyMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultiscaleMetadataParser;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetTreeCellRenderer;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class N5Importer implements PlugIn
{
//	private static final String[] axisNames = new String[] { "x", "y", "c", "z", "t" };
	private static final String[] axisNames = new String[] { "dim1", "dim2", "dim3", "dim4", "dim5" };

	public static final String n5PathKey = "n5";
	public static final String COMMAND_NAME = "N5";

	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	public static final String MetadataAutoKey = "Auto-detect";
	public static final String MetadataImageJKey = "ImageJ";
	public static final String MetadataN5CosemKey = "Cosem";
	public static final String MetadataN5ViewerKey = "N5Viewer";
	public static final String MetadataCustomKey = "Custom";
	public static final String MetadataDefaultKey = "Default";

	public static final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
					new N5ImagePlusMetadata( "" ),
					new N5CosemMetadata( "", null, null ),
					new N5SingleScaleMetadata(),
					new N5SingleScaleLegacyMetadata(),
					new DefaultMetadata( "", -1 )
				};
	
	public static final N5GroupParser<?>[] GROUP_PARSERS = new N5GroupParser[]{
			new N5CosemMultiScaleMetadata(),
			new N5ViewerMultiscaleMetadataParser()
	};

    private N5Reader n5;

	private DatasetSelectorDialog selectionDialog;

	private DataSelection selection;

	private Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes;

	private ImageplusMetadata< ? > impMeta;

	private Interval cropInterval;

	private boolean asVirtual;

	private boolean show = true;

	private boolean cropOption;

	private Thread loaderThread;

	private boolean record;

	private int numDimensionsForCrop;

	private long[] initMaxValuesForCrop;

	private static String lastOpenedContainer = "";

	public N5Importer()
	{
		// store value of record
		// necessary to skip initial opening of this dialog
		record = Recorder.record;
		Recorder.record = false;

		// default image plus metadata parsers
		impMetaWriterTypes = new HashMap< Class<?>, ImageplusMetadata< ? > >();
		impMetaWriterTypes.put( N5ImagePlusMetadata.class, new N5ImagePlusMetadata( "" ) );
		impMetaWriterTypes.put( N5CosemMetadata.class, new N5CosemMetadata( "", null, null ) );
		impMetaWriterTypes.put( N5SingleScaleMetadata.class, new N5SingleScaleMetadata());
		impMetaWriterTypes.put( N5SingleScaleLegacyMetadata.class, new N5SingleScaleLegacyMetadata());
		impMetaWriterTypes.put( DefaultMetadata.class, new DefaultMetadata( "", 1 ) );

		numDimensionsForCrop = 5;
		initMaxValuesForCrop = new long[ numDimensionsForCrop ];
		Arrays.fill( initMaxValuesForCrop, Long.MAX_VALUE );
	}

	public N5Reader getN5()
	{
		return n5;
	}

	public Map< Class< ? >, ImageplusMetadata< ? > > getImagePlusMetadataWriterMap()
	{
		return impMetaWriterTypes;
	}

	public void setNumDimensionsForCropDialog( final int numDimensionsForCrop )
	{
		this.numDimensionsForCrop = numDimensionsForCrop;
	}

	/**
	 * Set a flag determining whether the process method
	 * calls show on the resulting ImagePlus.
	 * 
	 * @param show the flag
	 */
	public void setShow( final boolean show )
	{
		this.show = show;
	}

	@Override
    public void run( final String args )
	{
		final String options = Macro.getOptions();
		final boolean isMacro = ( options != null && !options.isEmpty() );
		final boolean isCrop = ( args != null && !args.isEmpty() );

		if ( !isMacro && !isCrop )
		{
			// the fancy selector dialog
			selectionDialog = new DatasetSelectorDialog(
					new N5ViewerReaderFun(),
					new N5BasePathFun(),
					lastOpenedContainer,
					null, // no group parsers
					PARSERS );

			selectionDialog.setTreeRenderer( new N5DatasetTreeCellRenderer( true ) );

			selectionDialog.setContainerPathUpdateCallback( x -> {
				if( x != null )
					lastOpenedContainer = x;
			});

			selectionDialog.setCancelCallback( x -> {
				// set back recorder state if canceled
				Recorder.record = record;
			});

			selectionDialog.setVirtualOption( true );
			selectionDialog.setCropOption( true );
			selectionDialog.run( this::datasetSelectorCallBack );
		}
		else
		{
			// disable recorder
			record = Recorder.record;
			Recorder.record = false;

			String n5Path = Macro.getValue( args, n5PathKey, "" );
			final boolean dialogAsVirtual = args.contains( " virtual" );

			final GenericDialog gd = new GenericDialog( "Import N5" );
			gd.addStringField( "N5 path", n5Path );
			gd.addCheckbox( "Virtual", dialogAsVirtual );

			gd.addMessage( " ");
			gd.addMessage( "Crop parameters.");
			gd.addMessage( "[0,Infinity] loads the whole volume.");
			gd.addMessage( "Min:");
			for( int i = 0; i < numDimensionsForCrop; i++ )
				gd.addNumericField( "min_"+axisNames[ i ], 0 );

			gd.addMessage( "Max:");
			for( int i = 0; i < numDimensionsForCrop; i++ )
			{
				if( initMaxValuesForCrop != null )
					gd.addNumericField( "max_"+axisNames[ i ], initMaxValuesForCrop[ i ]);
				else
					gd.addNumericField( "max_"+axisNames[ i ], Double.POSITIVE_INFINITY );
			}

			gd.showDialog();
			if ( gd.wasCanceled() )
			{
				// set back recorder state if canceled
				Recorder.record = record;
				return;
			}

			n5Path = gd.getNextString();
			final boolean openAsVirtual = gd.getNextBoolean();

			// we don't always know ahead of time the dimensionality
			final long[] cropMin = new long[ numDimensionsForCrop ];
			final long[] cropMax = new long[ numDimensionsForCrop ];

			for( int i = 0; i < numDimensionsForCrop; i++ )
				cropMin[ i ] = Math.max( 0, (long)Math.floor( gd.getNextNumber()));

			for( int i = 0; i < numDimensionsForCrop; i++ )
			{
				final double v = gd.getNextNumber();
				cropMax[ i ] = Double.isInfinite( v ) ? Long.MAX_VALUE : (long)Math.ceil( v );
			}

			final Interval thisDatasetCropInterval = new FinalInterval( cropMin, cropMax );

			// set recorder back
			Recorder.record = record;

			final N5Reader n5ForThisDataset  = new N5ViewerReaderFun().apply( n5Path );
			N5Metadata meta;
			try
			{
				meta = new N5DatasetDiscoverer( null, PARSERS ).parse( n5ForThisDataset, "" ).getMetadata();

				process( n5ForThisDataset, n5Path, Collections.singletonList( meta ), openAsVirtual, thisDatasetCropInterval,
						impMetaWriterTypes );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public static boolean isTypeOpenable( final N5Metadata meta, final boolean showMessage )
	{
		final DataType type = meta.getAttributes().getDataType();
		if(	type != DataType.FLOAT32 &&
			type != DataType.UINT8 &&
			type != DataType.UINT16 &&
			type != DataType.UINT32 )
		{
			if( showMessage )
			{
				IJ.error( "Cannot open datasets of type (" + type + ").\n"
						+ "ImageJ supports uint8, uint16, uint32(rgb), or float32.");
			}
			return false;
		}
		return true;
	}

	private void datasetSelectorCallBack( final DataSelection selection )
	{
		// set the recorder back to its original value
		Recorder.record = record;

		this.selection = selection;
		this.n5 = selection.n5;
		this.asVirtual = selectionDialog.isVirtual();
		this.cropOption = selectionDialog.getCropOption();

		if( cropOption )
			processWithCrops();
		else
			processThread();
	}

	public static String generateAndStoreOptions( final String n5RootAndDataset, final boolean virtual, final Interval cropInterval )
	{
		Recorder.resetCommandOptions();
		Recorder.recordOption( "n5", n5RootAndDataset );

		if( virtual )
			Recorder.recordOption( "virtual" );

		if( cropInterval != null )
		{
			for( int i = 0; i < cropInterval.numDimensions(); i++ )
			{
				Recorder.recordOption( axisNames[i],  Long.toString( cropInterval.min( i )));
				Recorder.recordOption( axisNames[i] + "_0",  Long.toString( cropInterval.max( i )));
			}
		}
		return Recorder.getCommandOptions();
	}

	public static void record( final String n5RootAndDataset, final boolean virtual, final Interval cropInterval )
	{
		if ( !Recorder.record )
			return;

		Recorder.setCommand( COMMAND_NAME );
		generateAndStoreOptions( n5RootAndDataset, virtual, cropInterval );

		Recorder.saveCommand();
	}

	/**
	 * Read a single N5 dataset into a ImagePlus and show it
	 *
	 * @param <T> the image data type
	 * @param <M> the metadata type
	 * @param n5 the n5Reader
	 * @param datasetMeta datasetMetadata containing the path
	 * @param cropIntervalIn optional crop interval
	 * @param asVirtual whether to open virtually 
	 * @param ipMeta metadata
	 * @return the ImagePlus
	 * @throws IOException io
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > ImagePlus read( 
				final N5Reader n5, 
				final N5Metadata datasetMeta, final Interval cropIntervalIn, final boolean asVirtual,
				final ImageplusMetadata<M> ipMeta ) throws IOException
	{
		final String d = datasetMeta.getPath();
		final RandomAccessibleInterval imgRaw = N5Utils.open( n5, d );

		RandomAccessibleInterval img;
		if( cropIntervalIn != null )
		{
			img = Views.interval( imgRaw, processCropInterval( imgRaw, cropIntervalIn ));
			if( datasetMeta instanceof N5ImagePlusMetadata )
			{
				 ((N5ImagePlusMetadata)datasetMeta).crop( cropIntervalIn );
			}
		}
		else
			img = imgRaw;

		RandomAccessibleInterval< T > convImg;
		DataType type = datasetMeta.getAttributes().getDataType();

		final boolean isRGB = (datasetMeta instanceof N5ImagePlusMetadata) &&
				((N5ImagePlusMetadata)datasetMeta).getType() == ImagePlus.COLOR_RGB;

		// Compute LUT after crop
		if(	type == DataType.FLOAT64 )
		{
			convImg = convertDouble( img );
		}
		else if( isRGB && type == DataType.UINT32)
		{
			convImg = convertToRGB( img );
		}
		else if( type == DataType.INT32 || type == DataType.UINT32 ||
				 type == DataType.INT64 || type == DataType.UINT64 )
		{
			convImg = convertToUShortLUT( img );
		}
		else
		{
			// this covers int8 -> uint8 and int16 -> uint16
			convImg = img;
		}

		ImagePlus imp;
		if( asVirtual )
		{
			imp = ImageJFunctions.wrap( convImg, d );
		}
		else
		{
			ImagePlusImg<T,?> ipImg = new ImagePlusImgFactory<>( Util.getTypeFromInterval( convImg ) ).create( img );
			LoopBuilder.setImages( convImg, ipImg ).forEachPixel( (x,y) -> y.set( x ) );
			imp = ipImg.getImagePlus();
		}

		if( ipMeta != null )
		{
			try
			{
				ipMeta.writeMetadata( ( M ) datasetMeta, imp );
			}
			catch( final Exception e )
			{
				System.err.println("Failed to convert metadata to Imageplus for " + d );
			}
		}
		return imp;
	}

	public static RandomAccessibleInterval<FloatType> convertDouble(
			final RandomAccessibleInterval< DoubleType > img)
	{
		return Converters.convert( 
				img, 
				new RealFloatConverter< DoubleType >(),
				new FloatType() );
	}

	public static RandomAccessibleInterval< ARGBType >
		convertToRGB( final RandomAccessibleInterval< UnsignedIntType > img )
	{
		return Converters.convert( 
				img,
				new IntegerToaRGBConverter(),
				new ARGBType() );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static < T extends NumericType< T > & NativeType< T > > RandomAccessibleInterval< UnsignedShortType >
		convertToUShortLUT( final RandomAccessibleInterval< T > img )
	{
		return Converters.convert( 
				img,
				new UnsignedShortLUTConverter( Views.flatIterable( img ) ),
				new UnsignedShortType() );
	}

	private static Interval processCropInterval( final RandomAccessibleInterval< ? > img, final Interval cropInterval )
	{
		assert img.numDimensions() == cropInterval.numDimensions();

		final int nd = img.numDimensions();
		final long[] min = new long[ nd ];
		final long[] max = new long[ nd ];

		for( int i = 0; i < nd; i++ )
		{
			min[ i ] = Math.max( img.min( i ), cropInterval.min( i ) );
			max[ i ] = Math.min( img.max( i ), cropInterval.max( i ));
		}

		return new FinalInterval( min, max );
	}

	/**
	 * Read one or more N5 dataset into ImagePlus object(s),
	 * first prompting the user to specify crop parameters.
	 */
	public void processWithCrops()
	{
		asVirtual = selectionDialog.isVirtual();
		final String rootPath = selectionDialog.getN5RootPath();
		for ( final N5Metadata datasetMeta : selection.metadata )
		{
			// Macro.getOptions() does not return what I'd expect after this call.  why?
			// Macro.setOptions( String.format( "n5=%s", datasetMeta.getPath() ));

			final String datasetPath = datasetMeta.getPath();
			final String pathToN5Dataset = datasetPath.isEmpty() ? rootPath : rootPath + File.separator + datasetPath;

			numDimensionsForCrop = datasetMeta.getAttributes().getNumDimensions();
			initMaxValuesForCrop =
					Arrays.stream( datasetMeta.getAttributes().getDimensions())
						.map( x -> x - 1 )
						.toArray();

//			selectionDialog.setMessage( "Loading\n" + datasetPath );
			this.run( generateAndStoreOptions( pathToN5Dataset, asVirtual, null ));
		}
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them.
	 */
	public static List<ImagePlus> process( final N5Reader n5,
			final String rootPath,
			final List< N5Metadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval,
			final Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes )
	{
		return process( n5, rootPath, datasetMetadataList, asVirtual, cropInterval, true, impMetaWriterTypes );
	}

	/*
	 * Read one or more N5 dataset into ImagePlus object(s) and show them, if requested.
	 */
	public static List<ImagePlus> process( final N5Reader n5,
			final String rootPath,
			final List< N5Metadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval,
			final boolean show,
			final Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes )
	{
		final ArrayList<ImagePlus> imgList = new ArrayList<>();
		for ( final N5Metadata datasetMeta : datasetMetadataList )
		{
			// is this check necessary?
			if( datasetMeta == null )
				continue;

			final String d = normalPathName(datasetMeta.getPath(), n5.getGroupSeparator() );
			final String pathToN5Dataset = d.isEmpty() ? rootPath : rootPath + File.separator + d;

			final ImageplusMetadata< ? > impMeta = impMetaWriterTypes.get( datasetMeta.getClass() );
			ImagePlus imp;
			try
			{
				imp = N5Importer.read( n5, datasetMeta, cropInterval, asVirtual, impMeta );
				record( pathToN5Dataset, asVirtual, cropInterval );
				imgList.add( imp );
				if( show )
					imp.show();
			}
			catch ( final IOException e )
			{
				IJ.error( "failed to read n5" );
			}
		}
		return imgList;
	}

	/*
	 * Convenience method to process using the current state of this object.
	 * Can not be used directly when this plugin shows the crop dialog.
	 */
	public void process()
	{
		process( n5, selectionDialog.getN5RootPath(), selection.metadata, asVirtual, cropInterval, impMetaWriterTypes );
	}

	public List< ImagePlus > process( final String n5FullPath, final boolean asVirtual )
	{
		return process( n5FullPath, asVirtual, null );
	}

	public List<ImagePlus> process( final String n5FullPath, final boolean asVirtual, final Interval cropInterval )
	{
		n5 = new N5ViewerReaderFun().apply( n5FullPath );
		final String dataset = new N5BasePathFun().apply( n5FullPath );
		N5Metadata metadata;
		try
		{
			metadata = new N5DatasetDiscoverer( null, PARSERS ).parse( n5, dataset ).getMetadata();
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not parse metadata.");
			return null;
		}

		List< ImagePlus > result = process( n5, dataset, Collections.singletonList( metadata ),
				asVirtual, cropInterval, show, getImagePlusMetadataWriterMap() );

		n5.close();

		return result;
	}

	public void processThread()
	{
		loaderThread = new Thread()
		{
			@Override
			public void run()
			{
				process();
			}
		};
		loaderThread.run();
	}

	public static class N5ViewerReaderFun implements Function< String, N5Reader >
	{
		public String message;

		public N5Reader apply( final String n5PathIn )
		{
			N5Reader n5;
			if ( n5PathIn == null || n5PathIn.isEmpty() )
				return null;

			N5Factory factory = new N5Factory();
			try
			{
				n5 = factory.openReader( n5PathIn );
			}
			catch ( IOException e )
			{
				IJ.handleException( e );
				return null;
			}
			return n5;
		}
	}

	public static class N5BasePathFun implements Function< String, String >
	{
		public String message;

		@Override
		public String apply( final String n5Path )
		{
			final DataAccessType type = DataAccessType.detectType( n5Path.trim() );
			if ( type == null )
			{
				message = "Not a valid path or link to an N5 container.";
				return null;
			}

			switch ( type )
			{
			case HDF5:
				return h5DatasetPath( n5Path );
			default:
				return "";
			}
		}
	}

	public static String h5DatasetPath( final String h5PathAndDataset )
	{
		return h5DatasetPath( h5PathAndDataset, false );
	}

	public static String h5DatasetPath( final String h5PathAndDataset, final boolean getFilePath )
	{
		int len = 3;
		int i = h5PathAndDataset.lastIndexOf( ".h5" );

		if( i < 0 )
		{
			i = h5PathAndDataset.lastIndexOf( ".hdf5" );
			len =  5;
		}

		if( i < 0 )
		{
			i = h5PathAndDataset.lastIndexOf( ".hdf" );
			len =  4;
		}

		if( i < 0 )
			return "";
		else if( getFilePath )
			return h5PathAndDataset.substring( 0, i + len );
		else
			return h5PathAndDataset.substring( i + len );
	}

	public static class IntegerToaRGBConverter implements Converter< UnsignedIntType, ARGBType >
	{
		@Override
		public void convert( UnsignedIntType input, ARGBType output )
		{
			output.set( input.getInt() );
		}
	}

	private static String normalPathName( final String fullPath, final String groupSeparator )
	{
		return fullPath.replaceAll( "(^" + groupSeparator + "*)|(" + groupSeparator + "*$)", "" );
	}

}
