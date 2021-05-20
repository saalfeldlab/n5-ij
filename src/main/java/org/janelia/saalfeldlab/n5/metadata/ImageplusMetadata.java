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

import ij.ImagePlus;

import java.io.IOException;

/**
 * A interface for reading and writing metadata to an {@link ImagePlus}.
 *
 * @param <T>
 *            the metadata type
 * @author John Bogovic
 */
public interface ImageplusMetadata<T extends N5DatasetMetadata> {

	/**
	 * Modify the metadata of the {@link ImagePlus} according to the given
	 * metadata.
	 *
	 * @param t
	 *            metadata
	 * @param ip
	 *            ImagePlus
	 * @throws IOException
	 *             the io exception
	 */
	public void writeMetadata(T t, ImagePlus ip) throws IOException;

	/**
	 * Create and return a new metadata object from the given {@link ImagePlus}.
	 * 
	 * @param ip
	 *            the ImagePlus
	 * @return
	 * @throws IOException
	 *             the io exception
	 */
	public T readMetadata(ImagePlus ip) throws IOException;
}
