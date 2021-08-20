
def isDataset: type == "object" and has("attribute") and (.attributes | has("dimensions") and has("dataType") );

def isAttributes: type == "object" and has("dimensions") and has("dataType");

def hasDims: .attributes | has("dimensions");

def flattenTree: .. | select( type == "object" and has("path")) | del(.children);

def parentPath: if length <= 1 then "" elif length == 2 then .[0] else .[0:-1] | map(select( . != "children")) | join("/") end;

def attrPaths: paths | select(.[-1] == "attributes");

def addPaths: reduce attrPaths as $path ( . ; setpath( [ ($path | .[]), "path"]; ( $path | parentPath )));

def id3d: [1,0,0,0, 0,1,0,0, 0,0,1,0];

def id2d: [1,0,0, 0,1,0];

def setScale2d( $s ): .[0] = $s[0] | .[4] = $s[1];

def setTranslation2d( $t ): .[2] = $t[0] | .[5] = $t[1];

def setScale3d( $s ): .[0] = $s[0] | .[5] = $s[1] | .[10] = $s[2];

def setTranslation3d( $t ): .[3] = $t[0] | .[7] = $t[1] | .[11] = $t[2];

def arrayAndUnitToTransform: {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": .[0]
        },
        "unit": .[1]
    }
};

def arrayUnitToTransform($a;$u): {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": $a 
        },
        "unit": $u
    }
};

def affineFromScaleAndFactors( $s; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]    
  else null end;

def applyDownsamplingToFlatAffine( $a; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]    
  else null end;

def n5vTransformArr: { "type":"affine", "affine": (.pixelResolution | affineFromScaleArray)};

def n5vTransformObj: { "type":"affine", "affine": (.pixelResolution.dimensions | affineFromScaleArray )};

def n5visResObjDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResObj: has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResArrDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "array");

def n5visResArr: has("pixelResolution") and (.pixelResolution | type == "array");

def attrHasTform: (.attributes | has("spatialTransform"));

def numTformChildren: .children | reduce (keys| .[]) as $k (
    [.,0];
    [  .[0],
       if (.[0] | .[$k] | attrHasTform) then .[1] + 1 else .[1] end ])
      | .[1];

def isCosem: type == "object" and has("transform") and (.transform | type == "object") and  (.transform | has("axes") and has("scale") and has("translate") and has("units") );

def cosemAxisIndexes: {"axisIndexes":[ (.axes | index("x")) , (.axes | index("y")), (.axes | index("z")) ]};

def cosemToTransformSimple: { "spatialTransform": {
        "transform": {
            "type":"affine",
            "affine": [ .scale[.axisIndexes[0]], 0.0, 0.0, .translate[.axisIndexes[0]],
                        0.0, .scale[.axisIndexes[1]], 0.0, .translate[.axisIndexes[1]],
                        0.0, 0.0, .scale[.axisIndexes[2]], .translate[.axisIndexes[2]]]
        },
        "unit" : .units[0]
        }
    };

def cosemToTransform: (.transform |= . + cosemAxisIndexes) | . + (.transform | cosemToTransformSimple);

def hasMultiscales: type == "object" and has("children") and ( numTformChildren > 1 );

def buildMultiscale: [(.children | keys | .[]) as $k | .children |  {"path": (.[$k].attributes.path | split("/") |.[-1]), "spatialTransform" : .[$k].attributes.spatialTransform }];

def addMultiscale: buildMultiscale as $ms | .attributes |= . + { "multiscales": { "datasets": $ms , "path": .path }};

def addAllMultiscales: walk( if hasMultiscales then addMultiscale else . end );
