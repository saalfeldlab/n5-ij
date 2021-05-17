///**
// * Copyright (c) 2018--2020, Saalfeld lab
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// * 1. Redistributions of source code must retain the above copyright notice,
// *    this list of conditions and the following disclaimer.
// * 2. Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// * POSSIBILITY OF SUCH DAMAGE.
// */
//package org.janelia.saalfeldlab.n5.metadata;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//
//public class AutoDetectMetadata implements N5GsonMetadataParser<N5DatasetMetadata> {
//
//  private N5MetadataParser<?>[] parsers;
//  private HashMap<String, Class<?>> keysToTypes;
//
//  public AutoDetectMetadata(final N5MetadataParser<?>[] parsers) {
//
//	this.parsers = parsers;
//	keysToTypes = new HashMap<>();
//  }
//
//  public AutoDetectMetadata() {
//
//	this(new N5MetadataParser[]
//			{
//					new N5ImagePlusMetadata(""),
//					new N5CosemMetadata(),
//					new N5SingleScaleMetadata(),
//					new DefaultMetadata("", -1)
//			});
//  }
//
//	@Override
//	public HashMap<String,Class<?>> keysToTypes()
//	{
//		return keysToTypes;
//	}
//
//  @Override
//  public N5DatasetMetadata parseMetadata(final Map<String, Object> metaMap) throws Exception {
//
//	final ArrayList<Exception> elist = new ArrayList<>();
//	for (final N5MetadataParser<?> p : parsers) {
//	  try {
//		final N5DatasetMetadata meta = p.parseMetadata(metaMap);
//		if (meta != null)
//		  return meta;
//	  } catch (final Exception e) {
//		elist.add(e);
//			}
//		}
//
//		for( final Exception e : elist )
//		{
//			e.printStackTrace();
//		}
//		return null;
//	}
//
//}
