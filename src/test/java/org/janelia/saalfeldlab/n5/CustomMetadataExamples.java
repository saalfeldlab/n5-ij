package org.janelia.saalfeldlab.n5;

import org.janelia.saalfeldlab.n5.metadata.imagej.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.imagej.MetadataTemplateMapper;

import ij.IJ;
import ij.ImagePlus;

public class CustomMetadataExamples
{

	public static void main( final String[] args ) throws Exception
	{
		final ImagePlus mitosisImage = IJ.openImage( "/home/john/tmp/mitosis.tif" );
		System.out.println( mitosisImage );

		System.out.println( "resolution only mapper: " );
		runWith( mitosisImage, MetadataTemplateMapper.RESOLUTION_ONLY_MAPPER );

		System.out.println( "COSEM mapper: " );
		runWith( mitosisImage, MetadataTemplateMapper.COSEM_MAPPER );

	}

	public static void runWith( final ImagePlus imp, final String translationSpec ) throws Exception
	{
		System.out.println( " " );
		System.out.println( translationSpec );
		final ImagePlusMetadataTemplate metaTemplate = ImagePlusMetadataTemplate.readMetadataStatic( imp );
		final MetadataTemplateMapper mapper = new MetadataTemplateMapper( translationSpec );
		System.out.println( " " );

	}

	public static final String perAxisMapper = "{\n" +
			"\t\"x\": [.xResolution, .yResolution, .zResolution],\n" +
			"\t\"translate\": [.xOrigin, .yOrigin, .zOrigin],\n" +
			"\t\"axes\": [.axis0, .axis1, .axis2, .axis3, .axis4],\n" +
			"\t\"units\": [.xUnit, .yUnit, .zUnit]\n" +
			"\t}\n" +
			"}";


}
