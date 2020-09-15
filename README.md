# n5-ij [![Build Status](https://travis-ci.com/saalfeldlab/n5-ij.svg?branch=master)](https://travis-ci.com/saalfeldlab/n5-ij)
ImageJ convenience layer for N5

Build into your Fiji installation:
```bash
mvn -Dimagej.app.directory=/home/saalfelds/packages/Fiji.app clean install
```

Then, in Fiji's Scriptin Interpreter (Plugins > Scripts > Scripting Interpreter), load an N5 dataset into an `ImagePlus`:
```java
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;

imp = N5IJUtils.load(new N5FSReader("/home/saalfelds/example.n5"), "/volumes/raw");
```

or save an `ImagePlus` into an N5 dataset:
```java
import ij.IJ;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;

N5IJUtils.save(
    IJ.getImage(),
    new N5FSWriter("/home/saalfelds/example.n5"),
    "/volumes/raw",
    new int[] {128, 128, 64},
    new GzipCompression()
);
```


## Cloud writing benchmarks

Below are a benchmarks for writing images of various sizes, block sizes, with 
increasing amount of parallelism.  

### Amazon S3

Time in seconds to write the image data. Increased parallelism speeds
up writing substantially when the total number of blocks is high.

|  Image size  | Block size |  1  |  2  |  4  |  8  |  16  |
|  64x64x64  | 32x32x32 | 0.98 | 0.60 | 0.45 | 0.50 | 0.51 | 
|  128x128x128  | 32x32x32 | 4.72 | 2.64 | 1.62 | 1.00 |
|  256x256x256  | 32x32x32 | 37.09 | 19.11 | 9.09 | 5.20 | 3.2 |
|  256x256x256  | 64x64x64 | 10.56 | 5.04 | 3.23 | 2.17 | 1.86 |
|  512x512x512  | 32x32x32 | 279.28 | 156.89 | 74.72 | 37.15 | 19.77 |
|  512x512x512  | 64x64x64 | 76.63 | 38.16 | 19.86 | 10.16 | 6.14 |
|  512x512x512  | 128x128x128 | 27.16 | 14.32 | 8.01 | 4.70 | 3.31 |
|  1024x1024x1024  | 32x32x32 | 2014.73 | 980.66 | 483.04 | 249.83 | 122.36 |
|  1024x1024x1024  | 64x64x64 | 579.46 | 289.53 | 149.98 | 75.85 | 38.18 |
|  1024x1024x1024  | 128x128x128 | 203.47 | 107.23 | 55.11 | 27.41 | 15.33 |

