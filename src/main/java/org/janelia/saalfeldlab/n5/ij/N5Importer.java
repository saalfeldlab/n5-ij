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

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessException;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessFactory;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
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
					new DefaultMetadata( "", -1 )
				};

    private N5Reader n5;

	private DatasetSelectorDialog selectionDialog;

	private DataSelection selection;

	private Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes;

	private ImageplusMetadata< ? > impMeta;

	private Interval cropInterval;

	private boolean asVirtual;

	private boolean cropOption;

	private Thread loaderThread;

	private boolean record;

	private int numDimensionsForCrop;

	private long[] initMaxValuesForCrop;

	public N5Importer()
	{
		record = Recorder.record;
		Recorder.record = false;

		// default image plus metadata parsers
		impMetaWriterTypes = new HashMap< Class<?>, ImageplusMetadata< ? > >();
		impMetaWriterTypes.put( N5ImagePlusMetadata.class, new N5ImagePlusMetadata( "" ) );
		impMetaWriterTypes.put( N5CosemMetadata.class, new N5CosemMetadata( "", null, null ) );
		impMetaWriterTypes.put( N5SingleScaleMetadata.class, new N5SingleScaleMetadata());
		impMetaWriterTypes.put( DefaultMetadata.class, new DefaultMetadata( "", 1 ) );

		numDimensionsForCrop = 5;
		initMaxValuesForCrop = new long[ numDimensionsForCrop ];
		Arrays.fill( initMaxValuesForCrop, Long.MAX_VALUE );
	}

	public Map< Class< ? >, ImageplusMetadata< ? > > getImagePlusMetadataWriterMap()
	{
		ImageJFunctions impf;
		return impMetaWriterTypes;
	}
	
	public void setNumDimensionsForCropDialog( final int numDimensionsForCrop )
	{
		this.numDimensionsForCrop = numDimensionsForCrop; 
	}

	@Override
    public void run( String args )
	{
		String options = Macro.getOptions();
		boolean isMacro = ( options != null && !options.isEmpty() );
		boolean isCrop = ( args != null && !args.isEmpty() );

		if ( !isMacro && !isCrop )
		{
			// the fancy selector dialog
			selectionDialog = new DatasetSelectorDialog(
					new N5ViewerReaderFun(), 
					new N5BasePathFun(),
					null, // no group parsers
					PARSERS );
			selectionDialog.setVirtualOption( true );
			selectionDialog.setCropOption( true );
			selectionDialog.run( this::datasetSelectorCallBack );
		}
		else
		{
			String n5Path = Macro.getValue( args, n5PathKey, "" );
			boolean dialogAsVirtual = args.contains( " virtual" );

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
				return;

			n5Path = gd.getNextString();
			boolean openAsVirtual = gd.getNextBoolean();

			// we don't always know ahead of time the dimensionality
			long[] cropMin = new long[ numDimensionsForCrop ];
			long[] cropMax = new long[ numDimensionsForCrop ];

			for( int i = 0; i < numDimensionsForCrop; i++ )
				cropMin[ i ] = Math.max( 0, (long)Math.floor( gd.getNextNumber()));

			for( int i = 0; i < numDimensionsForCrop; i++ )
			{
				double v = gd.getNextNumber();
				cropMax[ i ] = Double.isInfinite( v ) ? Long.MAX_VALUE : (long)Math.ceil( v );
			}

			Interval thisDatasetCropInterval = new FinalInterval( cropMin, cropMax );

			N5Reader n5ForThisDataset  = new N5ViewerReaderFun().apply( n5Path );
			N5Metadata meta;
			try
			{
				meta = new N5DatasetDiscoverer( null, PARSERS ).parse( n5ForThisDataset, "" ).getMetadata();
				process( n5ForThisDataset, n5Path, Collections.singletonList( meta ), openAsVirtual, thisDatasetCropInterval,
						impMetaWriterTypes );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	private void datasetSelectorCallBack( final DataSelection selection )
	{
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
	 * @param n5 the n5Reader
	 * @param datasetMeta datasetMetadata containing the path
	 * @param cropInterval 
	 * @param asVirtual
	 * @param ipMeta
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > ImagePlus read( 
			final N5Reader n5, 
			final N5Metadata datasetMeta, final Interval cropIntervalIn, final boolean asVirtual,
			final ImageplusMetadata<M> ipMeta ) throws IOException
	{
		String d = datasetMeta.getPath();
		RandomAccessibleInterval<T> imgRaw = (RandomAccessibleInterval<T>) N5Utils.open( n5, d );

		RandomAccessibleInterval<T> img;
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

		ImagePlus imp;
		if( asVirtual )
		{
			imp = ImageJFunctions.wrap( img, d );
		}
		else
		{
			ImagePlusImg<T,?> ipImg = new ImagePlusImgFactory<>( Views.flatIterable( img ).firstElement()).create( img );
			LoopBuilder.setImages( img, ipImg ).forEachPixel( (x,y) -> y.set( x ) );
			imp = ipImg.getImagePlus();
		}

		if( ipMeta != null )
		{
			try
			{
				ipMeta.writeMetadata( ( M ) datasetMeta, imp );
			}
			catch( Exception e )
			{
				System.err.println("Failed to convert metadata to Imageplus for " + d );
			}
		}
		return imp;
	}

	private static Interval processCropInterval( final RandomAccessibleInterval< ? > img, final Interval cropInterval )
	{
		assert img.numDimensions() == cropInterval.numDimensions();

		int nd = img.numDimensions();
		long[] min = new long[ nd ];
		long[] max = new long[ nd ];

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
		for ( N5Metadata datasetMeta : selection.metadata )
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

	/**
	 * Read one or more N5 dataset into ImagePlus object(s) and show them.
	 * Parameters are stored in this object. 
	 */
	public static List<ImagePlus> process( final N5Reader n5, 
			final String rootPath,
			final List< N5Metadata> datasetMetadataList,
			final boolean asVirtual,
			final Interval cropInterval,
			final Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes ) 
	{
		ArrayList<ImagePlus> imgList = new ArrayList<>();
		for ( N5Metadata datasetMeta : datasetMetadataList )
		{
			String d = datasetMeta.getPath();
			String pathToN5Dataset = d.isEmpty() ? rootPath : rootPath + File.separator + d;

			ImageplusMetadata< ? > impMeta = impMetaWriterTypes.get( datasetMeta.getClass() );
			ImagePlus imp;
			try
			{
				imp = N5Importer.read( n5, datasetMeta, cropInterval, asVirtual, impMeta );
				record( pathToN5Dataset, asVirtual, cropInterval );
				imgList.add( imp );
				imp.show();
			}
			catch ( IOException e )
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
		String dataset = new N5BasePathFun().apply( n5FullPath );
		N5Metadata metadata;
		try
		{
			metadata = new N5DatasetDiscoverer( null, PARSERS ).parse( n5, dataset ).getMetadata();
		}
		catch ( IOException e )
		{
			System.err.println( "Could not parse metadata.");
			return null;
		}

		return process( n5, dataset, Collections.singletonList( metadata ),
				asVirtual, cropInterval, getImagePlusMetadataWriterMap() );
	}

	public void processThread() 
	{
//		String datasetPath = selection.metadata.get( 0 ).getPath();
//		selectionDialog.setMessage( "Loading\n" + datasetPath );
		loaderThread = new Thread()
		{
			public void run()
			{
				process();
			}
		};
		loaderThread.run();
	}

//	public static Interval containingBlockAlignedInterval(
//			final N5Reader n5, 
//			final String dataset, 
//			final Interval interval ) throws IOException
//	{
//		return containingBlockAlignedInterval( n5, dataset, interval );
//	}

	/**
	 * Returns the smallest {@link Interval} that contains the input interval
	 * and contains complete blocks.
	 * 
	 * @param n5 the n5 reader
	 * @param dataset the dataset
	 * @param interval the interval
	 * @return the smallest containing interval
	 * @throws IOException 
	 */
//	public static Interval containingBlockAlignedInterval(
//			final N5Reader n5, 
//			final String dataset, 
//			final Interval interval) throws IOException
//	{
//		// TODO move to N5Utils?
//		if ( !n5.datasetExists( dataset ) )
//		{
////			if( log != null )
////				log.error( "no dataset" );
//
//			return null;
//		}
//
//		DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
//		int nd = attrs.getNumDimensions();
//		int[] blockSize = attrs.getBlockSize();
//		long[] dims = attrs.getDimensions();
//
//		long[] min = new long[ nd ];
//		long[] max = new long[ nd ];
//		for( int d = 0; d < nd; d++ )
//		{
//			// check that interval aligns with blocks
//			min[ d ] = interval.min( d )- (interval.min( d ) % blockSize[ d ]);
//			max[ d ] = interval.max( d )  + ((interval.max( d )  + blockSize[ d ] - 1 ) % blockSize[ d ]);
//
//			// check that interval is contained in the dataset dimensions
//			min[ d ] = Math.max( 0, interval.min( d ) );
//			max[ d ] = Math.min( dims[ d ] - 1, interval.max( d ) );
//		}
//
//		return new FinalInterval( min, max );
//	}

	public static class N5ViewerReaderFun implements Function< String, N5Reader >
	{
		public String message;

		@Override
		public N5Reader apply( String n5Path )
		{
			N5Reader n5;
			if ( n5Path == null || n5Path.isEmpty() )
				return null;

			final DataAccessType type = DataAccessType.detectType( n5Path );
			if ( type == null )
			{
				message = "Not a valid path or link to an N5 container.";
				return null;
			}

			String n5BasePath = n5Path;
			if( type.equals( DataAccessType.HDF5 ))
				n5BasePath = N5Importer.h5DatasetPath( n5Path, true );

			n5 = null;
			try
			{
				n5 = new DataAccessFactory( type, n5BasePath ).createN5Reader( n5BasePath );

				/* 
				 * Do we need this check?
				 */
//				if ( !n5.exists( "/" ) || n5.getVersion().equals( new N5Reader.Version( null ) ) )
//				{
////					JOptionPane.showMessageDialog( dialog, "Not a valid path or link to an N5 container.", "N5 Viewer", JOptionPane.ERROR_MESSAGE );
//					return null;
//				}

			}
			catch ( final DataAccessException | IOException e )
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
		public String apply( String n5Path )
		{
			final DataAccessType type = DataAccessType.detectType( n5Path );
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

	public static String h5DatasetPath( final String h5PathAndDataset, boolean getFilePath )
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

}
