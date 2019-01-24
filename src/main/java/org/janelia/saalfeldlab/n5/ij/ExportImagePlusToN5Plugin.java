package org.janelia.saalfeldlab.n5.ij;

import java.awt.AWTEvent;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.scijava.command.Command;

import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5.AfterEachPlane;
import bdv.export.WriteSequenceToHdf5.LoopbackHeuristic;
import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.export.imgloader.ImagePlusImgLoader.MinMaxOption;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.spimdata.SequenceDescriptionMinimal;
import ij.Prefs;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;


/**
 * Most of this is take verbatim from bdv.ij.ExportImagePlus.
 * Replaced the xml-hdf5 writers with n5 file system writer.
 *
 */
public class ExportImagePlusToN5Plugin implements Command {

	public static void main(String[] args) {
		new ImageJ();
		//IJ.run("Confocal Series (2.2MB)");
		System.out.println("load");
		ImagePlus imp = IJ.openImage("/Users/bogovicj/tmp/confocal-series.tif");
		imp.show();
		System.out.println("run");
		new ExportImagePlusToN5Plugin().run();
	}
	
	@Override
	public void run()
	{
		if ( ij.Prefs.setIJMenuBar )
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		// get the current image
		final ImagePlus imp = WindowManager.getCurrentImage();

		// make sure there is one
		if ( imp == null )
		{
			IJ.showMessage( "Please open an image first." );
			return;
		}

		// check the image type
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
		case ImagePlus.GRAY32:
			break;
		default:
			IJ.showMessage( "Only 8, 16, 32-bit images are supported currently!" );
			return;
		}
		
		// get calibration and image size
		final double pw = imp.getCalibration().pixelWidth;
		final double ph = imp.getCalibration().pixelHeight;
		final double pd = imp.getCalibration().pixelDepth;
		String punit = imp.getCalibration().getUnit();
		if ( punit == null || punit.isEmpty() )
			punit = "px";
		final FinalVoxelDimensions voxelSize = new FinalVoxelDimensions( punit, pw, ph, pd );
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();
		final FinalDimensions size = new FinalDimensions( new int[] { w, h, d } );

		// propose reasonable mipmap settings
		final ExportMipmapInfo autoMipmapSettings = ProposeMipmaps.proposeMipmaps( new BasicViewSetup( 0, "", size, voxelSize ) );

		// show dialog to get output paths, resolutions, subdivisions, min-max option
		final Parameters params = getParameters( imp.getDisplayRangeMin(), imp.getDisplayRangeMax(), autoMipmapSettings );
		if ( params == null )
			return;

		final ProgressWriter progressWriter = new ProgressWriterIJ();
		progressWriter.out().println( "starting export..." );

		// create ImgLoader wrapping the image
		final ImagePlusImgLoader< ? > imgLoader;
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
			imgLoader = ImagePlusImgLoader.createGray8( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
			break;
		case ImagePlus.GRAY16:
			imgLoader = ImagePlusImgLoader.createGray16( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
			break;
		case ImagePlus.GRAY32:
		default:
			imgLoader = ImagePlusImgLoader.createGray32( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
			break;
		}

		final int numTimepoints = imp.getNFrames();
		final int numSetups = imp.getNChannels();

		// create SourceTransform from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );

		// write n5
		final HashMap< Integer, BasicViewSetup > setups = new HashMap<>( numSetups );
		for ( int s = 0; s < numSetups; ++s )
		{
			final BasicViewSetup setup = new BasicViewSetup( s, String.format( "channel %d", s + 1 ), size, voxelSize );
			setup.setAttribute( new Channel( s + 1 ) );
			setups.put( s, setup );
		}
		final ArrayList< TimePoint > timepoints = new ArrayList<>( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( new TimePoint( t ) );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, imgLoader, null );

		Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo;
		perSetupExportMipmapInfo = new HashMap<>();
		final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo( params.resolutions, params.subdivisions );
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );
		
		// LoopBackHeuristic:
		// - If saving more than 8x on pixel reads use the loopback image over
		//   original image
		// - For virtual stacks also consider the cache size that would be
		//   required for all original planes contributing to a "plane of
		//   blocks" at the current level. If this is more than 1/4 of
		//   available memory, use the loopback image.
		final boolean isVirtual = imp.getStack().isVirtual();
		final long planeSizeInBytes = imp.getWidth() * imp.getHeight() * imp.getBytesPerPixel();
		final long ijMaxMemory = IJ.maxMemory();
		final int numCellCreatorThreads = Math.max( 1, PluginHelper.numThreads() - 1 );
		final LoopbackHeuristic loopbackHeuristic = new LoopbackHeuristic()
		{
			@Override
			public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
			{
				if ( previousLevel < 0 )
					return false;

				if ( WriteSequenceToN5.numElements( factorsToOriginalImg ) / WriteSequenceToN5.numElements( factorsToPreviousLevel ) >= 8 )
					return true;

				if ( isVirtual )
				{
					final long requiredCacheSize = planeSizeInBytes * factorsToOriginalImg[ 2 ] * chunkSize[ 2 ];
					if ( requiredCacheSize > ijMaxMemory / 4 )
						return true;
				}

				return false;
			}
		};
		

		final AfterEachPlane afterEachPlane = new AfterEachPlane()
		{
			@Override
			public void afterEachPlane( final boolean usedLoopBack )
			{
				if ( !usedLoopBack && isVirtual )
				{
					final long free = Runtime.getRuntime().freeMemory();
					final long total = Runtime.getRuntime().totalMemory();
					final long max = Runtime.getRuntime().maxMemory();
					final long actuallyFree = max - total + free;

					if ( actuallyFree < max / 2 )
						imgLoader.clearCache();
				}
			}

		};

		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
			WriteSequenceToN5.writeN5File(
				seq, new UnsignedByteType(), perSetupExportMipmapInfo, params.deflate, params.n5File, 
				loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
			break;
		case ImagePlus.GRAY16:
			WriteSequenceToN5.writeN5File(
				seq, new UnsignedShortType(), perSetupExportMipmapInfo, params.deflate, params.n5File, 
				loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
			break;
		case ImagePlus.GRAY32:
		default:
			WriteSequenceToN5.writeN5File(
					seq, new FloatType(), perSetupExportMipmapInfo, params.deflate, params.n5File, 
					loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
			break;
		}
		System.out.println("n5 writing done");
	}


	/**
	 * Taken from bdv.ij.ExportImagePlus
	 */
	protected static class Parameters
	{
		final boolean setMipmapManual;

		final int[][] resolutions;

		final int[][] subdivisions;

		final File n5File;

		final MinMaxOption minMaxOption;

		final double rangeMin;

		final double rangeMax;

		final boolean deflate;

		public Parameters(
				final boolean setMipmapManual, final int[][] resolutions, final int[][] subdivisions,
				final File n5File, final MinMaxOption minMaxOption, final double rangeMin, final double rangeMax,
				final boolean deflate )
		{
			this.setMipmapManual = setMipmapManual;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.n5File = n5File;
			this.minMaxOption = minMaxOption;
			this.rangeMin = rangeMin;
			this.rangeMax = rangeMax;
			this.deflate = deflate;
		}
	}

	static boolean lastSetMipmapManual = false;

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

	static String lastChunkSizes = "{32,32,4}, {16,16,8}, {8,8,8}";

	static int lastMinMaxChoice = 2;

	static double lastMin = 0;

	static double lastMax = 65535;

	static boolean lastDeflate = true;

	static String lastExportPath = "./export.n5";

	protected Parameters getParameters( final double impMin, final double impMax, final ExportMipmapInfo autoMipmapSettings  )
	{
		if ( lastMinMaxChoice == 0 ) // use ImageJs...
		{
			lastMin = impMin;
			lastMax = impMax;
		}

		while ( true )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

			gd.addCheckbox( "manual_mipmap_setup", lastSetMipmapManual );
			final Checkbox cManualMipmap = ( Checkbox ) gd.getCheckboxes().lastElement();
			gd.addStringField( "Subsampling_factors", lastSubsampling, 25 );
			final TextField tfSubsampling = ( TextField ) gd.getStringFields().lastElement();
			gd.addStringField( "N5_chunk_sizes", lastChunkSizes, 25 );
			final TextField tfChunkSizes = ( TextField ) gd.getStringFields().lastElement();

			gd.addMessage( "" );
			final String[] minMaxChoices = new String[] { "Use ImageJ's current min/max setting", "Compute min/max of the (hyper-)stack", "Use values specified below" };
			gd.addChoice( "Value_range", minMaxChoices, minMaxChoices[ lastMinMaxChoice ] );
			final Choice cMinMaxChoices = (Choice) gd.getChoices().lastElement();
			gd.addNumericField( "Min", lastMin, 0 );
			final TextField tfMin = (TextField) gd.getNumericFields().lastElement();
			gd.addNumericField( "Max", lastMax, 0 );
			final TextField tfMax = (TextField) gd.getNumericFields().lastElement();

			gd.addMessage( "" );
			gd.addCheckbox( "use_deflate_compression", lastDeflate );

			gd.addMessage( "" );
			N5PluginHelper.addSaveAsFileField( gd, "Export_path", lastExportPath, 25 );

//			gd.addMessage( "" );
//			gd.addMessage( "This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n" );
//			Bead_Registration.addHyperLinkListener( ( MultiLineLabel ) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de" );

			final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
			final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );
			gd.addDialogListener( new DialogListener()
			{
				@Override
				public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
				{
					gd.getNextBoolean();
					gd.getNextString();
					gd.getNextString();
					gd.getNextChoiceIndex();
					gd.getNextNumber();
					gd.getNextNumber();
					gd.getNextBoolean();
					gd.getNextString();
					
					if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cMinMaxChoices )
					{
						final boolean enable = cMinMaxChoices.getSelectedIndex() == 2;
						tfMin.setEnabled( enable );
						tfMax.setEnabled( enable );
					}
					else if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cManualMipmap )
					{
						final boolean useManual = cManualMipmap.getState();
						tfSubsampling.setEnabled( useManual );
						tfChunkSizes.setEnabled( useManual );
						if ( !useManual )
						{
							tfSubsampling.setText( autoSubsampling );
							tfChunkSizes.setText( autoChunkSizes );
						}
					}
					return true;
				}
			} );

			final boolean enable = lastMinMaxChoice == 2;
			tfMin.setEnabled( enable );
			tfMax.setEnabled( enable );

			tfSubsampling.setEnabled( lastSetMipmapManual );
			tfChunkSizes.setEnabled( lastSetMipmapManual );
			if ( !lastSetMipmapManual )
			{
				tfSubsampling.setText( autoSubsampling );
				tfChunkSizes.setText( autoChunkSizes );
			}

			gd.showDialog();
			if ( gd.wasCanceled() )
				return null;

			lastSetMipmapManual = gd.getNextBoolean();
			lastSubsampling = gd.getNextString();
			lastChunkSizes = gd.getNextString();
			lastMinMaxChoice = gd.getNextChoiceIndex();
			lastMin = gd.getNextNumber();
			lastMax = gd.getNextNumber();
			lastDeflate = gd.getNextBoolean();
			lastExportPath = gd.getNextString();

			// parse mipmap resolutions and cell sizes
			final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );
			if ( resolutions.length == 0 )
			{
				IJ.showMessage( "Cannot parse subsampling factors " + lastSubsampling );
				continue;
			}
			if ( subdivisions.length == 0 )
			{
				IJ.showMessage( "Cannot parse n5 chunk sizes " + lastChunkSizes );
				continue;
			}
			else if ( resolutions.length != subdivisions.length )
			{
				IJ.showMessage( "subsampling factors and n5 chunk sizes must have the same number of elements" );
				continue;
			}

			final MinMaxOption minMaxOption;
			if ( lastMinMaxChoice == 0 )
				minMaxOption = MinMaxOption.TAKE_FROM_IMAGEPROCESSOR;
			else if ( lastMinMaxChoice == 1 )
				minMaxOption = MinMaxOption.COMPUTE;
			else
				minMaxOption = MinMaxOption.SET;

			String n5Filename = lastExportPath;
			if ( !n5Filename.endsWith( ".n5" ) )
				n5Filename += ".n5";

			final File n5File = new File( n5Filename );
			final File parent = n5File.getParentFile();
			if ( parent == null || !parent.exists() || !parent.isDirectory() )
			{
				IJ.showMessage( "Invalid export filename " + n5Filename );
				continue;
			}


			return new Parameters( lastSetMipmapManual, resolutions, subdivisions, n5File, minMaxOption, lastMin, lastMax, lastDeflate );
		}
	}

	/**
	 * Create a new helper because the original chooses only xml files.
	 *
	 */
	public static class N5PluginHelper extends PluginHelper {

		public static void addSaveAsFileField( final GenericDialogPlus dialog, final String label, final String defaultPath, final int columns) {
			dialog.addStringField( label, defaultPath, columns );

			final TextField text = ( TextField ) dialog.getStringFields().lastElement();
			final GridBagLayout layout = ( GridBagLayout ) dialog.getLayout();
			final GridBagConstraints constraints = layout.getConstraints( text );

			final Button button = new Button( "Browse..." );
			final ChooseFileListener listener = new ChooseFileListener( text, ".n5" );
			button.addActionListener( listener );
			button.addKeyListener( dialog );

			final Panel panel = new Panel();
			panel.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
			panel.add( text );
			panel.add( button );

			layout.setConstraints( panel, constraints );
			dialog.add( panel );
		}	
	}
	
	public static class ChooseFileListener implements ActionListener
	{
		TextField text;
		String extension;

		public ChooseFileListener( final TextField text, final String extension )
		{
			this.text = text;
			if( extension.startsWith("."))
				this.extension = extension;
			else
				this.extension = "." + extension;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			File directory = new File( text.getText() );
			final String fn = directory.getName();
			while ( directory != null && !directory.exists() )
				directory = directory.getParentFile();

			if ( Prefs.useJFileChooser )
			{
				final JFileChooser fc = new JFileChooser( directory );
				fc.setSelectedFile( new File( fn ) );
				fc.setFileFilter( new FileFilter()
				{
					@Override
					public String getDescription()
					{
						return extension + " files";
					}

					@Override
					public boolean accept( final File f )
					{
						if ( f.isDirectory() )
							return true;
						if ( f.isFile() )
						{
							final String s = f.getName();
							final int i = s.lastIndexOf( '.' );
							if ( i > 0 && i < s.length() - 1 )
							{
								final String ext = s.substring( i + 1 ).toLowerCase();
								return ext.equals( extension );
							}
						}
						return false;
					}
				} );

				fc.setFileSelectionMode( JFileChooser.FILES_ONLY );

				final int returnVal = fc.showSaveDialog( null );
				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					String f = fc.getSelectedFile().getAbsolutePath();
					if ( !f.endsWith( extension ) )
						f += extension;
					text.setText( f );
				}
			}
			else // use FileDialog
			{
				final FileDialog fd = new FileDialog( ( Frame ) null, "Save", FileDialog.SAVE );
				fd.setDirectory( directory.getAbsolutePath() );
				fd.setFile( fn );
				fd.setFilenameFilter( new FilenameFilter()
				{
					@Override
					public boolean accept( final File dir, final String name )
					{
						final int i = name.lastIndexOf( '.' );
						if ( i > 0 && i < name.length() - 1 )
						{
							final String ext = name.substring( i + 1 ).toLowerCase();
							return ext.equals( extension );
						}
						return false;
					}
				} );
				fd.setVisible( true );
				final String filename = fd.getFile();
				if ( filename != null )
				{
					String f = new File( fd.getDirectory() + filename ).getAbsolutePath();
					if ( !f.endsWith( extension ) )
						f += extension;
					text.setText( f );
				}
			}
		}
	}
}
