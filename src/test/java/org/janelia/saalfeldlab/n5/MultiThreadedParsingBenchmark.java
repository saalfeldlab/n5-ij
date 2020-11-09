package org.janelia.saalfeldlab.n5;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.janelia.saalfeldlab.n5.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessException;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessFactory;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessType;
import org.janelia.saalfeldlab.n5.ij.N5Importer;

public class MultiThreadedParsingBenchmark
{

	public static void main( String[] args ) throws IOException, DataAccessException
	{
//		ExecutorService executor = Executors.newSingleThreadExecutor();
//		ExecutorService executor = Executors.newFixedThreadPool( 16 );
		ExecutorService executor = Executors.newCachedThreadPool();
		N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer( executor, null, N5Importer.PARSERS );

        final String n5RootPath = args[ 0 ];
        final String n5Dataset = args[ 1 ];

		N5Reader n5 = new DataAccessFactory( DataAccessType.AMAZON_S3, n5RootPath ).createN5Reader( n5RootPath );
		N5TreeNode root = new N5TreeNode( n5Dataset, false );

		long start = System.currentTimeMillis();
		System.out.println( "discover" );
		LinkedBlockingQueue< Future< N5TreeNode > > parseFutures = discoverer.discoverThreads( n5, root );
		try
		{
			System.out.println( executor.isShutdown() );
			System.out.println( executor.isTerminated() );
			while( !parseFutures.isEmpty())
			{
				parseFutures.poll().get();
				//System.out.println("job done, still have " + parseFutures.size() );
			}
			System.out.println( "All jobs done" );
		}
		catch ( InterruptedException e )
		{
			e.printStackTrace();
		}
		catch ( ExecutionException e )
		{
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();

		System.out.println( "parsing took " + (end-start) + " ms");
	}

}
