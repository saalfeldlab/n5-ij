include "n5";

def canConvert: type == "object" and (has("scale") or has("origin") or has("unit"));

def getUnit: if has("unit") then .unit else "pixel" end;
def getScale: if has("scale") then .scale | [.x, .y, .z] else [1,1,1] end;
def getTranslation: if has("origin") then .origin | [.x, .y, .z] else [0,0,0] end;

def convert:
    getScale as $s | getTranslation as $t | getUnit as $unit |
    [   id3d | setScale3d( $s ) | setTranslation3d( $t ),
        $unit
    ] | arrayAndUnitToTransform;

walk( if canConvert then . + convert else . end ) | addPaths
