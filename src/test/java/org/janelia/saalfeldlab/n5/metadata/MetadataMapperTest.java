package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.util.Map;

import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata.CosemTransform;
import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.imagej.MetadataTemplateMapper;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import ij.ImagePlus;
import ij.gui.NewImage;

public class MetadataMapperTest {

	final static Gson gson = new Gson();

	@Test
	public void testMetadataMapping()
	{
		final ImagePlus imp = NewImage.createImage("test", 8, 6, 1, 16, NewImage.FILL_NOISE);
		imp.getCalibration().pixelWidth = 2.0;
		imp.getCalibration().pixelHeight = 3.0;
		imp.getCalibration().pixelDepth = 4.0;
		final ImagePlusMetadataTemplate meta = new ImagePlusMetadataTemplate("", imp );

		final MetadataTemplateMapper resMapper = new MetadataTemplateMapper( MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );
		final MetadataTemplateMapper cosemMapper = new MetadataTemplateMapper( MetadataTemplateMapper.COSEM_MAPPER );

		try {
			String resJson = resMapper.mapToJson( meta );
			Assert.assertTrue("resolution only mapper key", resJson.contains("resolution"));
			Assert.assertTrue("resolution only mapper value", resJson.contains("2.0,3.0,4.0"));

			String cosemJson = cosemMapper.mapToJson( meta );
			System.out.println( cosemJson );
			Assert.assertTrue("cosem only mapper keys", 
					cosemJson.contains("scale") &&
					cosemJson.contains("translate") &&
					cosemJson.contains("axes") &&
					cosemJson.contains("units"));

			final Map baseMap = gson.fromJson(cosemJson, Map.class);
			final String transformJson = gson.toJson(baseMap.get("transform"));
			final CosemTransform cosemTransform = gson.fromJson(transformJson, CosemTransform.class);

			Assert.assertNotNull("cosem mapper get transform", cosemTransform);
			Assert.assertArrayEquals("cosem mapper scale", new double[]{4, 3, 2}, cosemTransform.scale, 1e-9);
			Assert.assertArrayEquals("cosem mapper translate", new double[]{0, 0, 0}, cosemTransform.translate, 1e-9);
			Assert.assertArrayEquals("cosem mapper axes", new String[]{"z", "y", "x"}, cosemTransform.axes);

		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

}
