## Custom metadata

You can specify the format of metadata for exported datasets by selecting the `Custom` option for the `metadata type`. 
A dialog will appear, initialized with the [resolution only examples](#spatial-resolution-in-array)
specification. The specification transforms the [variables](#variables) into a new json, formatting the metadata
as you desire. The specifcation mapper uses [jackson-jq](https://github.com/eiiches/jackson-jq), a java implementation of [jq](https://stedolan.github.io/jq/).
See also the examples below.

<img src=https://raw.githubusercontent.com/saalfeldlab/n5-ij/master/doc/MetadataTranslationDialog.png width="500">

### Variables

* `name`
* `xResolution` - spatial resolution in x
* `yResolution` - spatial resolution in y
* `zResolution` - spatial resolution in z
* `tResolution` - resolution in time
* `xOrigin` - spatial origin x
* `yOrigin` - spatial origin y
* `zOrigin` - spatial origin z
* `tOrigin` - origin in time
* `xUnit` - physical unit of x resolution
* `yUnit`- physical unit of y resolution
* `zUnit`- physical unit of z resolution
* `tUnit`- unit of time resolution
* `globalUnit` - global spatial resolution unit
* `axis0` - label of axis 0
* `axis1` - label of axis 1
* `axis2` - label of axis 2
* `axis3` - label of axis 3
* `axis4` - label of axis 4

### Examples

All examples will show output using the 5d [`mitosis.tif`](https://imagej.net/images/Spindly-GFP.zip) sample image from ImageJ.
Code implementing these examples can be found [here](src/test/java/org/janelia/saalfeldlab/n5/CustomMetadataExamples.java).

#### Spatial resolution in array
```
{
"resolution" : [ .xResolution, .yResolution, .zResolution ]
}
```

yields:

```
{
  "resolution": [
    0.08850000022125,
    0.08850000022125,
    1
  ]
}
```


#### COSEM
```
{
"transform": 
    { 
    "scale": [.xResolution, .yResolution, .zResolution], 
    "translate": [.xOrigin, .yOrigin, .zOrigin], 
    "axes": [.axis0, .axis1, .axis2, .axis3, .axis4], 
    "units": [.xUnit, .yUnit, .zUnit] 
    } 
}
```

```
{
"transform" :
    {
    "axes" : ["x","y","c","z","t"],
    "scale" : [0.08850000022125,0.08850000022125,1.0],
    "units" : ["µm","µm","µm"],
    "translate" : [0.0,0.0,0.0]
    }
}
```
