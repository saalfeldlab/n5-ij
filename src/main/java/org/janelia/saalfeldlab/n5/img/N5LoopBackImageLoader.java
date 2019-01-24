package org.janelia.saalfeldlab.n5.img;

import java.util.HashMap;

import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.spimdata.SequenceDescriptionMinimal;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import net.imglib2.Dimensions;
import net.imglib2.Volatile;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;

public class N5LoopBackImageLoader <T extends RealType<T> & NativeType<T>, V extends Volatile<T> > {

	protected final N5Reader n5;

	protected final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;
	
	protected final T type;

	protected final V vtype;

	private N5LoopBackImageLoader( final N5Reader existingN5Reader, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription, T t, V vtype)
	{
		this.n5 = existingN5Reader;
		this.sequenceDescription = sequenceDescription;
		this.type = t;
		this.vtype = vtype;
	}

	public static <T extends RealType<T> & NativeType<T>, V extends Volatile<T>> N5LoopBackImageLoader<T,V> create( final N5Reader existingN5Reader, final int timepointIdPartition, final int setupIdPartition, final Dimensions imageDimensions, final T t, final V v )
	{
		final HashMap< Integer, TimePoint > timepoints = new HashMap<>();
		timepoints.put( timepointIdPartition, new TimePoint( timepointIdPartition ) );
		final HashMap< Integer, BasicViewSetup > setups = new HashMap<>();
		setups.put( setupIdPartition, new BasicViewSetup( setupIdPartition, null, imageDimensions, null ) );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepoints ), setups, null, null );
		return new N5LoopBackImageLoader<T,V>( existingN5Reader, seq, t, v );
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public N5SetupImgLoader getSetupImgLoader( final int setupId )
	{
		return new N5SetupImgLoader(n5, (NumericType) type, new Volatile<T>(type), setupId );
	}
	
	public void close()
	{
		// dn't need to do anything
	}
}
