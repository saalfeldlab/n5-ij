# n5-ij [![Build Status](https://travis-ci.com/saalfeldlab/n5-ij.svg?branch=master)](https://travis-ci.com/saalfeldlab/n5-ij)

A Fiji plugin for loading and saving image data to N5 containers. Supports [HDF5](https://www.hdfgroup.org/solutions/hdf5/), [Zarr](https://zarr.readthedocs.io/en/stable/#), [Amazon S3](https://aws.amazon.com/s3/), and [Google cloud storage](https://cloud.google.com/storage).

## Contents
1. [Open N5](#open-n5)
2. [Export N5](#export-n5)
3. [Metadata](#metadata)
4. [For developers](#for-developers)
5. [Cloud writing benchmarks](#cloud-writing-benchmarks)

## Open N5

Open N5 datasets from Fiji with `File > Import > N5`. 

Quickly open a dataset by pasting the full path to the dataset and press `OK`.
For example, try `gs://example_multi-n5_bucket/mitosis.n5/raw` to open the sample mitosis image from google
cloud storage.

Click the `Browse` button to select a folder on your filesystem.

<img src=https://raw.githubusercontent.com/saalfeldlab/n5-ij/master/doc/OpenN5DialogWithBrowse.png width="600">

The detected datasets will be displayed in the dialog. Selected (highlight) the datasets you would like to open
and press `OK`. In the example below, we will open the datasets `/blobs`, and `/t1-head/c0/s0`.

<img src=https://raw.githubusercontent.com/saalfeldlab/n5-ij/master/doc/OpenN5DialogWithTree.png width="600">

### Virtual 

Check the `Open as virtual` box to open the n5 dataset as a [virtual stacks in imagej](https://imagej.nih.gov/ij/docs/guide/146-8.html#toc-Section-8). 
This enable the opening and viewing of image data that do not fit in RAM. Image slices are loaded on-the-fly, so
navigation will be slow when parts of the images are loaded.

### Cropping 

Subsets of images can be opened by checking the `Crop` box in the dialog, then pressing `OK`.
A separate dialog will appear for each selected dataset as shown below.

<img src=https://raw.githubusercontent.com/saalfeldlab/n5-ij/master/doc/OpenN5DialogWithCrop.png width="700">

Give the min and max values for the field-of-view to open **in pixel / voxel units** to open a particular
subset. The opened interval includes buth min and max values, so the image will be of size `max - min + 1` along
each dimension.  In the example shown above, the resulting image will be of size `101 x 111 x 2 x 51`.

## Export N5

Save images open in Fiji as N5 datasets with `File > Save As > Export N5`.

<img src=https://raw.githubusercontent.com/saalfeldlab/n5-ij/master/doc/SaveN5Dialog.png width="280">

Parameters
* `N5Root` - the root location of the n5 (see also [Container types](#container-types))
* `Dataset` - the name of the dataset.
* `Block size` - block size as comma-separated list.  
  * Length of list must match dimensionality of dataset
* `metadata type` - style and type of metadata to store (see also [Metadata](#metadata))
* `thread count` - number of threads used for parallel writing (see also [Cloud writing benchmarks](#cloud-writing-benchmarks))

## Container types

The export plugin infers container type from the file/directory path or url given as the n5 root:

* Filesystem N5 
    * Specify a directory ending in `.n5` 
    * example `/path/to/my/data.n5`
* Zarr
    * Specify a directory ending in `.zarr` 
    * example `/Users/user/Documents/sample.zarr`
* HDF5
    * Specify a file ending in `.h5` ,`.hdf5`, or `.hdf`
    * example `C:\user\docs\example.h5`
* Amazon S3 
    * Specify one of two url styles:
    * `s3://bucket-name/path/to/root.n5`
    * `https://bucket-name.s3.amazonaws.com/path/to/root.n5`
* Google cloud storage (one of two url styles)
    * Specify one of two url styles:
    * `gs://bucket-name/path/inside/bucket/root.n5`
    * `https://bucket-name.s3.amazonaws.com/path/to/root.n5`


## Metadata 

This plugin supports three types of image metadata:
1) ImageJ-style metadata 
2) [N5-viewer](https://github.com/saalfeldlab/n5-viewer) metadata
3) [COSEM](https://github.com/janelia-cosem/schemas/blob/master/multiscale.md) metadata

The metadata style for exported N5 datasets is customizable, more detail coming soon.

## For developers

ImageJ convenience layer for N5

Build into your Fiji installation:
```bash
mvn -Dscijava.app.directory=/home/saalfelds/packages/Fiji.app -Ddelete.other.versions=true clean install
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

## Details

* This plugin supports images of up to 5 dimensions, and the datatypes supported by Imagej (`uint8`, `uint16`, `float32`) For higher dimensions and other datatypes, we recommend [n5-imglib2](https://github.com/saalfeldlab/n5-imglib2).

* This plugin supports only the datatypes supported by ImageJ, namely uint8, uint16, and float32. For other datatypes[n5-imglib2](https://github.com/saalfeldlab/n5-imglib2).

### Metadata

### Cloud writing benchmarks

Below are a benchmarks for writing images of various sizes, block sizes, with 
increasing amount of parallelism.  

#### Amazon S3

Time in seconds to write the image data. Increased parallelism speeds
up writing substantially when the total number of blocks is high.

|  Image size  | Block size |  1 thread  |  2 threads  |  4 threads  |  8 threads  |  16 threads  |
| ------------ | ---------- | ---------- | ----------- | ----------- | ----------- | ----------- |
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

