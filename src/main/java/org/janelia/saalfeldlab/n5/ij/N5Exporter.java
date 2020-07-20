package org.janelia.saalfeldlab.n5.ij;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataWriter;
import org.janelia.saalfeldlab.n5.metadata.N5SingleScaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadataWriter;
import org.janelia.saalfeldlab.n5.ui.N5MetadataSpecDialog;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin( type = Command.class, menuPath = "File>Save As>Export N5" )
public class N5Exporter implements Command, WindowListener
{
	public static final String N5FS = "Filesystem";
	public static final String N5H5 = "Hdf5";
	public static final String N5Zarr = "Zarr"; // TODO

	public static final String GZIP_COMPRESSION = "gzip";
	public static final String RAW_COMPRESSION = "raw";
	public static final String LZ4_COMPRESSION = "lz4";
	public static final String XZ_COMPRESSION = "xz";
	public static final String BLOSC_COMPRESSION = "blosc";

    @Parameter(visibility=ItemVisibility.MESSAGE, required=false)
    private String message = "Export an ImagePlus to an N5 container.";

	@Parameter
	private LogService log;

	@Parameter
	private ImagePlus image; // or use Dataset?
	
    @Parameter( label = "n5 root")
    private String n5RootLocation;

    @Parameter( label = "dataset", required = false, 
    		description = "This argument is ignored if the N5ViewerMetadata style is selected" )
    private String n5Dataset;

    @Parameter( label = "block size")
    private String blockSizeArg;

    @Parameter( label = "compresstion",
    		choices={GZIP_COMPRESSION, RAW_COMPRESSION, LZ4_COMPRESSION, XZ_COMPRESSION}, style="listBox")
    private String compressionArg = GZIP_COMPRESSION;

    @Parameter( label = "container type", choices={N5FS, N5H5}, style="listBox")
    private String type = N5FS;
    
    @Parameter( label="metadata type", 
    		description = "The style for metadata to be stored in the exported N5.",
    		choices={ 	N5Importer.MetadataN5ViewerKey, 
    					N5Importer.MetadataN5CosemKey,
    					N5Importer.MetadataSimpleKey } )
    private String metadataStyle = N5Importer.MetadataN5ViewerKey;

    private int[] blockSize;

	private Map<String, N5MetadataWriter<?>> styles;

	private ImageplusMetadata<?> impMeta;

	private N5MetadataSpecDialog metaSpecDialog;

	@SuppressWarnings( "unchecked" )
	public < T extends RealType< T > & NativeType< T >, M extends N5Metadata > void process() throws IOException
	{
		if( image.getNFrames() > 1 && metadataStyle.equals( N5Importer.MetadataN5ViewerKey ))
		{
			log.error("Writer does not yet support time points");
			return;
		}

		N5Writer n5 = getWriter();
		Compression compression = getCompression();
		blockSize = Arrays.stream( blockSizeArg.split( "," )).mapToInt( x -> Integer.parseInt( x ) ).toArray();

		Img<T> img = ImageJFunctions.wrap( image );
		N5MetadataWriter<M> writer = ( N5MetadataWriter< M > ) styles.get( metadataStyle );

		String datasetString = "";
		for( int c = 0; c < image.getNChannels(); c++ )
		{
			RandomAccessibleInterval<T> channelImg;
			if( img.numDimensions() >= 4 )
			{
				channelImg = Views.hyperSlice( img, 2, c );
			}
			else
			{
				channelImg = img;
			}

			if( metadataStyle.equals( N5Importer.MetadataN5ViewerKey ))
			{
				datasetString = String.format( "/c%d/s0", c );
			}
			else if( image.getNChannels() > 1 )
			{
				datasetString = String.format( "%s/c%d", n5Dataset, c );
			}
			else
			{
				datasetString = n5Dataset;
			}

			N5Utils.save( channelImg , n5, datasetString, blockSize, compression );

			try
			{
				M meta = ( M ) impMeta.readMetadata( image );
				writer.writeMetadata( meta, n5, datasetString );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run()
	{
		styles = new HashMap<String,N5MetadataWriter<?>>();
		styles.put( N5Importer.MetadataN5ViewerKey, new N5ViewerMetadataWriter() );
		styles.put( N5Importer.MetadataN5CosemKey, new N5CosemMetadata( "", null, null ) );
		styles.put( N5Importer.MetadataSimpleKey, new N5ImagePlusMetadata("") );

		// TODO expand with more options
		impMeta = new N5ImagePlusMetadata("");

		if( metadataStyle.equals(  N5Importer.MetadataCustomKey  ))
		{
			metaSpecDialog = new N5MetadataSpecDialog( this );
			metaSpecDialog.show( MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );
//			metaSpecDialog.show( MetadataTemplateMapper.COSEM_MAPPER );
		}
		else
		{
			try
			{
				process();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	private Compression getCompression()
	{
		switch( compressionArg )
		{
		case GZIP_COMPRESSION:
			return new GzipCompression();
		case LZ4_COMPRESSION:
			return new Lz4Compression();
		case XZ_COMPRESSION:
			return new XzCompression();
		case RAW_COMPRESSION:
			return new RawCompression();
		default:
			return new RawCompression();
		}
	}
	
	private N5Writer getWriter() throws IOException
	{
		switch( type )
		{
		case N5FS:
			return new N5FSWriter( n5RootLocation );
		case N5H5:
			return new N5HDF5Writer( n5RootLocation, blockSize );
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
		// TODO fix
//		styles.put( N5Importer.MetadataCustomKey, metaSpecDialog.getMapper() );
		try
		{
			process();
		}
		catch ( IOException e1 )
		{
			e1.printStackTrace();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowActivated(WindowEvent e) { }

}
