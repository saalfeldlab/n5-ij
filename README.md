# n5-ij
ImageJ convenience layer for N5

## Installation
Build into your Fiji installation:
```bash
mvn -Dimagej.app.directory=/home/saalfelds/packages/Fiji.app clean install
```

## Usage
Installation adds three new plugins:
* `Plugins > N5 > Export N5 Simple`
  * Writes the currently open image to an n5 dataset
* `Plugins > N5 > Read N5 Simple`
  * Reads and n5 dataset and 
* `Plugins > N5 > Export Current Image Multiscale`
  * Writes the currently open image into a multiscale n5 dataset suitable for viewing with the [n5-viewer](https://github.com/saalfeldlab/n5-viewer)

## For developers
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

More to come...
