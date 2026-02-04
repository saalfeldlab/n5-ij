package org.janelia.saalfeldlab.n5.parse;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.janelia.saalfeldlab.n5.parse.BlockSizeParsers.BlockSizeParser;
import org.janelia.saalfeldlab.n5.parse.BlockSizeParsers.DOWNSAMPLE_POLICY;
import org.janelia.saalfeldlab.n5.parse.BlockSizeParsers.DownsampledBlockParser;
import org.junit.Test;


public class BlockSizeParsingTests {

	@Test
	public void testBlockParsing() {

		long[] dimensions = new long[]{256, 256, 256, 256, 256};
		BlockSizeParser parser = new BlockSizeParser(dimensions);

		final String simple = "64,32,1,64,16";
		assertArrayEquals(new int[]{64, 32, 1, 64, 16}, parser.parse(simple));

		final String autoFill = "64";
		assertArrayEquals(new int[]{64, 64, 1, 64, 64}, parser.parse(autoFill));

		final String partiallyComplete = "64,32";
		assertArrayEquals(new int[]{64, 32, 1, 32, 32}, parser.parse(partiallyComplete));

		final String fillEmpty = "64,,,,16";
		assertArrayEquals(new int[]{64, 64, 1, 64, 16}, parser.parse(fillEmpty));

		final String fillNonNumeric = "64,b,\n,a,16";
		assertArrayEquals(new int[]{64, 64, 1, 64, 16}, parser.parse(fillNonNumeric));

		final String invalid = "64,-1";
		assertThrows(IllegalArgumentException.class, () -> parser.parse(invalid));

		final String invalid2 = "64,0";
		assertThrows(IllegalArgumentException.class, () -> parser.parse(invalid2));

		long[] dimensionsSmall = new long[]{256, 32, 2, 256, 256};
		BlockSizeParser parserClamping = new BlockSizeParser(dimensionsSmall);

		assertArrayEquals(new int[]{64, 32, 1, 64, 64}, parserClamping.parse(autoFill));
	}

	@Test
	public void testDownsampledBlockParsingConservative() {

		final long[] dimensions = new long[] {128,128,3,64,32};
		final double[] resolutions = new double[] {1, 1, 1, 1, 1};
		final DownsampledBlockParser parser = new DownsampledBlockParser(dimensions, resolutions);
		parser.setDownsamplingPolicy(DOWNSAMPLE_POLICY.Conservative);

		assertArray2dEquals(
				new int[][]{{128, 128, 1, 64, 32}},
				parser.parse("128"));

		assertArray2dEquals(
				new int[][]{{32, 32, 1, 32, 32}, {32, 32, 1, 32, 32}},
				parser.parse("32"));

		assertArray2dEquals(
				new int[][]{{128, 128, 3, 64, 32}},
				parser.parse("128,128,3,64,32"));

		assertArray2dEquals(
				new int[][]{{32, 32, 3, 64, 32}},
				parser.parse("32,32,3,64,32"));

		assertArray2dEquals(
				new int[][]{{32, 32, 3, 32, 32}, {32, 32, 3, 32, 32}},
				parser.parse("32,32,3,32,32"));

		assertArray2dEquals(
				new int[][]{{64, 32, 3, 32, 32}, {32, 32, 3, 32, 32}},
				parser.parse("64,32,3,32,32;32,32,3,32,32"));
		
		final double[] resolutionsAniso = new double[] {1, 1, 1, 4, 1};
		final DownsampledBlockParser parserAniso = new DownsampledBlockParser(dimensions, resolutionsAniso);
		parserAniso.setDownsamplingPolicy(DOWNSAMPLE_POLICY.Conservative);

		assertArray2dEquals(
				new int[][]{{128, 128, 1, 32, 32}},
				parserAniso.parse("128"));
	}

	@Test
	public void testDownsampledBlockParsingAggressive() {
		
		final long[] dimensions = new long[] {128,128,3,64,32};
		final double[] resolutions = new double[] {1, 1, 1, 1, 1};
		final DownsampledBlockParser parser = new DownsampledBlockParser(dimensions, resolutions);
		parser.setDownsamplingPolicy(DOWNSAMPLE_POLICY.Aggressive);
		
		assertArray2dEquals(
				new int[][]{{128, 128, 1, 64, 32}},
				parser.parse("128"));

		assertArray2dEquals(
				new int[][]{{32, 32, 1, 32, 32}, {32, 32, 1, 32, 32}, {32, 32, 1, 16, 32}},
				parser.parse("32"));

		assertArray2dEquals(
				new int[][]{{128, 128, 3, 64, 32}},
				parser.parse("128,128,3,64,32"));

		assertArray2dEquals(
				new int[][]{{32, 32, 3, 64, 32}, {32, 32, 3, 32, 32}, {32, 32, 3, 16, 32}},
				parser.parse("32,32,3,64,32"));

		assertArray2dEquals(
				new int[][]{{32, 32, 3, 32, 32}, {32, 32, 3, 32, 32}, {32, 32, 3, 16, 32}},
				parser.parse("32,32,3,32,32"));

		assertArray2dEquals(
				new int[][]{{64, 32, 3, 32, 32}, {32, 32, 3, 32, 32}, {32, 32, 3, 16, 32}},
				parser.parse("64,32,3,32,32;32,32,3,32,32"));


		final double[] resolutionsAniso = new double[] {1, 1, 1, 4, 1};
		final DownsampledBlockParser parserAniso = new DownsampledBlockParser(dimensions, resolutionsAniso);
		parserAniso.setDownsamplingPolicy(DOWNSAMPLE_POLICY.Aggressive);

		assertArray2dEquals(
				new int[][]{{128, 128, 1, 32, 32}, {64, 64, 1, 32, 32}},
				parserAniso.parse("128"));
	}

	@Test
	public void testDownsampledBlockParsingIsotropy() {

		final long[] dimensions = new long[] {128,128,3,64,32};
		final double[] resolutions = new double[] {1, 1, 1, 4, 0.1};
		final DownsampledBlockParser parser = new DownsampledBlockParser(dimensions, resolutions);
		parser.setDownsamplingPolicy(DOWNSAMPLE_POLICY.Aggressive);

		// here the XY dimensions are not downsampled for the second level due to the resolution.
		// the block size for Z at level two is reduced so that the blocks are isotropically sized
		assertArray2dEquals(
				new int[][]{{64, 64, 3, 64, 32}, {64, 64, 3, 32, 32}},
				parser.parse("64,64,3,64,64"));

		// the Z dimension is 4 times smaller due to resolution
		assertArray2dEquals(
				new int[][]{{64, 64, 1, 16, 32}, {64, 64, 1, 16, 32}, {32, 32, 1, 16, 32}},
				parser.parse("64"));
	}

	@Test
	public void testMitosis() {

		DownsampledBlockParser p = new DownsampledBlockParser(
				new long[]{171, 196, 2, 5, 51},
				new double[]{0.0885000, 0.0885000, 1, 1, 0.14});

		int[][] blks = p.parse("64");
		assertArray2dEquals(
				new int[][]{{64, 64, 1, 5, 51}, {64, 64, 1, 2, 51}, {42, 49, 1, 1, 51}},
				blks);
	}

	public static void assertArray2dEquals(int[][] expected, int[][] result) {

		assertEquals(expected.length, result.length);
		for (int i = 0; i < expected.length; i++)
			assertArrayEquals(expected[i], result[i]);
	}

}
