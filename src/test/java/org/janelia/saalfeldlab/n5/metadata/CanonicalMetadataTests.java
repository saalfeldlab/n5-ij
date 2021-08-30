package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.metadata.canonical.Axis;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalDatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.SpatialMetadataCanonical;
import org.janelia.saalfeldlab.n5.metadata.canonical.TranslatedTreeMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.transforms.AffineSpatialTransform;

import com.google.gson.Gson;

public class CanonicalMetadataTests {

	public static void main(String[] args) {

		axisTest();

	}

	public static void axisTest() {
//		DatasetAttributes attributes = new DatasetAttributes(new long[] { 0, 0, 0, 0, 0 }, 5,
//				DataType.FLOAT32, new GzipCompression());
		DatasetAttributes attributes = new DatasetAttributes(new long[] { 0, 0, 0 }, new int[] { 5, 5, 5 },
				DataType.FLOAT32, new GzipCompression());

		Axis[] axes = new Axis[3];
		axes[0] = new Axis("space", "x", "mm");
		axes[1] = new Axis("space", "y", "mm");
		axes[2] = new Axis("space", "z", "mm");

		AffineSpatialTransform affine = new AffineSpatialTransform(
				new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 });
		SpatialMetadataCanonical spatial = new SpatialMetadataCanonical("", affine, "mm", axes);

		CanonicalDatasetMetadata meta = new CanonicalDatasetMetadata("", spatial, attributes);
		Gson gson = TranslatedTreeMetadataParser.buildGson();

		System.out.println(gson.toJson(meta));
		System.out.println("");
		System.out.println(gson.toJson(spatial));
		System.out.println("");
		System.out.println(gson.toJson(affine));

	}

}
