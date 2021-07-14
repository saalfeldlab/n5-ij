## Translate Metadata

Translations are writtein in [jq](https://stedolan.github.io/jq). See the [jq manual](https://stedolan.github.io/jq/manual/v1.6/) to learn more.

### Examples

#### N5viewer to canonical

This examples shows how to write a translation that converts n5-viewer style spatial metadata to "canonical" metadata style.
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


### Built-in functions

#### `isDataset`

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

#### `addPaths`

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


#### `arrayAndUnitToTransform`

Creates a canonical `spatialTransform` object from a two element array containing a flat affine tranform,
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


