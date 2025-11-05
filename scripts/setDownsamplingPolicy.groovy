#@ String (visibility=MESSAGE, value="<html>Sets the downsampling policy for the N5 scale pyramid exporter<br><ul><li>Aggressive: more scale levels</li><li>Conservative: fewer scale levels</li><ul></html>") docmsg
#@ PrefService prefs
#@ String (choices={"Aggressive", "Conservative"}, style="radioButtonHorizontal") policy

prefs.put(N5ScalePyramidExporter.class, N5ScalePyramidExporter.IJ_PROPERTY_DOWNSAMPLE_POLICY, policy);

import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;