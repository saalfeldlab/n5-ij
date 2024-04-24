#@ PrefService prefs
#@ UIService ui
#@output skipWarning

skipWarning = prefs.getBoolean(N5ScalePyramidExporter.class, N5ScalePyramidExporter.IJ_PROPERTY_DO_NOT_WARN, false);
ui.showDialog("The skip warning option is " + skipWarning);

import org.janelia.saalfeldlab.n5.ij.N5ScalePyramidExporter;