package org.janelia.saalfeldlab.n5.ui;

import java.io.IOException;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5BasePathFun;
import org.janelia.saalfeldlab.n5.ij.N5Importer.N5ViewerReaderFun;
import org.janelia.saalfeldlab.n5.metadata.ImageplusMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.N5MetadataParser;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class N5LoadSingleDatasetPlugin implements PlugIn
{
	private static final String COMMAND_NAME = "N5 Open simple";

	private String n5Path;

	private boolean asVirtual;

	private N5Reader n5;

	private String dataset;
	
	private double[] initMin; 

	private double[] initMax; 
	
	private static final String[] axisNames = new String[] { "x", "y", "z", "c", "t" };
	
	private N5MetadataParser<?>[] parsers;
	
	private int numDimensions;

	private ImageplusMetadata<?> impMeta;

	private boolean record;

	public N5LoadSingleDatasetPlugin()
	{
		this( "", true, null, false );
	}

	public N5LoadSingleDatasetPlugin(
			final String initN5Path,
			final boolean initVirtual,
			final Interval initialCrop,
			final boolean record )
	{
		this.n5Path = initN5Path;
		this.asVirtual = initVirtual;
		this.record = record;
		
		if( initialCrop != null )
		{
			this.numDimensions = initialCrop.numDimensions();
			initMin = Intervals.minAsDoubleArray( initialCrop );
			initMax = Intervals.maxAsDoubleArray( initialCrop );
		}
		else
		{
			numDimensions = 3;
			initMin = new double[ 3 ];
			initMax = new double[ 3 ];
			Arrays.fill( initMax, Double.POSITIVE_INFINITY );
		}
		
		this.parsers = N5Importer.PARSERS;
	}

	public void setParsers( final N5MetadataParser< ? >[] parsers )
	{
		this.parsers = parsers;
	}

	public void setImagePlusMetadata( final ImageplusMetadata< ? > impMeta )
	{
		this.impMeta = impMeta;
	}

	@Override
	public void run( String arg )
	{
		final GenericDialog gd = new GenericDialog( "Import N5" );
		gd.addStringField( "N5 path", n5Path );
		gd.addCheckbox( "Virtual", asVirtual );

		gd.addMessage( " ");
		gd.addMessage( "Crop parameters.");
		gd.addMessage( "[0,Infinity] loads the whole volume.");
		gd.addMessage( "Min:");
		for( int i = 0; i < initMin.length; i++ )
			gd.addNumericField( axisNames[ i ], initMin[ i ] );

		gd.addMessage( "Max:");
		for( int i = 0; i < initMin.length; i++ )
			gd.addNumericField( axisNames[ i ], initMax[ i ] );

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		n5Path = gd.getNextString();
		asVirtual = gd.getNextBoolean();
		
		Interval subset = null;

		// we dont know ahead of time the dimensionality
		long[] cropMin = new long[ numDimensions ];
		long[] cropMax = new long[ numDimensions ];

		for( int i = 0; i < numDimensions; i++ )
			cropMin[ i ] = Math.max( 0, (long)Math.floor( gd.getNextNumber()));

		boolean setInterval = true;
		for( int i = 0; i < numDimensions; i++ )
		{
			double num = gd.getNextNumber();
			if( Double.isInfinite( num ))
			{
				subset = null;
				setInterval = false;
				break;
			}

			cropMax[ i ] = (long)Math.ceil( num );
			if( cropMax[ i ] < cropMin[ i ])
			{
				IJ.error( "Crop max must be greater than or equal to min" );
				return;
			}
			}
			if( setInterval )
				subset = new FinalInterval( cropMin, cropMax );

		n5 = new N5ViewerReaderFun().apply( n5Path );
		dataset = new N5BasePathFun().apply( n5Path ) ;
		ImagePlus imp;
		try
		{
			N5Metadata meta = new N5DatasetDiscoverer( null, parsers ).parse( n5, dataset ).getMetadata();
			if( impMeta == null )
				impMeta = new N5Importer().getImagePlusMetadataWriterMap().get( meta.getClass() );

			imp = N5Importer.read( n5, meta, subset, asVirtual, impMeta );
			imp.show();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		record( record, n5Path, asVirtual, subset );
	}
	
	public static void record( final boolean record, final String n5RootAndDataset, final boolean virtual, final Interval cropInterval )
	{
		if( !record )
			return;

		Recorder.setCommand( COMMAND_NAME );
		Recorder.recordOption( "n5", n5RootAndDataset );

		if( virtual )
			Recorder.recordOption( "virtual" );

		if( cropInterval != null )
		{
			System.out.println( "record crop" );
			for( int i = 0; i < cropInterval.numDimensions(); i++ )
			{
				Recorder.recordOption( axisNames[i],  Long.toString( cropInterval.min( i )));
				Recorder.recordOption( axisNames[i] + "_0",  Long.toString( cropInterval.max( i )));
			}
		}
		Recorder.saveCommand();
	}
	
	public static String argsToString( final String n5RootAndDataset, final boolean virtual, final Interval cropInterval )
	{
		String cropString = "";
		if( cropInterval != null )
		{
			StringBuffer cropArg = new StringBuffer();
			for( int i = 0; i < cropInterval.numDimensions(); i++ )
			{
				cropArg.append( axisNames[i] + "=" + Long.toString( cropInterval.min( i )));
				cropArg.append( " " );
			}
			for( int i = 0; i < cropInterval.numDimensions(); i++ )
			{
				cropArg.append( axisNames[i] + "_0="  + Long.toString( cropInterval.max( i )));
				cropArg.append( " " );
			}
			cropString = cropArg.toString();
		}

		String virtualString = virtual ? "virtual" : "";
		return String.format( "n5=%s %s %s", n5RootAndDataset, virtualString, cropString );
	}

}