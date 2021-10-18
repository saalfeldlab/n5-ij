def isDataset: type == "object" and has("attributes") and (.attributes | has("dimensions") and has("dataType") );

def hasAttributes: type == "object" and has("attributes");

def isAttributes: type == "object" and has("dimensions") and has("dataType");

def numDimensions: .dimensions | length;

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

def setFlatAffine( $val; $nd; $i; $j ): ($i * ($nd +1)  + $j) as $k | .[$k] = $val;

def permute( $arr; $indexes ):
   if all( (( $arr | type ) == "array" ); ( ($indexes | type) == "array"  )) then
       reduce ($indexes | .[]) as $i ( []; . + [ $arr | .[$i]])
   else null end;

def identityAsFlatAffine( $nd ): 
    reduce range( $nd * ($nd +1)) as $i ([]; . + [0]) |
    reduce range($nd) as $i (.; . | setFlatAffine( 1; $nd; $i; $i ));

def scaleAndOffset( $s; $o ):
{
    "type" : "scale_offset",
    "scale" : $s,
    "offset" : $o
};

def scaleOffsetUnitToTransform( $s; $o; $u ):
{
    "spatialTransform" : {
        "transform" : scaleAndOffset( $s; $o ),
        "unit": .[1]
    }
};

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

def arrayUnitAxisToTransform($a;$u;$x): {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": $a
        },
        "unit": $u,
        "axes" : $x
    }
};

def affineFromScaleAndFactors( $s; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ] 
  else null end;
  
def affineFromScaleAndFactorsArr:
  .[0] as $s | .[1] as $f | 
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ] 
  else null end;

def scaleOffsetFromScaleAndFactorsArr:
  .[0] as $s | .[1] as $f | 
  if ( $s | length) == 2 then [ [($s[0]*$f[0]), ($s[1]*$f[1])], [ ($f[0]-1)/2.0,  ($f[1]-1)/2.0 ] ]
  elif ($s | length) == 3 then [ [($s[0]*$f[0]), ($s[1]*$f[1]), ($s[2]*$f[2])], [($f[0]-1)/2.0, ($f[1]-1)/2.0, ($f[2]-1)/2.0 ]]
  else null end;

def applyDownsamplingToFlatAffine( $a; $f ):
  if ( $s | length) == 2 then [ ($s[0]*$f[0]), 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), ($f[1]-1)/2.0 ]
  elif ($s | length) == 3 then [($s[0]*$f[0]), 0, 0, ($f[0]-1)/2.0, 0, ($s[1]*$f[1]), 0, ($f[1]-1)/2.0, 0, 0,  ($s[2]*$f[2]), ($f[2]-1)/2.0 ]    
  else null end;

def isN5V: type == "object" and has("pixelResolution");

def n5vTransformArr: { "type":"affine", "affine": (.pixelResolution | affineFromScaleArray)};

def n5vTransformObj: { "type":"affine", "affine": (.pixelResolution.dimensions | affineFromScaleArray )};

def n5visResObjDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResObj: has("pixelResolution") and (.pixelResolution | type == "object");

def n5visResArrDs: has("downsamplingFactors") and has("pixelResolution") and (.pixelResolution | type == "array");

def n5visResArr: has("pixelResolution") and (.pixelResolution | type == "array");

def n5vToTransform: {
  "transform": {
    "type": "affine",
    "affine": [ 
            (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
            (.downsamplingFactors // [1, 1, 1] )] |
            affineFromScaleAndFactorsArr  
  },
  "unit" : (.pixelResolution.unit // "pixel")
};

def n5vToScaleOffset: 
( [ (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
(.downsamplingFactors // [1, 1, 1] )] | scaleOffsetFromScaleAndFactorsArr ) as $scaleOffset | 
{
  "transform": {
    "type": "scale_offset",
    "scale": ($scaleOffset | .[0]),
    "offset": ($scaleOffset | .[1])
  }
};

def n5vToTransformF: . + {
  "transform": {
    "type": "affine",
    "affine":  affineFromScaleAndFactors(
            (if n5visResObj then .pixelResolution.dimensions elif n5visResArr then .pixelResolution else null end),
            (.downsamplingFactors // [1, 1, 1] ) )
  },
  "unit" : (.pixelResolution.unit // "pixel")
};

def scaleOffsetToN5v: .spatialTransform | .unit as $unit | .transform | {
    "pixelResolution" :  {
        "dimensions": .scale,
        "unit" : $unit
    }
};

def n5vToCanonicalAffine: . + { "spatialTransform" : n5vToTransform };

def n5vToCanonicalScaleOffset: . + { "spatialTransform" : n5vToScaleOffset };

def attrHasTform: (.attributes | has("spatialTransform"));

def numTformChildren: .children | reduce (keys| .[]) as $k (
    [.,0];
    [  .[0],
       if (.[0] | .[$k] | attrHasTform) then .[1] + 1 else .[1] end ])
      | .[1];

def isCosem: type == "object" and has("transform") and (.transform | type == "object") and  (.transform | has("axes") and has("scale") and has("translate") and has("units") );

def cosemAxisIndexes: {"axisIndexes":[ (.axes | index("x")) , (.axes | index("y")), (.axes | index("z")) ]};

def cosemToScaleOffsetSimple: { "spatialTransform": {
        "transform": {
            "type":"scale_offset",
            "scale" : [ .scale[.axisIndexes[0]],
                .scale[ .axisIndexes[1]],
                .scale[ .axisIndexes[2]] ],
            "offset" : [ .translate[.axisIndexes[0]],
                .translate[.axisIndexes[1]],
                .translate[.axisIndexes[2]] ]
        },
        "unit" : .units[0]
    }
};

def scaleOffsetToCosem: .spatialTransform | .unit as $u | .transform | {
    "scale" : permute( .scale; [2,1,0] ),
    "translate" : permute( .offset; [2,1,0]),
    "units" : [ $u, $u, $u ],
    "axes" : ["z", "y", "x" ]
};

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

def cosemToTransform: (.transform |= . + cosemAxisIndexes) | . + (.transform | cosemToScaleOffsetSimple);

def isIJ: isAttributes and has("pixelWidth") and has("pixelHeight") and has("pixelUnit") and has("xOrigin") and has("yOrigin");

def ijDimensionsSafe: . as $this | [1,1,1] 
    | if ( $this.numChannels > 1 ) then .[0] = $this.numChannels else . end 
    | if ( $this.numSlices > 1 ) then .[1] = $this.numSlices else . end 
    | if ( $this.numFrames > 1 ) then .[2] = $this.numFrames else . end;

def ijDimensions: [.numChannels, .numSlices, .numFrames];

def axis( $l; $t; $u ): { label : $l, type : $t, unit : $u };

def ijAxes: .pixelUnit as $unit | ijDimensions as $czt | 
     [  axis( "x"; "space"; $unit ), axis( "y"; "space"; $unit ) ] 
     | if ($czt | .[0]) > 1 then . + [axis("c";"channels";"na")] else . end
     | if ($czt | .[1]) > 1 then . + [axis("z";"space";$unit)] else . end
     | if ($czt | .[2]) > 1 then . + [axis("t";"time";"s")] else . end;

def ijAffine2d3d: 
    . as $this |
    if ( .dimensions | length ) == 2 then
        id2d | setScale2d( [$this.pixelWidth, $this.pixelHeight] ) | setTranslation2d([ $this.xOrigin, $this.yOrigin] ) 
    elif ( .dimensions | length ) == 3 then
        id3d | setScale3d( [$this.pixelWidth, $this.pixelHeight, $this.pixelDepth]) | setTranslation3d([ $this.xOrigin, $this.yOrigin, $this.zOrigin])
    else null end;

def ijAffineNd: . as $this | numDimensions as $nd | ijDimensions as $czt 
    | identityAsFlatAffine($nd)
    | setFlatAffine( $this.pixelWidth; $nd; 0; 0 )
    | setFlatAffine( $this.xOrigin; $nd; 0; $nd )
    | setFlatAffine( $this.pixelHeight; $nd; 1; 1 )
    | setFlatAffine( $this.yOrigin; $nd; 1; $nd )
    | [2, .]
    | if ($czt | .[0]) > 1 then [ .[0] +1, .[1] ] else . end
    | if ($czt | .[1]) > 1 then 
        .[0] as $i | .[1] | setFlatAffine( $this.pixelDepth; $nd; $i; $i) | setFlatAffine( $this.zOrigin; $nd; $i; $nd) 
        | [ $i +1, . ]
        else . end
    | if ($czt | .[2]) > 1 then 
        .[0] as $i | .[1] | setFlatAffine( $this.frameInterval; $nd; $i; $i) | [ $i +1, . ]
        else . end
    | .[1];

def ijToTransform: ([ijAffineNd, null] | arrayAndUnitToTransform) as $transform |
    ijAxes as $axes | . + $transform | . + { axes: $axes } ;

def hasMultiscales: type == "object" and has("children") and ( numTformChildren > 1 );

def buildMultiscale: [(.children | keys | .[]) as $k | .children |  {"path": (.[$k].attributes.path | split("/") |.[-1]), "spatialTransform" : .[$k].attributes.spatialTransform }];

def addMultiscale: buildMultiscale as $ms | .attributes |= . + { "multiscales": { "datasets": $ms , "path": .path }};

def addAllMultiscales: walk( if hasMultiscales then addMultiscale else . end );

def isMultiChannel: type == "object" and has("children") and ( .children | has("c0"));

def buildMultiChannelFull: [(.children | keys | .[]) as $k | .children |  ( .[$k].attributes ) ];

def buildMultiChannel: [(.children | keys | .[]) as $k | .children |  {"path": (.[$k].attributes.path | split("/") |.[-1]) }];

def addMultiChannelFull: buildMultiChannelFull as $ms | .attributes |= . + { "multichannel": { "datasets": $ms , "path": .path }};

def addMultiChannel: buildMultiChannel as $ms | .attributes |= . + { "multichannel": { "datasets": $ms , "path": .path }};

def addAllMultichannelFull: walk( if isMultiChannel then addMultiChannelFull else . end );

def addAllMultichannel: walk( if isMultiChannel then addMultiChannel else . end );

def getScales: .multiscales | .[0] | .metadata | .scale;

def arrMultiply( $s1; $s2 ): [$s1, $s2] | transpose | map(.[0] * .[1]) ;

def scaleTransform( $scales ): { "type" : "scale", "scale" : $scales };

def isOmeNgffMultiscale:
    has("attributes") and
    (.attributes | has("multiscales")) and
    (.attributes | .multiscales | type == "array") and
    (.attributes | .multiscales | length > 0 ) and
    (.attributes | .multiscales | .[0] | has("datasets") );

def omeNgffTransformsFromMultiscale:
    (.metadata | .scale) as $scales |
    reduce (.datasets | .[]) as $d (
        [ {}, $scales, $scales ];
        [ .[0] + { ($d | .path) : { "transform": scaleTransform(.[1])} },
        arrMultiply( .[1]; .[2]),
        .[2] ])
    | .[0];

def omeNgffAddTransformsToChildren:
    .children as $children |
    (.attributes | .multiscales | .[0]) as $ms |
    ( $ms | omeZarrTransformsFromMultiscale) as $transforms |
    ( $ms | .datasets | map (.path)) as $paths |
    ( reduce ($paths | .[] ) as $p (
        $children;
        .[$p] |= . + ( $transforms | .[$p]) )) as $newChildren |
    .children |= $newChildren;

def n5PathToTreePath: ltrimstr( "/") | split("/") | map_values( ["children", . ] ) | flatten;

def getSubTree( $path ): getpath( $path | n5PathToTreePath );

def moveSubTree( $srcPath; $dstPath ): getSubTree( $srcPath ) as $subTree | setpath( $dstPath | n5PathToTreePath; $subTree ) 
    | delpaths([$srcPath | n5PathToTreePath]);
