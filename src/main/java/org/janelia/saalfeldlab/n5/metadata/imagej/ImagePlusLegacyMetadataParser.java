package org.janelia.saalfeldlab.n5.metadata.imagej;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataParser;
import org.janelia.saalfeldlab.n5.universe.metadata.N5MetadataWriter;

import ij.ImagePlus;
import ij.measure.Calibration;

public class ImagePlusLegacyMetadataParser implements N5MetadataParser<N5ImagePlusMetadata>, 
	N5MetadataWriter<N5ImagePlusMetadata>, ImageplusMetadata<N5ImagePlusMetadata> 
{

	public static final String titleKey = "title";
	public static final String fpsKey = "fps";
	public static final String frameIntervalKey = "frameInterval";
	public static final String pixelWidthKey = "pixelWidth";
	public static final String pixelHeightKey = "pixelHeight";
	public static final String pixelDepthKey = "pixelDepth";
	public static final String pixelUnitKey = "pixelUnit";
	public static final String xOriginKey = "xOrigin";
	public static final String yOriginKey = "yOrigin";
	public static final String zOriginKey = "zOrigin";

	public static final String numChannelsKey = "numChannels";
	public static final String numSlicesKey = "numSlices";
	public static final String numFramesKey = "numFrames";

	public static final String typeKey = "ImagePlusType";

	public static final String imagePropertiesKey = "imageProperties";

	public static final String downsamplingFactorsKey = "downsamplingFactors";
	
	@Override
	public void writeMetadata(final N5ImagePlusMetadata t, final N5Writer n5, final String dataset) throws Exception {

		if (!n5.datasetExists(dataset))
			throw new Exception("Can't write into " + dataset + ".  Must be a dataset.");

		HashMap<String, Object> attrs = new HashMap<>();
		attrs.put(titleKey, t.name);

		attrs.put(fpsKey, t.fps);
		attrs.put(frameIntervalKey, t.frameInterval);
		attrs.put(pixelWidthKey, t.pixelWidth);
		attrs.put(pixelHeightKey, t.pixelHeight);
		attrs.put(pixelDepthKey, t.pixelDepth);
		attrs.put(pixelUnitKey, t.unit);

		attrs.put(xOriginKey, t.xOrigin);
		attrs.put(yOriginKey, t.yOrigin);
		attrs.put(zOriginKey, t.zOrigin);

		attrs.put(numChannelsKey, t.numChannels);
		attrs.put(numSlicesKey, t.numSlices);
		attrs.put(numFramesKey, t.numFrames);

		attrs.put(typeKey, t.type);

		attrs.put( imagePropertiesKey, t.properties );

//		if (t.properties != null) {
//			for (final Object k : t.properties.keySet()) {
//				try {
//					attrs.put(k.toString(), t.properties.get(k).toString());
//				} catch (final Exception e) {
//				}
//			}
//		}

		n5.setAttributes(dataset, attrs);
	}

	@Override
	public void writeMetadata(final N5ImagePlusMetadata t, final ImagePlus ip) throws IOException {
		ip.setTitle(t.name);

		final Calibration cal = ip.getCalibration();
		cal.fps = t.fps;
		cal.frameInterval = t.frameInterval;
		cal.pixelWidth = t.pixelWidth;
		cal.pixelHeight = t.pixelHeight;
		cal.pixelDepth = t.pixelDepth;
		cal.setUnit(t.unit);

		cal.xOrigin = t.xOrigin;
		cal.yOrigin = t.yOrigin;
		cal.zOrigin = t.zOrigin;
		ip.setCalibration( cal );

		ip.setDimensions(t.numChannels, t.numSlices, t.numFrames);

		final Properties props = ip.getProperties();
		if (t.properties != null) {
			for (final String k : t.properties.keySet()) {
				try {
					props.put(k, t.properties.get(k));
				} catch (final Exception e) {
				}
			}
		}
	}
	
	@Override
	public N5ImagePlusMetadata readMetadata(final ImagePlus ip) throws IOException {

		String name;
		if (ip.getTitle() == null)
			name = "ImagePlus";
		else
			name = ip.getTitle();

		final HashMap<String,Object> properties = new HashMap<>();
		final Properties props = ip.getProperties();
		if (props != null) {
			for (final Object k : props.keySet()) {
				try {
					properties.put(k.toString(), props.get(k));
				} catch (final Exception e) {
				}
			}
		}

		final Calibration cal = ip.getCalibration();
		final N5ImagePlusMetadata t = new N5ImagePlusMetadata("", null, name, cal.fps, cal.frameInterval, cal.getUnit(),
				cal.pixelWidth, cal.pixelHeight, cal.pixelDepth, cal.xOrigin, cal.yOrigin, cal.zOrigin, ip.getNChannels(),
				ip.getNSlices(), ip.getNFrames(), ip.getType(), properties );

		return t;
	}

	@Override
	public Optional<N5ImagePlusMetadata> parseMetadata(N5Reader n5, N5TreeNode node) {

		try {
			final String dataset = node.getPath();
			final DatasetAttributes attributes = n5.getDatasetAttributes(dataset);
			if (attributes == null)
				return Optional.empty();

			final String name = n5.getAttribute(dataset, titleKey, String.class);

			final Double pixelWidth = n5.getAttribute(dataset, pixelWidthKey, Double.class);
			final Double pixelHeight = n5.getAttribute(dataset, pixelHeightKey, Double.class);
			final Double pixelDepth = n5.getAttribute(dataset, pixelDepthKey, Double.class);
			final String unit = n5.getAttribute(dataset, pixelUnitKey, String.class);

			final Double xOrigin = n5.getAttribute(dataset, xOriginKey, Double.class);
			final Double yOrigin = n5.getAttribute(dataset, yOriginKey, Double.class);
			final Double zOrigin = n5.getAttribute(dataset, zOriginKey, Double.class);

			final Integer numChannels = n5.getAttribute(dataset, numChannelsKey, Integer.class);
			final Integer numSlices = n5.getAttribute(dataset, numSlicesKey, Integer.class);
			final Integer numFrames = n5.getAttribute(dataset, numFramesKey, Integer.class);

			final Double fps = n5.getAttribute(dataset, fpsKey, Double.class);
			final Double frameInterval = n5.getAttribute(dataset, fpsKey, Double.class);

			final Integer type = n5.getAttribute(dataset, typeKey, Integer.class);

			Map<String,Object> properties = n5.getAttribute(dataset, imagePropertiesKey, HashMap.class);
			if( properties == null )
				properties = new HashMap<>();

			final N5ImagePlusMetadata meta = new N5ImagePlusMetadata(dataset, attributes, name, fps, frameInterval,
					unit, pixelWidth, pixelHeight, pixelDepth, xOrigin, yOrigin, zOrigin, numChannels, numSlices,
					numFrames, type, properties);

			return Optional.of(meta);

		} catch (IOException e) {
		}
		
		return Optional.empty();
	}

}
