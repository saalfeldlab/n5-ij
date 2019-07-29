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

More to come...
