package org.janelia.saalfeldlab.n5.benchmarks;

import java.util.List;

import org.janelia.saalfeldlab.n5.ij.N5Importer;

import ij.ImagePlus;
import ij.Prefs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Intervals;

public class ReadBenchmark {

	public static void main(String[] args) throws InterruptedException {

		final String n5RootAndDataset = args[0];

		if( args.length > 1 )
			Prefs.setThreads( Integer.parseInt(args[1]));

		System.out.println( "nthreads: " + Prefs.getThreads() );

		final long start = System.currentTimeMillis();
		final List<ImagePlus> impList = new N5Importer().process(n5RootAndDataset, false);
		final long end = System.currentTimeMillis();

		System.out.println( String.format("loading took %d ms ", (end-start)));
		System.out.println( impList.size() );
		final ImagePlus imp = impList.get(0);

		System.out.println( imp );
		imp.show();
		double bytesPerVox = imp.getBitDepth() / 8.0;
		final double numsec = (end - start) / 1000.0;
		final long numVoxels = Intervals.numElements( ImageJFunctions.wrap(imp));
		final double bytesPerSec = bytesPerVox * numVoxels / numsec;

		System.out.println( "bytesPerVox: " + bytesPerVox );
		System.out.println( "numVoxels: " + numVoxels );
		System.out.println( "numsecs: " + numsec );

		System.out.println( "bytes per sec: " + bytesPerSec );
		System.out.println( "Mbs: " + bytesPerSec / 1e6 );
	}

}
