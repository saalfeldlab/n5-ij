/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5;

import org.janelia.saalfeldlab.n5.metadata.ImagePlusMetadataTemplate;
import org.janelia.saalfeldlab.n5.metadata.MetadataTemplateMapper;

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
