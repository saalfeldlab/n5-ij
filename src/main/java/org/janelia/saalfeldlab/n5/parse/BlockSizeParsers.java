package org.janelia.saalfeldlab.n5.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.N5Exception;

public class BlockSizeParsers {

	public enum DOWNSAMPLE_POLICY {
		Aggressive, Conservative
	};

	public static class BlockSizeParser {

		private int numDimensions;

		private long[] dimensions;

		// if this array is not empty, the indexes
		// contained here should be auto-filled to '1'
		// only if the size for that dimension is not explicitly specified
		private final Set<Integer> singletonDimensionSet; 
		
		// optional resolution information for isotropic block sizing
		private double[] resolution;
		
		// which dimensions are downsampled and should have isotropic sizing
		private boolean[] applyDownsampling;

		public BlockSizeParser(long[] dimensions) {

			this(dimensions, new int[]{2, 4}); // singleton C and T
		}

		public BlockSizeParser(long[] dimensions, int[] singletonDimensions) {

			this(dimensions, singletonDimensions, null, null);
		}

		public BlockSizeParser(long[] dimensions, int[] singletonDimensions, double[] resolution, boolean[] applyDownsampling) {

			this.numDimensions = dimensions.length;
			this.dimensions = dimensions;
	
			this.singletonDimensionSet = singletonDimensions == null ? 
				IntStream.of(2).boxed().collect(Collectors.toSet()) : 
				Arrays.stream(singletonDimensions).boxed().collect(Collectors.toSet());

			this.resolution = resolution;
			this.applyDownsampling = applyDownsampling;
		}

		private static final int UNSET_VALUE = -1;

		public void setDimensions(final long[] dimensions) {

			this.dimensions = dimensions;
		}

		public int[] parse(String blockSizeString) {

			if (blockSizeString == null) {
				throw new IllegalArgumentException("Block size string cannot be null");
			}
			else if (blockSizeString.isEmpty()) {
				throw new IllegalArgumentException("Block size string cannot be empty");
			}

			String[] parts = blockSizeString.split(",", -1);
			int[] result = new int[numDimensions];
			Arrays.fill(result, UNSET_VALUE);

			// Parse explicit values from input
			parseExplicitValues(parts, result);
			return parse(result);
		}
		
		public int[] parse(int[] result) {

			// Fill in missing values using appropriate defaults
			fillMissingValues(result, firstValidValue(result));
			clamp(result);

			return result;
		}

		private int firstValidValue(int[] results) {

			for (int i = 0; i < results.length; i++) {
				if (results[i] != UNSET_VALUE)
					return results[i];
			}
			return UNSET_VALUE;
		}

		private void parseExplicitValues(String[] parts, int[] result) {

			for (int i = 0; i < Math.min(numDimensions, parts.length); i++) {
				String part = parts[i].trim();
				if (part.isEmpty()) {
					continue;
				}

				try {
					int value = Integer.parseInt(part);
					if (value <= 0) {
						throw new IllegalArgumentException(
								"Block size values must be positive, got: " + value);
					}
					result[i] = value;
				} catch (NumberFormatException e) {
					// Non-numeric value, leave as UNSET_VALUE
				}
			}
		}

		private void fillMissingValues(final int[] result, int fillValue) {

			// If using isotropic sizing, find a reference dimension with known size
			Double targetPhysicalSize = null;
			if (resolution != null && applyDownsampling != null && fillValue != UNSET_VALUE) {
				// Find first dimension that has a value and should use isotropic sizing
				for (int i = 0; i < numDimensions; i++) {
					if (result[i] != UNSET_VALUE && applyDownsampling[i]) {
						targetPhysicalSize = result[i] * resolution[i];
						break;
					}
				}
				// If no explicit value found but we have a fill value, use it as reference
				if (targetPhysicalSize == null) {
					// Find first dimension that should use isotropic sizing
					for (int i = 0; i < numDimensions; i++) {
						if (applyDownsampling[i]) {
							targetPhysicalSize = fillValue * resolution[i];
							break;
						}
					}
				}
			}

			for (int i = 0; i < numDimensions; i++) {
				if (result[i] == UNSET_VALUE) {
					result[i] = getDefaultValue(i, fillValue, targetPhysicalSize);
				} else {
					// Update fillValue to the last valid value we've seen
					fillValue = result[i];
					// Update target physical size if this dimension uses isotropic sizing
					if (targetPhysicalSize != null && applyDownsampling != null && applyDownsampling[i]) {
						targetPhysicalSize = result[i] * resolution[i];
					}
				}
			}
		}

		private void clamp(final int[] result) {

			for (int i = 0; i < result.length; i++) {
				if (result[i] > dimensions[i])
					result[i] = (int)dimensions[i];
			}
		}

		private int getDefaultValue(int dimension, int fillValue, Double targetPhysicalSize) {

			if (singletonDimensionSet.contains(dimension)) {
				return 1;
			} else if (targetPhysicalSize != null && resolution != null && applyDownsampling != null && applyDownsampling[dimension]) {
				// Calculate isotropic block size
				int isotropicSize = (int) Math.round(targetPhysicalSize / resolution[dimension]);
				return Math.max(1, isotropicSize);
			} else if (fillValue != UNSET_VALUE) {
				return fillValue;
			} else {
				return 1;
			}
		}
	}
	
	public static class DownsampledBlockParser {

		private final long[] dimensions;

		private final int[] downsamplingFactors;

		// must be the same length as dimensions
		// if an entry is false, that dimension will not be downsampled
		// (usually channels and time)
		private boolean[] applyDownsampling;

		private DOWNSAMPLE_POLICY policy = DOWNSAMPLE_POLICY.Aggressive;

		private final BlockSizeParser blkParser;

		private long[] currentDimensions;

		private int[] currentBlockSize;

		public DownsampledBlockParser(BlockSizeParser blockSizeParser) {

			this.dimensions = blockSizeParser.dimensions;
			this.applyDownsampling = 
					blockSizeParser.applyDownsampling != null ?
					blockSizeParser.applyDownsampling :
					trueArray(this.dimensions.length);

			// Create BlockSizeParser with resolution info for isotropic sizing
			// Use applyDownsampling to determine which dimensions should have isotropic sizing
			blkParser = blockSizeParser;

			// init downsampling factors
			downsamplingFactors = new int[dimensions.length];
			for (int i = 0; i < dimensions.length; i++)
				if (applyDownsampling[i])
					downsamplingFactors[i] = 2;
				else
					downsamplingFactors[i] = 1;
		}

		public DownsampledBlockParser(long[] dimensions, double[] resolution, boolean[] applyDownsampling) {

			this(new BlockSizeParser(dimensions, new int[]{2}, resolution, applyDownsampling));
		}

		public DownsampledBlockParser(long[] dimensions, double[] resolution ) {

			// assumes XYCZT dimension ordering
			// do not downsample C or T
			this(dimensions, resolution, new boolean[]{true, true, false, true, false});
		}

		private static boolean[] trueArray(int n) {
			final boolean[] out = new boolean[n];
			Arrays.fill(out, true);
			return out;
		}
		
		public void setDownsamplingPolicy(DOWNSAMPLE_POLICY policy) {

			this.policy = policy;
		}

		public int[][] parse(String blockSizeString) {

			ArrayList<int[]> blockSizes = new ArrayList<>();
			currentBlockSize = new int[dimensions.length];
			currentDimensions = Arrays.copyOf(dimensions, dimensions.length);

			String[] blockSizeParams = blockSizeString.trim().split(";");

			int i = 0;
			int max = 20;
			while( i < max ) {

				parseBlockSize(i, blockSizeParams);
				blockSizes.add(Arrays.copyOf(currentBlockSize, currentBlockSize.length));
				if (stop()) {
					break;
				}

				updateDimensions(); // downsample
				i++;
			}

			final int[][] result = new int[blockSizes.size()][dimensions.length];
			for(i = 0; i < blockSizes.size(); i++)
				result[i] = blockSizes.get(i);
			
			return result;
		}

		private void parseBlockSize(int index, String[] blockSizeParams) {

			blkParser.setDimensions(currentDimensions);

			// get the starting point from the specified parameters if they exist,
			// otherwise use the previous block size
			int[] blockTemplate;
			if (blockSizeParams.length > index)
				blockTemplate = blkParser.parse(blockSizeParams[index]);
			else {
				blockTemplate = blkParser.parse(currentBlockSize);
			}

			if (blockTemplate.length == 0)
				throw new N5Exception("Failed to parse block. Parameters must have length > 0.");

			for (int i = 0; i < currentBlockSize.length; i++) {
				currentBlockSize[i] = blockTemplate[i];
			}
		}

		private boolean stop() {

			if (policy == DOWNSAMPLE_POLICY.Conservative) {
				// Stop if ANY dimension that downsampling is applied to
				// is less than or equal to the chunk size in that dimension
				for (int i = 0; i < currentDimensions.length; i++) {
					if (applyDownsampling[i] && currentDimensions[i] <= currentBlockSize[i]) {
						return true;
					}
				}
				return false;
			} else { // Aggressive
				// Stop if ALL dimensions that downsampling is applied to
				// are less than or equal to the chunk size in that dimension
				for (int i = 0; i < currentDimensions.length; i++) {
					if (applyDownsampling[i] && currentDimensions[i] > currentBlockSize[i]) {
						return false;
					}
				}
				return true;
			}
		}

		private void updateDimensions() {

			for (int i = 0; i < currentDimensions.length; i++)
				currentDimensions[i] = Math.max(1, currentDimensions[i] / downsamplingFactors[i]);
		}

	}

	public static class DownsampledBlockParserClaude {

		private long[] dimensions;

		// must be the same length as dimensions
		private double[] resolution;

		// must be the same length as dimensions
		// if an entry is false, that dimension will not be downsampled 
		// (usually channels and time)
		private boolean[] applyDownsampling;

		private DOWNSAMPLE_POLICY policy = DOWNSAMPLE_POLICY.Aggressive;

		private final BlockSizeParser blkParser;

		public DownsampledBlockParserClaude(long[] dimensions, double[] resolution, boolean[] applyDownsampling) {

			this.dimensions = dimensions;
			this.resolution = resolution;
			this.applyDownsampling = applyDownsampling;
			
			// Create BlockSizeParser with resolution info for isotropic sizing
			// Use applyDownsampling to determine which dimensions should have isotropic sizing
			blkParser = new BlockSizeParser(dimensions, new int[]{2}, resolution, applyDownsampling);
		}

		public DownsampledBlockParserClaude(long[] dimensions, double[] resolution ) {

			// assumes XYCZT dimension ordering
			// do not downsample C or T
			this(dimensions, resolution, new boolean[]{true, true, false, true, false});
		}

		public void setDownsamplingPolicy(DOWNSAMPLE_POLICY policy) {

			this.policy = policy;
		}

		public int[][] parse(String blockSizeString) {

			// Parse block sizes for each scale level
			// Format: "64,64;32,32;16,16;" where each semicolon-separated part
			// is a scale level
			if (blockSizeString == null || blockSizeString.isEmpty()) {
				throw new IllegalArgumentException("Block size string cannot be null or empty");
			}

			// Split by semicolon to get scale levels
			String[] scaleLevels = blockSizeString.split(";");

			// Parse the first level to get base block sizes
			int[] baseBlockSizes = null;
			if (scaleLevels.length > 0 && !scaleLevels[0].trim().isEmpty()) {
				baseBlockSizes = parseScaleLevel(scaleLevels[0].trim(), 0);
			} else {
				// Generate default base block sizes
				baseBlockSizes = generateScaleLevel(0, null);
			}

			// Calculate how many scale levels we need based on downsampling
			// policy and base block sizes
			int numScaleLevels = calculateNumScaleLevels(baseBlockSizes);

			// Initialize result array
			int[][] result = new int[numScaleLevels][];

			// Set the first level
			result[0] = baseBlockSizes;

			// Parse remaining provided scale levels
			int numProvided = Math.min(scaleLevels.length, numScaleLevels);
			for (int s = 1; s < numProvided; s++) {
				String scaleString = scaleLevels[s].trim();
				if (!scaleString.isEmpty()) {
					result[s] = parseScaleLevel(scaleString, s);
				}
			}

			// Fill in remaining scale levels if needed
			for (int s = numProvided; s < numScaleLevels; s++) {
				result[s] = generateScaleLevel(s, baseBlockSizes);
			}

			return result;
		}

		private int[] parseScaleLevel(String scaleString, int scaleIndex) {

			// Use BlockSizeParser to parse individual scale level
			int[] parsedBlockSize = blkParser.parse(scaleString);

			// Adjust block sizes based on current scale dimensions
			long[] scaleDimensions = calculateScaleDimensions(scaleIndex);
			for (int i = 0; i < parsedBlockSize.length; i++) {
				if (parsedBlockSize[i] > scaleDimensions[i]) {
					parsedBlockSize[i] = (int)scaleDimensions[i];
				}
			}

			return parsedBlockSize;
		}

		private int[] generateScaleLevel(int scaleIndex, int[] templateBlockSizes) {
			// Generate block sizes for scales not explicitly provided
			// Use the provided template or default to 64
			int[] blockSize = new int[dimensions.length];
			long[] scaleDimensions = calculateScaleDimensions(scaleIndex);
			
			// Use template if provided, otherwise default to 64
			if (templateBlockSizes != null) {
				for (int i = 0; i < dimensions.length; i++) {
					blockSize[i] = (int) Math.min(templateBlockSizes[i], scaleDimensions[i]);
				}
			} else {
				// Default block size
				int defaultSize = 64;
				for (int i = 0; i < dimensions.length; i++) {
					blockSize[i] = (int) Math.min(defaultSize, scaleDimensions[i]);
				}
			}
			
			return blockSize;
		}
		
		private long[] calculateScaleDimensions(int scaleIndex) {
			// Calculate dimensions at a given scale level
			long[] currentDims = Arrays.copyOf(dimensions, dimensions.length);
			long[] currentDownsampleFactors = new long[dimensions.length];
			Arrays.fill(currentDownsampleFactors, 1);
			
			for (int s = 0; s < scaleIndex; s++) {
				long[] relativeFactors = calculateRelativeDownsampleFactors(currentDims, currentDownsampleFactors);
				
				// Apply downsampling
				for (int i = 0; i < currentDims.length; i++) {
					if (relativeFactors[i] > 1) {
						currentDims[i] = (long) Math.ceil(currentDims[i] / 2.0);
						currentDownsampleFactors[i] *= 2;
					}
				}
			}
			
			return currentDims;
		}
		
		private int calculateNumScaleLevels(int[] blockSizes) {
			// Calculate the number of scale levels based on downsampling policy
			long[] currentDims = Arrays.copyOf(dimensions, dimensions.length);
			long[] currentDownsampleFactors = new long[dimensions.length];
			Arrays.fill(currentDownsampleFactors, 1);

			int numLevels = 1; // Always have at least the base level

			// Check if we should stop before creating any additional levels
			boolean shouldStop = false;
			if (policy == DOWNSAMPLE_POLICY.Conservative) {
				// Stop if any dimension that downsampling is applied to 
				// is less than or equal to the chunk size in that dimension
				for (int i = 0; i < currentDims.length; i++) {
					if (applyDownsampling[i] && currentDims[i] <= blockSizes[i]) {
						shouldStop = true;
						break;
					}
				}
			} else { // Aggressive
				// Stop if all dimensions that downsampling is applied to
				// are less than or equal to the chunk size in that dimension
				shouldStop = true;
				for (int i = 0; i < currentDims.length; i++) {
					if (applyDownsampling[i] && currentDims[i] > blockSizes[i]) {
						shouldStop = false;
						break;
					}
				}
			}
			
			if (shouldStop) {
				return numLevels;
			}

			// Maximum reasonable number of levels
			final int maxLevels = 20;

			for (int s = 1; s < maxLevels; s++) {
				// Calculate downsampling factors for this level
				long[] relativeFactors = calculateRelativeDownsampleFactors(currentDims, currentDownsampleFactors);
				
				// Apply downsampling to get next dimensions
				boolean anyDownsampled = false;
				for (int i = 0; i < currentDims.length; i++) {
					if (relativeFactors[i] > 1) {
						currentDims[i] = (long) Math.ceil(currentDims[i] / 2.0);
						currentDownsampleFactors[i] *= 2;
						anyDownsampled = true;
					}
				}

				// If nothing was downsampled, we're done
				if (!anyDownsampled) {
					break;
				}

				// We've successfully created a new level
				numLevels++;

				// Check stopping criteria based on policy for the NEXT potential level
				shouldStop = false;
				if (policy == DOWNSAMPLE_POLICY.Conservative) {
					// Stop if any dimension that downsampling is applied to 
					// is less than or equal to the chunk size in that dimension
					for (int i = 0; i < currentDims.length; i++) {
						if (applyDownsampling[i] && currentDims[i] <= blockSizes[i]) {
							shouldStop = true;
							break;
						}
					}
				} else { // Aggressive
					// Stop if all dimensions that downsampling is applied to
					// are less than or equal to the chunk size in that dimension
					shouldStop = true;
					for (int i = 0; i < currentDims.length; i++) {
						if (applyDownsampling[i] && currentDims[i] > blockSizes[i]) {
							shouldStop = false;
							break;
						}
					}
				}

				if (shouldStop) {
					break;
				}
			}

			return numLevels;
		}

		private long[] calculateRelativeDownsampleFactors(long[] currentDims, long[] absoluteDownsampleFactors) {

			// Calculate which dimensions to downsample based on isotropy
			// preservation
			long[] factors = new long[dimensions.length];
			Arrays.fill(factors, 1);

			// Find minimum spatial resolution
			double minSpatialRes = Double.POSITIVE_INFINITY;
			for (int i = 0; i < resolution.length; i++) {
				if (applyDownsampling[i]) {
					double currentRes = resolution[i] * absoluteDownsampleFactors[i];
					if (currentRes < minSpatialRes) {
						minSpatialRes = currentRes;
					}
				}
			}

			// Determine which dimensions to downsample
			for (int i = 0; i < dimensions.length; i++) {
				if (applyDownsampling[i] && currentDims[i] > 1) {
					// Check if downsampling preserves isotropy
					double currentRes = resolution[i] * absoluteDownsampleFactors[i];
					if (minSpatialRes * 2 >= currentRes) {
						factors[i] = 2;
					}
				}
			}

			return factors;
		}
		
	}

}
