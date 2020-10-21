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

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.imglib2.realtransform.AffineTransform3D;

public class AffineTransform3DJsonAdapter implements JsonSerializer< AffineTransform3D >, JsonDeserializer< AffineTransform3D >
{
	@Override
	public JsonElement serialize( final AffineTransform3D src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonArray jsonMatrixArray = new JsonArray();
		for ( int row = 0; row < src.numDimensions(); ++row )
		{
			final JsonArray jsonRowArray = new JsonArray();
			for ( int col = 0; col < src.numDimensions() + 1; ++col )
				jsonRowArray.add( src.get( row, col ) );
			jsonMatrixArray.add( jsonRowArray );
		}
		return jsonMatrixArray;
	}

	@Override
	public AffineTransform3D deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
	{
		final AffineTransform3D affineTransform = new AffineTransform3D();
		final JsonArray jsonMatrixArray = json.getAsJsonArray();
		for ( int row = 0; row < jsonMatrixArray.size(); ++row )
		{
			final JsonArray jsonRowArray = jsonMatrixArray.get( row ).getAsJsonArray();
			for ( int col = 0; col < jsonRowArray.size(); ++col )
				affineTransform.set( jsonRowArray.get( col ).getAsDouble(), row, col );
		}
		return affineTransform;
	}
}
