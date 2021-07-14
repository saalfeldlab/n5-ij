## Translate Metadata

### Examples


### Built-in functions

#### `isDataset`

Returns true when called from a tree node that represents an n5 dataset.

Example:
```json
{ 
    "attributes": { 
        "dimensions": [8, 8],
        "dataType": "uint8"
    }
    "children" : {}
}
```
returns `true`

#### `addPaths`



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


