package org.janelia.saalfeldlab.n5.converters;

import net.imglib2.converter.Converter;
import net.imglib2.type.label.LabelMultisetType;
import net.imglib2.type.numeric.integer.UnsignedLongType;

public class LabelMultisetLongConverter implements Converter< LabelMultisetType, UnsignedLongType > {

	@Override
	public void convert( final LabelMultisetType input, final UnsignedLongType output ) {

		output.set( input.argMax() );
	}

}
