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
package org.janelia.saalfeldlab.n5.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class N5StructureTemplateRecursive
{
	public String name;
	public String attributes;
	public Map<String,N5StructureTemplateRecursive> children;

	public N5StructureTemplateRecursive(
			final String name,
			final String attributes,
			final Map<String,N5StructureTemplateRecursive> children )
	{
		this.name = name;
		this.attributes = attributes;
		this.children = children;
	}

	public static void main( final String[] args ) throws IOException
	{
		final String path = "/home/john/dev/json/jq_Examples/sample_multiscale.n5/n5Structure.json";
		final String jsonString = new String(Files.readAllBytes(Paths.get( path )));

//		N5StructureTemplate n5Structure = new Gson().fromJson( jsonString, N5StructureTemplate.class );
//		System.out.println( n5Structure );

		final MultiscaleDatasetsTemplate.MultiscaleDataset[] list =
				new MultiscaleDatasetsTemplate.MultiscaleDataset[] {
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s0", new double[] { 1,1,1 }),
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s1", new double[] { 2,2,2 }),
					new MultiscaleDatasetsTemplate.MultiscaleDataset( "/volumes/raw/s2", new double[] { 4,4,4 }),
				};

		final HashMap msmap = new HashMap<>();
		msmap.put( "raw", list );

		final MultiscaleDatasetsTemplate mst = new MultiscaleDatasetsTemplate( msmap );
		System.out.println( new Gson().toJson( mst ));

		System.out.println("done");
	}
}