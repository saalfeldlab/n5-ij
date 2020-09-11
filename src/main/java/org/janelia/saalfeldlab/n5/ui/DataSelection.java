package org.janelia.saalfeldlab.n5.ui;

import java.util.Collections;
import java.util.List;

import org.janelia.saalfeldlab.n5.AbstractGsonReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

public class DataSelection
{
	public final N5Reader n5;

	public final List< N5Metadata > metadata;

	public DataSelection( final N5Reader n5, final List< N5Metadata > metadata )
	{
		this.n5 = n5;
		this.metadata = Collections.unmodifiableList( metadata );
	}
}
