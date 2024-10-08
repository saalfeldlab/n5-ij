package org.janelia.saalfeldlab.n5.exps;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GithubActionsIssues {

	private static File baseDir;

	@BeforeClass
	public static void before() {

		try {
			baseDir = Files.createTempDirectory("n5-ij-tests-").toFile();
			baseDir.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@AfterClass
	public static void after() {

		baseDir.delete();
	}

	@Test
	public void n5WriteRead() {

		try (final N5FSWriter n5w = new N5FSWriter(baseDir.getAbsolutePath())) {

			for (int i = 0; i < 50; i++) {

				final String dset = String.format("%04d", i);
				n5w.createDataset(dset,
						new DatasetAttributes(new long[]{6, 5, 4}, new int[]{6, 5, 4}, DataType.FLOAT32, new RawCompression()));

				try (final N5FSReader n5r = new N5FSReader(baseDir.getAbsolutePath())) {

					final DatasetAttributes attrs = n5r.getDatasetAttributes(dset);
					assertNotNull("null attrs for dataset: " + dset , attrs);
					n5r.close();
					n5w.remove(dset);
				}
			}
			n5w.remove();
			n5w.close();
		}
	}

}
