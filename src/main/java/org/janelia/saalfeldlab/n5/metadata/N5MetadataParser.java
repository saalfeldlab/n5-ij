/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
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

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5TreeNode;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Interface for reading metadata from N5 containers.
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface N5MetadataParser<T extends N5Metadata> extends BiFunction<N5Reader, N5TreeNode, Optional<T>> {

  static DatasetAttributes parseDatasetAttributes(N5Reader n5, N5TreeNode node) {

	try {
	  final int[] blockSize = n5.getAttribute(node.getPath(), "blockSize", int[].class);
	  final long[] dimensions = n5.getAttribute(node.getPath(), "blockSize", long[].class);
	  final String dataTypeString = n5.getAttribute(node.getPath(), "blockSize", String.class);
	  if (dataTypeString == null)
		return null;
	  final DataType dataType = DataType.fromString(dataTypeString);

	  if (dimensions != null && dataType != null)
		return new DatasetAttributes(dimensions, blockSize, dataType, null);
	} catch (IOException e) {
	}
	return null;
  }

  /**
   * Called by the {@link org.janelia.saalfeldlab.n5.N5DatasetDiscoverer}
   * while discovering the N5 tree and filling the metadata for datasets or groups.
   * <p>
   * The metadata parsing is done in the bottom-up fashion, so the children of the given {@code node}
   * have already been processed and should already contain valid metadata (if any).
   *
   * @param n5   the reader
   * @param node list of tree nodes
   * @return the metadata
   * @throws Exception parsing exception
   */
  Optional<T> parseMetadata(final N5Reader n5, final N5TreeNode node);

  default Optional<T> parseMetadata(final N5Reader n5, final String dataset) {

	if (!n5.exists(dataset))
	  return Optional.empty();
	return parseMetadata(n5, new N5TreeNode(dataset));
  }

  @Override default Optional<T> apply(N5Reader n5Reader, N5TreeNode n5TreeNode) {

	return parseMetadata(n5Reader, n5TreeNode);
  }
}
