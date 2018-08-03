# n5-ij
ImageJ convenience layer for N5

Build into your Fiji installation:
```bash
mvn -Dimagej.app.directory=/home/saalfelds/packages/Fiji.app clean install
```

Then, in Fiji's Scriptin Interpreter (Plugins > Scripts > Scripting Interpreter), load an N5 dataseets into an `ImagePlus`:
```java
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.*;

imp = N5IJUtils.load(new N5FSReader("/home/saalfelds/example.n5"), "/volumes/raw");
```

More to come...
