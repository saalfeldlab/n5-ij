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

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

public class N5TreeNode extends DefaultMutableTreeNode
{
	private static final long serialVersionUID = -6433341489220400345L;

	public final String path;

	private boolean isDataset;

	private N5Metadata metadata;

	public N5TreeNode( final String path, final boolean isDataset )
	{
		super();
		this.path = path;
		this.isDataset = isDataset;
	}

    public String getNodeName()
    {
        return Paths.get(removeLeadingSlash(path)).getFileName().toString();
    }

	@SuppressWarnings("unchecked")
	public List< N5TreeNode > childrenList()
	{
		/* TODO compiles with Eclipse compiler but not with javac because children
		 * forwards to rawtype Enumeration DefaultMutableTreeNode#children()...
		 */
		@SuppressWarnings("rawtypes")
		final List children = Collections.list( children() );
		return children;
	}

	public void setIsDataset( final boolean isDataset )
	{
		this.isDataset = isDataset;
	}

	public boolean isDataset()
	{
		return isDataset;
	}

	public void setMetadata( final N5Metadata metadata )
	{
		this.metadata = metadata;
	}

	public N5Metadata getMetadata()
	{
		return metadata;
	}


	@Override
	public String toString()
	{
		final String nodeName = getNodeName();
		return nodeName.isEmpty() ? "/" : nodeName;
	}

    /**
     * Removes the leading slash from a given path and returns the corrected path.
     * It ensures correctness on both Unix and Windows, otherwise {@code pathName} is treated
     * as UNC path on Windows, and {@code Paths.get(pathName, ...)} fails with {@code InvalidPathException}.
     *
     * @param pathName
     * @return
     */
    protected static String removeLeadingSlash(final String pathName) {

        return pathName.startsWith("/") || pathName.startsWith("\\") ? pathName.substring(1) : pathName;
    }
}