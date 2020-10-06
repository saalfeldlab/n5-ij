package org.janelia.saalfeldlab.n5.ij;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
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
import org.janelia.saalfeldlab.n5.metadata.N5ViewerSingleMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataWriter;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5LoadSingleDatasetPlugin;
import org.scijava.log.LogService;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

public class N5Importer implements PlugIn
{
	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	public static final String MetadataAutoKey = "Auto-detect";
	public static final String MetadataImageJKey = "ImageJMetadata";
	public static final String MetadataN5CosemKey = "Cosem Metadata";
	public static final String MetadataN5ViewerKey = "N5Viewer Metadata";
	public static final String MetadataCustomKey = "CustomMetadata";
	public static final String MetadataDefaultKey = "DefaultMetadata";
	
	public static final N5MetadataParser<?>[] PARSERS = new N5MetadataParser[]{
					new N5ImagePlusMetadata( "" ),
					new N5CosemMetadata( "", null, null ),
					new N5ViewerSingleMetadataParser(),
					new DefaultMetadata( "", 1 )
				};

    private N5Reader n5;

	private DatasetSelectorDialog selectionDialog;

	private DataSelection selection;

	private Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes;

	private Interval subset;

	private boolean asVirtual;

	private boolean cropOption;

	private Thread loaderThread;

	private boolean record;

	public N5Importer()
	{
		record = Recorder.record;
		Recorder.record = false;

		// default image plus metadata parsers
		impMetaWriterTypes = new HashMap< Class<?>, ImageplusMetadata< ? > >();
		impMetaWriterTypes.put( N5ImagePlusMetadata.class, new N5ImagePlusMetadata( "" ) );
		impMetaWriterTypes.put( N5CosemMetadata.class, new N5CosemMetadata( "", null, null ) );
		impMetaWriterTypes.put( N5ViewerSingleMetadataParser.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( N5SingleScaleMetadata.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( DefaultMetadata.class, new DefaultMetadata( "", 1 ) );
	}

	public Map< Class< ? >, ImageplusMetadata< ? > > getImagePlusMetadataWriterMap()
	{
		return impMetaWriterTypes;
	}

	@Override
    public void run( String args )
	{
		selectionDialog = new DatasetSelectorDialog(
				new N5ViewerReaderFun(), 
				x -> "",
				null, // no group parsers
				PARSERS );
		selectionDialog.setVirtualOption( true );
		selectionDialog.setCropOption( true );
		selectionDialog.run(
				selection -> {
					this.selection = selection;
					this.n5 = selection.n5;
					this.asVirtual = selectionDialog.isVirtual();
					this.cropOption = selectionDialog.getCropOption();
					try { process(); }
					catch ( Exception e ) { e.printStackTrace(); }
				});
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > ImagePlus read( 
			final N5Reader n5, 
			final N5Metadata datasetMeta, final Interval subset, final boolean asVirtual,
			final ImageplusMetadata<M> ipMeta ) throws IOException
	{
		String d = datasetMeta.getPath();
		RandomAccessibleInterval<T> imgRaw = (RandomAccessibleInterval<T>) N5Utils.open( n5, d );

		RandomAccessibleInterval<T> img;
		if( subset != null )
		{
			img = Views.interval( imgRaw, subset );
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

	public <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > void process() 
	{
		Recorder.record = record;
		loaderThread = new Thread()
		{
			public void run()
			{
				for ( N5Metadata datasetMeta : selection.metadata )
				{
					String d = datasetMeta.getPath();
					String pathToN5Dataset = selectionDialog.getN5RootPath() + File.separator + d;

					if( cropOption )
					{
						new N5LoadSingleDatasetPlugin( pathToN5Dataset, asVirtual, null, record ).run( "" );
					}
					else
					{
						ImageplusMetadata< ? > impMeta = impMetaWriterTypes.get( datasetMeta.getClass() );
						ImagePlus imp;
						try
						{
							imp = N5Importer.read( n5, datasetMeta, subset, asVirtual, impMeta );
							imp.show();
							N5LoadSingleDatasetPlugin.record( record, pathToN5Dataset, asVirtual, null );
						}
						catch ( IOException e )
						{
							IJ.error( "failed to read n5" );
						}
					}

				}
			}
		};
		loaderThread.run();
		Recorder.record = record;
	}

	public static Interval containingBlockAlignedInterval(
			final N5Reader n5, 
			final String dataset, 
			final Interval interval ) throws IOException
	{
		return containingBlockAlignedInterval( n5, dataset, interval, null );
	}

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
	public static Interval containingBlockAlignedInterval(
			final N5Reader n5, 
			final String dataset, 
			final Interval interval,
			final LogService log ) throws IOException
	{
		// TODO move to N5Utils?
		if ( !n5.datasetExists( dataset ) )
		{
			if( log != null )
				log.error( "no dataset" );

			return null;
		}

		DatasetAttributes attrs = n5.getDatasetAttributes( dataset );
		int nd = attrs.getNumDimensions();
		int[] blockSize = attrs.getBlockSize();
		long[] dims = attrs.getDimensions();

		long[] min = new long[ nd ];
		long[] max = new long[ nd ];
		for( int d = 0; d < nd; d++ )
		{
			// check that interval aligns with blocks
			min[ d ] = interval.min( d )- (interval.min( d ) % blockSize[ d ]);
			max[ d ] = interval.max( d )  + ((interval.max( d )  + blockSize[ d ] - 1 ) % blockSize[ d ]);

			// check that interval is contained in the dataset dimensions
			min[ d ] = Math.max( 0, interval.min( d ) );
			max[ d ] = Math.min( dims[ d ] - 1, interval.max( d ) );
		}

		return new FinalInterval( min, max );
	}

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
//			if( type == DataAccessType.FILESYSTEM )
//			{
//				if( n5Path.contains( ".n5" ))
//					n5BasePath = n5Path.substring( 0, 3 + n5Path.indexOf( ".n5" ));
//			}

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
			case FILESYSTEM:
//				if( n5Path.contains( ".n5" ))
//					return n5Path.substring( 3 + n5Path.indexOf( ".n5" ));
				return "";
			default:
				return "";
			}
		}
	}

	/**
	 * Removes selected nodes that do not have metadata, and are therefore not openable.
	 */
	public static class N5IjTreeSelectionListener implements TreeSelectionListener
	{
		private TreeSelectionModel selectionModel;

		public N5IjTreeSelectionListener( TreeSelectionModel selectionModel )
		{
			this.selectionModel = selectionModel;
		}

		@Override
		public void valueChanged( TreeSelectionEvent sel )
		{
			int i = 0;
			for( TreePath path : sel.getPaths())
			{
				if( !sel.isAddedPath( i ))
					continue;

				Object last = path.getLastPathComponent();
				if( last instanceof N5TreeNode )
				{
					N5TreeNode node = (N5TreeNode)last;

					//if(!node.isDataset())
					if( node.getMetadata() == null )
					{
						selectionModel.removeSelectionPath( path );
					}
				}
				i++;
			}
		}
	}

}
