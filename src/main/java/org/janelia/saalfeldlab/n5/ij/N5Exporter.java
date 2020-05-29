package org.janelia.saalfeldlab.n5.ij;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.XzCompression;
import org.janelia.saalfeldlab.n5.bdv.N5ExportMetadataWriter;
import org.janelia.saalfeldlab.n5.hdf5.N5HDF5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMetadata;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

@Plugin( type = Command.class, menuPath = "File>Save As>Export N5" )
public class N5Exporter implements Command
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

    @Parameter( label = "container type",
    		choices={N5FS, N5H5}, style="listBox")
    private String type = N5FS;
    
    @Parameter(label="metadata type", choices={ N5Importer.MetadataN5ViewerKey, N5Importer.MetadataSimpleKey } )
    private String metadataStyle = N5Importer.MetadataN5ViewerKey;


    private int[] blockSize;
  
	public Map<String, N5Metadata<ImagePlus>> styles;


	public <T extends RealType<T> & NativeType<T>> void process() throws IOException
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
		N5Metadata<ImagePlus> meta = styles.get( metadataStyle );

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
			else
			{
				datasetString = n5Dataset;
			}

			N5Utils.save( channelImg , n5, datasetString, blockSize, compression );
			meta.metadataToN5( image, n5, datasetString );
		}

	}

	@Override
	public void run()
	{
		styles = new HashMap<String,N5Metadata<ImagePlus>>();
		styles.put( N5Importer.MetadataN5ViewerKey, new N5ViewerMetadata());
		styles.put( N5Importer.MetadataSimpleKey, new N5ImagePlusMetadata());

		try
		{
			process();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
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

}
