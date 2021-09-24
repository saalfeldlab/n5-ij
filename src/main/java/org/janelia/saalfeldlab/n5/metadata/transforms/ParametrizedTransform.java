package org.janelia.saalfeldlab.n5.metadata.transforms;

import org.janelia.saalfeldlab.n5.N5Reader;

public interface ParametrizedTransform<T,P> {

	public String getParameterPath();

	public T buildTransform( P parameters );

	public P getParameters( final N5Reader n5 );
}
