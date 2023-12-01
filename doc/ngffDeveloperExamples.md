# NGFF developer examples 

Examples of reading and writing [ngff](https://github.com/ome/ngff) data using the N5 API.

## v0.4

The example below deal with [ngff version 0.4](https://ngff.openmicroscopy.org/0.4/)

### write: N5ScalePyramidExporter

```java
// parameters
final String n5Root = "/home/john/tmp/ngff-test.zarr";
final String metaType = N5Importer.MetadataOmeZarrKey;
final String downsamplingMethod = N5ScalePyramidExporter.DOWN_AVG;
final String dataset = "";
final String blockSizeArgument = "64";
final boolean multiscale = true;
final String compressionType = "gzip";
final long[] imageDimensions = new long[] { 64, 32, 16 };
final double[] imageResolutions = new double[] { 2.0, 3.0, 4.0 };

// make a sample image
ImagePlus imp = makeDemoImagePlus(imageDimensions, imageResolutions);

// export the image
final N5ScalePyramidExporter exp = new N5ScalePyramidExporter(
        imp, n5Root, dataset, blockSizeArgument, multiscale, downsamplingMethod, metaType, compressionType );
exp.run();
```


<details>

<summary>where `makeDemoImagePlus`</summary>

```java
public static ImagePlus makeDemoImagePlus( long[] dimensions, double... resolution )
{
    final IntImagePlus<IntType> img = ImagePlusImgs.ints(dimensions);
    final PlanarCursor<IntType> c = img.cursor();
    int i = 0;
    while( c.hasNext() )
        c.next().set(i++);

    final ImagePlus imp = img.getImagePlus();
    for( i = 0; i < resolution.length; i++ )
        if( i == 0 )
            imp.getCalibration().pixelWidth = resolution[i];
        else if( i == 1 )
            imp.getCalibration().pixelHeight = resolution[i];
        else if( i == 2 )
            imp.getCalibration().pixelDepth = resolution[i];

    return imp;
}
```

</details>


### write: low-level, single-scale

```java
// parameters
final String n5Root = "/home/john/tmp/ngff-test.zarr";
final String baseDataset = "";
final long[] imageDimensions = new long[] { 64, 32, 16 };
final int[] blockSize = new int[] { 64, 32, 16 };

// make a demo array
final ArrayImg<IntType, IntArray> img = makeDemoImage( imageDimensions );

// make demo metadata
final OmeNgffMetadata meta = OmeNgffMetadata.buildForWriting( 3,
        "name",
        AxisUtils.defaultAxes("x", "y", "z"), 	// a helper method to create axes
        new String[] {"s0"}, 					// location of the array in the hierarchy
        new double[][]{{ 2.0, 3.0, 4.0 }},		// resolution
        null);									// translation / offset (if null, interpreted as zero)


// make the n5 writer
final N5Writer n5 = new N5Factory().openWriter(n5Root);

// write the array
N5Utils.save(img, n5, baseDataset + "/s0", blockSize, new GzipCompression());

// write the metadata
try {
    new OmeNgffMetadataParser().writeMetadata(meta, n5, baseDataset);
} catch (Exception e) { }
```

<details>

<summary>where `makeDemoImage`</summary>

```java
public static ArrayImg<IntType, IntArray> makeDemoImage( long[] dimensions )
{
    int N = 1;
    for (int i = 0; i < dimensions.length; i++)
        N *= dimensions[i];

    return ArrayImgs.ints(
            IntStream.range(0, N).toArray(),
            dimensions);
}
```

</details>

### write: low-level, multi-scale

```java
// parameters
final String n5Root = "/home/john/tmp/ngff-test.zarr";
final String baseDataset = "";
final long[] imageDimensions = new long[] { 64, 32, 16 };
final int[] blockSize = new int[] { 64, 32, 16 };

// make a demo array and scale levels
List<RandomAccessibleInterval<IntType>> scaleLevels = makeDemoImageMultiscales( 3, imageDimensions, new long[]{2, 2, 2} );

// make demo metadata

// make the resolutions, 3 scale levels, base resolution[2,3,4], downsampled by [2,2,2]
final double[][] resolutions = MetadataUtils.scalesAndTranslations(new double[]{2.0, 3.0, 4.0}, new double[]{2.0, 2.0, 2.0}, 3);
// this will be:
/*
 *  [[2,  3,  4]
 *   [4,  6,  8]
 *   [8, 12, 16]]
 */
final OmeNgffMetadata meta = OmeNgffMetadata.buildForWriting( 3,
        "name",
        AxisUtils.defaultAxes("x", "y", "z"), 	// a helper method to create axes
        new String[] {"s0", "s1", "s2"},		// location of the scale arrays in the hierarchy
        resolutions,							// resolutions
        null);									// translation / offset (if null, interpreted as zero)

// make the n5 writer
final N5Writer n5 = new N5Factory().openWriter(n5Root);

// write the array
int s = 0;
for( RandomAccessibleInterval<IntType> img : scaleLevels )
    N5Utils.save(img, n5, String.format("%s/s%d", baseDataset, s++), blockSize, new GzipCompression());

// write the metadata
try {
    new OmeNgffMetadataParser().writeMetadata(meta, n5, baseDataset);
} catch (Exception e) { }
```

<details>

<summary>where `makeDemoImageMultiscales`</summary>

```java
public static List<RandomAccessibleInterval<IntType>> makeDemoImageMultiscales( int numScales, long[] baseDimensions, long[] factors )
{
    int N = 1;
    for (int i = 0; i < baseDimensions.length; i++)
        N *= baseDimensions[i];

    ArrayList<RandomAccessibleInterval<IntType>> scaleList = new ArrayList<>();
    scaleList.add( ArrayImgs.ints( IntStream.range(0, N).toArray(), baseDimensions));
    for( int i = 1; i < numScales; i++ )
        scaleList.add(Views.subsample( scaleList.get(0), factors ));

    return scaleList;
}
```

</details>
