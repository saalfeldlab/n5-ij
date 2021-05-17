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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.thisptr.jackson.jq.BuiltinFunctionLoader;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Strings;
import net.thisptr.jackson.jq.path.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MetadataTemplateMapper { //implements N5MetadataWriter<ImagePlusMetadataTemplate> {

  //  public ImagePlusMetadataTemplate template;

  private final Scope scope;

  private final String query;

  private Gson gson;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public MetadataTemplateMapper(final Scope scope, final String query) {

	this.scope = scope;
	this.query = query;
	gson = new GsonBuilder().create();
  }

	public MetadataTemplateMapper( final String query )
	{
		this( buildRootScope(), query );
	}

	public String map( final String input ) throws IOException
	{
		final JsonQuery q = JsonQuery.compile( query, Versions.JQ_1_5 );
		final JsonNode in = MAPPER.readTree( input );

		final List< JsonNode > out = new ArrayList<>();
		q.apply( scope, in, out::add );

		final StringBuffer stringOutput = new StringBuffer();
		for ( final JsonNode node : out )
		{
			stringOutput.append( node.toString() + "\n" );
		}

		return stringOutput.toString();
	}

	public Object computeToMap( final String json ) throws IOException
	{
		return gson.fromJson( map( json ), Object.class );
	}

	public JsonElement computeToJson( final String input ) throws IOException
	{
		return new JsonParser().parse( map( input ));
	}

	public static Scope buildRootScope()
	{
		// First of all, you have to prepare a Scope which s a container of built-in/user-defined functions and variables.
		final Scope rootScope = Scope.newEmptyScope();

		// Use BuiltinFunctionLoader to load built-in functions from the classpath.
		BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_5, rootScope);

		// You can also define a custom function. E.g.
		rootScope.addFunction("repeat", 1, new Function() {
			@Override
			public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path path, final PathOutput output, final Version version) throws JsonQueryException {
				args.get(0).apply(scope, in, (time) -> {
					output.emit(new TextNode(Strings.repeat(in.asText(), time.asInt())), null);
				});
			}
		});
		return rootScope;
	}

	public static final String RESOLUTION_ONLY_MAPPER =
			"{\n\"resolution\" : [.xResolution, .yResolution, .zResolution ]\n}";

  public static final String COSEM_MAPPER = "{\n\t\"transform\":\n" +
		  "\t{\n" +
		  "\t\"scale\": [.xResolution, .yResolution, .zResolution],\n" +
		  "\t\"translate\": [.xOrigin, .yOrigin, .zOrigin],\n" +
		  "\t\"axes\": [.axis0, .axis1, .axis2, .axis3, .axis4],\n" +
		  "\t\"units\": [.xUnit, .yUnit, .zUnit]\n" +
		  "\t}\n" +
		  "}";

  //	@Override
  //	public void writeMetadata( final ImagePlusMetadataTemplate t, final N5Writer n5, final String dataset ) throws Exception
  //	{
  //		final Map< String, ? > map = ( Map< String, ? > ) computeToMap( gson.toJson( t ));
  //		n5.setAttributes( dataset, map );
  //	}
  //
  //	public String toJsonString( final ImagePlusMetadataTemplate t ) throws Exception
  //	{
  //		return computeToJson( gson.toJson( t )).toString();
  //	}

}
