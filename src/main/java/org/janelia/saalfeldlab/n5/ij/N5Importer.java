package org.janelia.saalfeldlab.n5.ij;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.AutoDetectMetadata;
import org.janelia.saalfeldlab.n5.metadata.DefaultMetadata;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataWriter;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.janelia.saalfeldlab.n5.ui.N5DatasetSelectorDialog;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import ij.ImagePlus;
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
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

@Plugin( type = Command.class, menuPath = "File>Import>Import N5" )
public class N5Importer implements Command, WindowListener
{
	public static final String BDV_OPTION = "BigDataViewer";
	public static final String IP_OPTION = "ImagePlus";

	public static final String MetadataAutoKey = "Auto-detect";
	public static final String MetadataImageJKey = "ImageJMetadata";
	public static final String MetadataN5CosemKey = "Cosem Metadata";
	public static final String MetadataN5ViewerKey = "N5Viewer Metadata";
	public static final String MetadataCustomKey = "CustomMetadata";
	public static final String MetadataDefaultKey = "DefaultMetadata";

	@Parameter
	private LogService log;
	
	@Parameter
	private UIService ui;

    @Parameter(visibility=ItemVisibility.MESSAGE, required=false)
    private String message = "Read an N5 container to an ImagePlus";
	
    @Parameter( label = "N5 root location")
    private String n5RootLocation;

    @Parameter( label = "N5 datasets (optional)", required=false, 
    		description="If not specified, you can select which datasets to open with from a dialog")
    private String datasetArg = "";
    
    // TODO consider implementing this later
//    @Parameter( label = "Interactive crop")
//    private boolean doInteractiveCrop = false;

    @Parameter( label = "Subset", required=false, 
    		description="Specify the subset of the volume to open. xmin,ymin,zmin;xmax,ymax,zmax" )
    private String subset = "";

    @Parameter( label = "as virtual?")
    private boolean isVirtual = false;

    @Parameter(label="metadata type", 
    		description = "The style for metadata stored in the N5 to import.",
    		choices={ 	MetadataAutoKey,
    					MetadataN5ViewerKey, 
    					MetadataN5CosemKey,
    					MetadataImageJKey } )
    private String metadataStyle = MetadataAutoKey;

    // TODO
    //@Parameter( label = "align to blocks", description = "description")
    //private boolean alignToBlockGrid;

    private N5Reader n5;

    private List<String> datasetList;

//	private N5DatasetSelectorDialog selectionDialog;

	private DatasetSelectorDialog selectionDialogNew;
	
	private DataSelection selection;
	
	private Map< String, N5MetadataParser< ? > > styles;

	private Map< Class< ? >, ImageplusMetadata< ? > > impMetaWriterTypes;

	public void setN5Root( String n5Root )
	{
		n5RootLocation = n5Root;
	}

	@Override
	public void run()
	{
		System.out.println("run");

		styles = new HashMap<String,N5MetadataParser<?>>();
		styles.put( MetadataAutoKey, new AutoDetectMetadata() );
		styles.put( MetadataImageJKey, new N5ImagePlusMetadata(""));
		styles.put( MetadataN5CosemKey, new N5CosemMetadata("", null, null));
		styles.put( MetadataN5ViewerKey, new N5ViewerMetadataParser());
		styles.put( MetadataDefaultKey, new DefaultMetadata( "", 1 ));
		
		impMetaWriterTypes = new HashMap< Class<?>, ImageplusMetadata< ? > >();
		impMetaWriterTypes.put( N5ImagePlusMetadata.class, new N5ImagePlusMetadata( "" ) );
		impMetaWriterTypes.put( N5CosemMetadata.class, new N5CosemMetadata( "", null, null ) );
		impMetaWriterTypes.put( N5ViewerMetadataParser.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( N5SingleScaleMetadata.class, new N5ViewerMetadataWriter());
		impMetaWriterTypes.put( DefaultMetadata.class, new DefaultMetadata( "", 1 ) );

		try
		{
			n5 = getReader();
			if( n5 == null )
			{
				log.error("Could not open as n5 root");
				return;
			}

			if( datasetArg == null || datasetArg.isEmpty() )
			{
//				selectionDialog = new N5DatasetSelectorDialog( n5 );
//				JFrame frame = selectionDialog.show();
//				frame.addWindowListener( this );

				selectionDialogNew = new DatasetSelectorDialog( n5,
						new N5ViewerMetadataParser( false ));

				selectionDialogNew.run(
						selection -> {
							this.selection = selection;
							try
							{
								process();
							}
							catch ( Exception e )
							{
								e.printStackTrace();
							}
						});
			}
			else
			{
				datasetList = Arrays.asList( datasetArg.split(","));
				process();
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends NumericType<T> & NativeType<T>, M extends N5Metadata > void process() throws ImgLibException, IOException
	{
		for( N5Metadata datasetMeta : selection.metadata )
		{
			String d = datasetMeta.getPath();
			RandomAccessibleInterval<T> imgRaw = (RandomAccessibleInterval<T>) N5Utils.open( n5, d );

			RandomAccessibleInterval<T> img;
			if( !subset.isEmpty() )
			{
				String[] minmax = subset.split(";");
				long[] min = Arrays.stream( minmax[ 0 ].split(",")).mapToLong( Long::parseLong ).toArray();
				long[] max = Arrays.stream( minmax[ 1 ].split(",")).mapToLong( Long::parseLong ).toArray();
				img = Views.interval( imgRaw, new FinalInterval( min, max ));
			}
			else
				img = imgRaw;

			ImagePlus imp = ImageJFunctions.wrap( img, d );

			try
			{ 
				ImageplusMetadata< M > ipMeta = ( ImageplusMetadata< M > ) impMetaWriterTypes.get( datasetMeta.getClass() );
				ipMeta.writeMetadata( ( M ) datasetMeta, imp );
			}
			catch( Exception e )
			{
				System.err.println("Failed to convert metadata to Imageplus for " + d );
			}

			imp.show();
		}
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

	private N5Reader getReader() throws IOException 
	{
		File f = new File( n5RootLocation );

		// TODO add extension requirements if more reader types are supported
		if( f.exists() && f.isDirectory())
		{
			return new N5FSReader( n5RootLocation );
		}
		else if( f.exists() && f.isFile() )
		{
			return new N5HDF5Reader( n5RootLocation );
		}

		return null;
	}

	@Override
	public void windowOpened(WindowEvent e) { }
	
	@Override
	public void windowIconified(WindowEvent e) { }
	
	@Override
	public void windowDeiconified(WindowEvent e) { }
	
	@Override
	public void windowDeactivated(WindowEvent e) { }
	
	@Override
	public void windowClosing(WindowEvent e)
	{
//		if( selectionDialog.getSelectedDatasets() != null && 
//				selectionDialog.getSelectedDatasets().size() > 0 )
//		{
////			dataset = selectionDialog.getSelectedDatasets().get( 0 );
//			datasetList = selectionDialog.getSelectedDatasets();
//		}
//		else
//		{
//			log.info("No dataset selected");
//			return;
//		}

		try
		{
			process();
		}
		catch (ImgLibException | IOException e1)
		{
			e1.printStackTrace();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowActivated(WindowEvent e) { }

}
