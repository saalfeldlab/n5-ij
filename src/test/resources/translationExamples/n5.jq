#def isDatasetOld: .attributes? | has("dimensions") and has("blockSize") and has("dataType") and has("compression");

def isDataset: type == "object" and has("attribute") and (.attributes | has("dimensions") and has("dataType") );

def isAttributes: type == "object" and has("dimensions") and has("dataType");

def hasDims: .attributes | has("dimensions");

def flattenTree: .. | select( type == "object" and has("path")) | del(.children);

def parentPath: if length <= 1 then "" elif length == 2 then .[0] else .[0:-1] | map(select( . != "children")) | join("/") end;

def attrPaths: paths | select(.[-1] == "attributes");

def addPaths: reduce attrPaths as $path ( . ; setpath( [ ($path | .[]), "path"]; ( $path | parentPath )));

def arrayAndUnitToTransform: {
    "spatialTransform": {
        "transform" : {
            "type": "affine",
            "affine": .[0]
        },
        "unit": .[1]
    }
};


