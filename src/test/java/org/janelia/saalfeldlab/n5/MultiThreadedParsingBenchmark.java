package org.janelia.saalfeldlab.n5;

import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedParsingBenchmark {

  public static void main(String[] args) throws IOException {
	//		ExecutorService executor = Executors.newSingleThreadExecutor();
	//		ExecutorService executor = Executors.newFixedThreadPool( 16 );
	final ExecutorService executor = Executors.newCachedThreadPool();

	final String n5RootPath = args[0];
	final String n5Dataset = args[1];

	final N5Reader n5 = new N5Factory().openReader(n5RootPath);
	List<N5MetadataParser<?>> metadataParsers = Arrays.asList(N5Importer.PARSERS);
	List<N5MetadataParser<?>> groupParsers = Arrays.asList(N5Importer.GROUP_PARSERS);
	final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5, executor, metadataParsers, groupParsers);

	long start = System.currentTimeMillis();
	System.out.println("discover");
	final N5TreeNode root = discoverer.discoverAndParseRecursive(n5Dataset);

	long end = System.currentTimeMillis();

	System.out.println("parsing took " + (end - start) + " ms");
  }

}
