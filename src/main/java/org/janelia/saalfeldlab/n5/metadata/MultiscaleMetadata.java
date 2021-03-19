package org.janelia.saalfeldlab.n5.metadata;

import java.util.Objects;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

public abstract class MultiscaleMetadata<T extends PhysicalMetadata> implements PhysicalMetadata
{
	final private String[] paths;

	final private AffineTransform3D[] transforms;

	final private String[] units;

	final private PhysicalMetadata[] childrenMetadata;
	
	public MultiscaleMetadata()
	{
		paths = null;
		transforms = null;
		childrenMetadata = null;
		units = null;
	}
	
	public MultiscaleMetadata( final T[] childrenMetadata )
	{
        Objects.requireNonNull( childrenMetadata );
        this.childrenMetadata = childrenMetadata;
        
		final int N = childrenMetadata.length;
        transforms = new AffineTransform3D[ N ];
        paths = new String[ N ];
        units = childrenMetadata[ 0 ].units();

        int i = 0;
        for( T meta : childrenMetadata)
        {
        	 Objects.requireNonNull( meta );	
        	 paths[ i ] = meta.getPath();
        	 transforms[ i ] = meta.physicalTransform3d();
        }
	}

	public MultiscaleMetadata( final String[] paths, final AffineTransform3D[] transforms, final String[] units )
	{
        Objects.requireNonNull(paths);
        Objects.requireNonNull(transforms);

        for (final String path : paths)
            Objects.requireNonNull(path);
        for (final AffineTransform3D transform : transforms)
            Objects.requireNonNull(transform);

		this.paths = paths;
		this.transforms = transforms;
		this.units = units;
		this.childrenMetadata = null;
	}

	public String[] getPaths()
	{
		return paths;
	}

	public AffineTransform3D[] getTransforms()
	{
		return transforms;
	}

	@Override
	public AffineGet physicalTransform()
	{
		// by default, spatial transforms are specified by the individual scales by default
		return null;
	}

	@Override
	public String[] units()
	{
		return units;
	}

}
