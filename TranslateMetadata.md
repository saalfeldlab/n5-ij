## Translate Metadata

Translations are written in [jq](https://stedolan.github.io/jq). See the [jq manual](https://stedolan.github.io/jq/manual/v1.6/) to learn more.

### Practical examples

## COSEM to canonical

With multiscales:
```
include "n5";
addPaths | walk ( if isCosem then cosemToTransform else . end ) | addAllMultiscales
```

If you know your n5 container only has single scales, you can omit the `addPaths` and `addAllMultiscales`
functions:
```
include "n5";
walk ( if isCosem then cosemToTransform else . end )
```
uses:
* [`isCosem`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L113)
* [`cosemToTransform`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L128)
* [`addPaths`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L15)
* [`addAllMultiscales`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L181)

## N5Viewer to canonical

With multiscales:
```
include "n5";
addPaths | walk ( if isN5V then n5vToTransform else . end ) | addAllMultiscales
```

If you know your n5 container only has single scales, you can omit the `addPaths` and `addAllMultiscales`
functions:
```
include "n5";
walk ( if isN5V then n5vToTransform else . end )
```

uses:
* [`isN5V`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L71)
* [`n5vToTransform`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L85)


## N5Viewer and COSEM to canonical

```
include "n5";
addPaths |  walk (
	if isCosem then cosemToTransform
	elif isN5V then n5vToTransform
	else . end )
| addAlMultiscales
```

## ImageJ to canonical

```
include "n5";
walk (
	if isIJ then ijToTransform
	else . end )
```

uses:
* [`ijToTransform`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L172)


### Tutorial

#### N5viewer to canonical

This tutorial shows how to write a translation that converts n5-viewer style spatial metadata to "canonical" metadata style from scratch.
I.e. a function that adds a new attribute to any `attribute` object in the tree with a `pixelResolution` field.

<details>
<summary>Input</summary>

```json
{ 
    "pixelResolution": { 
        "dimensions": [8, 7, 6],
        "unit": "mm"
    }
}
```
</details>

<details>
<summary>Desired output</summary>

```json
{ 
    "pixelResolution": { 
        "dimensions": [8, 7, 6],
        "unit": "mm"
    },
    "spatialTransform": {
        "transform":{
            "type": "affine",
            "affine": [8, 0, 0, 0, 0, 7, 0, 0, 0, 0, 6, 0]
        },
        "unit":"mm"
    }
}
```
</details>

We'll use the built-in jq functions [built-in jq functions](https://stedolan.github.io/jq/manual/v1.6/#Builtinoperatorsandfunctions) 
`walk`, `type`, and `has`.  The `walk` function applies a function to every part of the metadata.  `type`
returns the type of the input object: one of `object`, `array`, `string`, `number`, or `null` The `has` function
checks for the existance of a field in an object.

First let's write a function that checks if the input is an object and has the `pixelResolution` field.  We'll call it `isN5v` for "is n5-viewer".
```bash
def isN5v: type == "object" and has("pixelResolution");
```

Next, let's write a function that builds a canonical transform object from the pixel resolution object.  We'll
use the n5 jq functions `arrayAndUnitToTransform` (see below). To use it, we need to make an array containing
a flattened affine matrix as an array, and spatial units.
For the above example, we want this output:

```json
[ [8, 0, 0, 0, 0, 7, 0, 0, 0, 0, 6, 0],
"mm" ]
```

This produces the output we need:
```bash
[
  [ .pixelResolution.dimensions[0], 0, 0, 0,
     0, .pixelResolution.dimensions[1], 0, 0,
     0, 0, .pixelResolution.dimensions[2], 0 ],
  .pixelResolution.unit
]
```

* `.pixelResolution` gets the `pixelResolution` object
* `.pixelResolution.dimensions` gets the `dimensions` array
* `.pixelResolution.dimensions[0]` gets the first value (0th index) of the `dimensions` array.
* `.pixelResolution.unit` gets the value of the `unit` field in the `pixelResolution` Object


This also works:
```bash
.pixelResolution | 
[ [ .dimensions[0], 0, 0, 0, 0, .dimensions[1], 0, 0, 0, 0, .dimensions[2], 0 ],
.unit ]

```

Here `|` is the pipe operator, which feeds the output of the operation on the left to the operation on the
right.  Finally, we need to pass the output of the above to the `arrayAndUnitToTransform` function, which we
can simply do with another pipe:
```bash 
def convert: 
    .pixelResolution | 
    [ [ .dimensions[0], 0, 0, 0, 0, .dimensions[1], 0, 0, 0, 0, .dimensions[2], 0 ],
      .unit ]
    | arrayAndUnitToTransform;
```
We also made this whole operation a function called `convert`.
It's output will be a new object, which we want to add to the current attributes. 

Finally, let's apply our new repeatedly with `walk`, but only for those relevant parts of the metadata tree
(where `isN5v` returns `true`).

```bash
walk( if isN5v then . + convert else . end )
```

* `. + convert` adds the result of the `convert` method to the current object.
* `else . end` returns the current state of the tree wherever `isN5v` is `false`, and makes sure our translation
  only affects the relevant parts of the metadata tree.


Putting it all together, we have:
```bash
def convert: .pixelResolution | 
    [ [ .dimensions[0], 0, 0, 0, 0, .dimensions[1], 0, 0, 0, 0, .dimensions[2], 0 ],
      .unit ] 
    | arrayAndUnitToTransform;
def isN5v: type == "object" and has("pixelResolution");

walk( if isN5v then . + convert else . end )
```


#### Quiz 

Try writing a translation function that applies to [this metadata tree](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/test/resources/translationExamples/quizTree.json#L9-L14).

I.e. we need a function that converts this input:
```json
{
    "physicalScales": {
        "x":2.0,
        "y":3.0,
        "z":4.0
    },
    "physicalUnit": "cm"
}
```

to this output:
```json
{
  "pixelResolution": {
    "dimensions": [ 1, 2, 3 ],
    "unit": "um"
  },
  "downsamplingFactors": [ 2, 4, 8 ],
  "transform": {
    "type": "affine",
    "affine": [ 2, 0, 0, 0.5, 0, 8, 0, 1.5, 0, 0, 24, 3.5 ]
  },
  "unit": "um"
}
```

### Built-in functions

#### N5Viewer

##### [`isN5v`]

Returns true when called from a tree node that has metadata in n5 viewers metadata dialect.

<details>
<summary>Example</summary>

Input 1:
```json
{
    "pixelResolution": [1,2,3],
    "downsamplingFactors": [2,2,2]
}
```

Output 1:
`true`

Input 2:
```json
{
	"resolution": [1,2,3],
	"downsamplingFactors": [2,2,2]
}
```

Output 2:
`false`

</details>

##### [`n5vToTransform`]

Adds a canonical transform object from n5 viewer metadata dialect.

<details>
<summary>Example</summary>

Input:
```json
{
	"pixelResolution": {
		"dimensions":[1,2,3],
		"unit": "um"
    }
	"downsamplingFactors": [2,2,2]
}
```

Output:
```json
{
  "pixelResolution": {
    "dimensions": [ 1, 2, 3 ],
    "unit": "um"
  },
  "downsamplingFactors": [ 2, 4, 8 ],
  "transform": {
    "type": "affine",
    "affine": [ 2, 0, 0, 0.5, 0, 8, 0, 1.5, 0, 0, 24, 3.5 ]
  },
  "unit": "um"
}
```

</details>

#### COSEM

##### `isCosem`

Returns true when called from a tree node that has metadata in the COSEM metadata dialect.

<details>
<summary>Example</summary>

Input 1:
```json
{
  "transform": {
    "axes": [ "z", "y", "x" ],
    "scale": [ 3, 2, 1 ],
    "translate": [ 0.3, 0.2, 0.1 ],
    "units": [ "mm", "mm", "mm" ]
  }
}
```

Output 1:
`true`

Input 2:
```json
{
    "pixelResolution": {
		"dimensions": [1,2,3],
		"unit": "um"
    }
    "downsamplingFactors": [2,2,2]
}
```

Output 2:
`false`

</details>

##### `cosemToTransform`

Adds a canonical transform object from the COSEM metadata attributes.

<details>
<summary>Example</summary>

Input:

```json
{
  "transform": {
    "axes": [ "z", "y", "x" ],
    "scale": [ 3, 2, 1 ],
    "translate": [ 0.3, 0.2, 0.1 ],
    "units": [ "mm", "mm", "mm" ],
    "axisIndexes": [ 2, 1, 0 ]
  }
}
```


Output:

```json
{
  "transform": {
    "axes": [ "z", "y", "x" ],
    "scale": [ 3, 2, 1 ],
    "translate": [ 0.3, 0.2, 0.1 ],
    "units": [ "mm", "mm", "mm" ],
    "axisIndexes": [ 2, 1, 0 ]
  },
  "spatialTransform": {
    "transform": {
      "type": "affine",
      "affine": [ 1, 0, 0, 0.1, 0, 2, 0, 0.2, 0, 0, 3, 0.3 ]
    },
    "unit": "mm"
  }
}
```

</details>

#### OME-NGFF

See the [Ome-Ngff v0.3 specification](https://ngff.openmicroscopy.org/0.3/).


##### [`isOmeZarrMultiscale`] (https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L189)

Returns true when called from a tree node that has metadata in the OME-NGFF multiscale metadata.

<details>
<summary>Example</summary>

Input:
```json
{ "attributes: {
    "multiscales": [
        {
            "axes": [ "z", "y", "x" ],
            "datasets": [
                { "path": "s0" },
                { "path": "s1" },
                { "path": "s2" }
            ],
            "metadata": {
                "order": 0,
                "preserve_range": true,
                "scale": [ 0.5, 0.5, 0.5 ]
            },
            "name": "zyx",
            "type": "skimage.transform._warps.rescale",
            "version": "0.3"
        }
    ]
  },
  "children" : []
}
```

Output:
`true`

</details>


##### [`omeZarrTransformsFromMultiscale`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L196)

Given a multiscales object, returns a map from dataset names to pixel-to-physical transforms.  Useful because in
some OME-NGFF specifications, this information is not present in the dataset-level metadata attributes.

<details>
<summary>Example</summary>

Input:
```json
{
  "axes": [ "z", "y", "x" ],
  "datasets": [
    { "path": "s0" },
    { "path": "s1" },
    { "path": "s2" }
  ],
  "metadata": {
    "order": 0,
    "preserve_range": true,
    "scale": [ 0.5, 0.5, 0.5 ]
  },
  "name": "zyx",
  "type": "skimage.transform._warps.rescale",
  "version": "0.3"
}
```

Output:
```json
{
  "s0": {
    "transform": {
      "type": "scale",
      "scale": [ 0.5, 0.5, 0.5 ]
    }
  },
  "s1": {
    "transform": {
      "type": "scale",
      "scale": [ 0.25, 0.25, 0.25
      ]
    }
  },
  "s2": {
    "transform": {
      "type": "scale",
      "scale": [ 0.125, 0.125, 0.125
      ]
    }
  }
}
```

</details>


##### [`omeZarrAddTransformsToChildren`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L205)

When called from a tree node that has metadata in the OME-NGFF multiscale metadata, adds appropriate
transformation metadata to its child nodes, where this transformation is inferred from the multiscale metadata
with `omeZarrTransformsFromMultiscale`.

#### Others

##### [`isDataset`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L1)

Returns true when called from a tree node that represents an n5 dataset.

<details>
<summary>Example</summary>

Input:
```json
{ 
    "attributes": { 
        "dimensions": [8, 8],
        "dataType": "uint8"
    },
    "children" : {}
}
```
    
Output: 
`true`
</details>

##### [`isAttributes`]

Returns true when called from a tree node that represents the attributes of an n5 group or dataset.

<details>
<summary>Examples</summary>


Input 1:
```json
{
    "attributes": {
        "dimensions": [8, 8],
        "dataType": "uint8"
    },
    "children" : {}
}
```

Output 2:
`false`

Input 1:
```json
{
	"dimensions": [8, 8],
	"dataType": "uint8"
}
```

Output 2:
`true`

</details

##### [`addPaths`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L15)

Adds `path` variables into attribute objects throughout the tree.  Useful for making local operations
aware of their global location in the tree.

<details>
<summary>Example</summary>

Input:
```json
{
  "attributes": {},
  "children": {
    "c0": {
      "attributes": {},
      "children": {
        "s0": {
          "attributes": { }
        },
        "s1": {
          "attributes": { }
        }
      }
    }
  }
}
```

Output:
```json
{
  "attributes": {
    "path": ""
  },
  "children": {
    "c0": {
      "attributes": {
        "path": "c0"
      },
      "children": {
        "s0": {
          "attributes": {
            "path": "c0/s0"
          }
        },
        "s1": {
          "attributes": {
            "path": "c0/s1"
          }
        }
      }
    }
  }
}
```
</details>


##### [`arrayAndUnitToTransform`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L35)

Creates a canonical `spatialTransform` object from a two element array containing a flat affine transform,
and spatial units.

<details>
<summary>Example</summary>

Input:
```json
[ [1, 2, 3, 4, 5, 6], "parsec"]
```

Output:
```json
{
  "spatialTransform": {
    "transform": {
      "type": "affine",
      "affine": [1, 2, 3, 4, 5, 6]
    },
    "unit": "parsec"
  }
}

```

</details>


##### [`id2d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L19)

Returns the 2D identity matrix (homogeneous coordinates) as a flat array, i.e. `[1,0,0, 0,1,0]`

##### [`id3d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L17)

Returns the 3D identity matrix (homogeneous coordinates) as a flat array, i.e. `[1,0,0,0, 0,1,0,0, 0,0,1,0]`

##### [`setScale2d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L21)

Returns a 2D matrix (homogeneous coordinates) as a flat array, but replaces the diagonal elements
with the elements of the argument.

##### [`setTranslation2d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L23)

Returns a 2D matrix (homogeneous coordinates) as a flat array, but replaces the translation elements
with the elements of the argument.

##### [`setScale3d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L25)

Returns a 3D matrix (homogeneous coordinates) as a flat array, but replaces the diagonal elements
with the elements of the argument.

##### [`setTranslation3d`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L27)

Returns a 3D matrix (homogeneous coordinates) as a flat array, but replaces the translation elements
with the elements of the argument.

##### [`arrMultiply`](https://github.com/saalfeldlab/n5-ij/blob/translation-metadata/src/main/resources/n5.jq#L185

Elementwise array multiplication.

<details>
<summary>Example</summary>

Input:
```json
[1, 2, 3] as $x | [4, 5, 6] as $y | arrMultiply( $x; $y )
```

Output:
```json
[ 4, 10, 18 ]
```

</details>
