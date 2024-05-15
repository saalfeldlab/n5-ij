package org.janelia.saalfeldlab.n5.ui;

import java.awt.Component;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.apache.commons.lang.ArrayUtils;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff.v04.OmeNgffMultiScaleMetadata;

import ij.ImagePlus;

public class N5DatasetTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = -4245251506197982653L;

	protected static final String thinSpace = "&#x2009;";

	protected static final String times = "&#xd7;";

	protected static final String warningFormat = "<font color=\"rgb(179, 58, 58)\">%s</font>";

	protected static final String nameFormat = "<b>%s</b>";

	protected static final String dimDelimeter = thinSpace + times + thinSpace;

	protected final boolean showConversionWarning;

	protected String rootName;

	public N5DatasetTreeCellRenderer( final boolean showConversionWarning )
	{
		this.showConversionWarning = showConversionWarning;
	}

	public void setRootName( final String rootName ) {
		this.rootName = rootName;
	}

	@Override
	public Component getTreeCellRendererComponent( final JTree tree, final Object value,
			final boolean sel, final boolean exp, final boolean leaf, final int row, final boolean hasFocus )
	{

		super.getTreeCellRendererComponent( tree, value, sel, exp, leaf, row, hasFocus );

		N5SwingTreeNode node;
		if ( value instanceof N5SwingTreeNode )
		{
			node = ( ( N5SwingTreeNode ) value );
			if ( node.getMetadata() != null && node.getMetadata() instanceof N5DatasetMetadata )
			{
				final String convSuffix = conversionSuffix( node );
				final String conversionString;
				if ( showConversionWarning && !convSuffix.isEmpty() )
					conversionString = " " + String.format( warningFormat, conversionSuffix( node ) );
				else
					conversionString = "";

			    final String memStr = memString( node );
			    final String memSizeString = memStr.isEmpty() ? "" : " (" + memStr + ")";
			    final String name = node.getParent() == null ? rootName : node.getNodeName();

				setText( String.join( "", new String[]{
						"<html>",
						String.format( nameFormat, name ),
						" (",
						getParameterString( node ),
						conversionString,
						")",
						memSizeString,
						"</html>"
				}));
			}
			else
			{
				setText(node.getParent() == null ? rootName : node.getNodeName());
			}
		}
		return this;
    }

	public static String conversionSuffix( final N5TreeNode node ) {

	  DataType type;
	  final N5Metadata meta = node.getMetadata();
	  if ( meta != null && meta instanceof N5DatasetMetadata )
		type = ((N5DatasetMetadata)node.getMetadata()).getAttributes().getDataType();
	  else
		return "";

	  if ( node.getMetadata() instanceof N5ImagePlusMetadata ) {
		  final N5ImagePlusMetadata ijMeta = (N5ImagePlusMetadata)node.getMetadata();
		  if( ijMeta.getType() == ImagePlus.COLOR_RGB && type == DataType.UINT32 )
			  return "(RGB)";
	  }

	  if (type == DataType.FLOAT64) {
		return "&#x2192; 32-bit";
	  } else if (type == DataType.INT8) {
		return "&#x2192; 8-bit";
	  } else if (type == DataType.INT32 || type == DataType.INT64 ||
				type == DataType.UINT32 || type == DataType.UINT64 ||
				type == DataType.INT16 )
		{
			return "&#x2192; 16-bit";
		}
		return "";
	}

	public String getParameterString(final N5TreeNode node) {

		final N5Metadata meta = node.getMetadata();
		if (meta == null || !(meta instanceof N5DatasetMetadata))
			return "";

		final DatasetAttributes attributes = ((N5DatasetMetadata)node.getMetadata()).getAttributes();
		final String[] dimStrArr = Arrays.stream(attributes.getDimensions()).mapToObj(d -> Long.toString(d)).toArray(n -> new String[n]);

		if (OmeNgffMultiScaleMetadata.fOrder(attributes))
			ArrayUtils.reverse(dimStrArr);

		return String.join(dimDelimeter, dimStrArr) + ", " + attributes.getDataType();
	}

	protected String memString( final N5TreeNode node )
	{
	    final N5Metadata meta = node.getMetadata();
	    if ( meta == null || !(meta instanceof N5DatasetMetadata ) )
		  return "";

		final DatasetAttributes attributes = ((N5DatasetMetadata)node.getMetadata()).getAttributes();
		final long nBytes = estimateBytes(attributes);
		if( nBytes < 0)
			return "";
		else
			return humanReadableByteCountSI(nBytes);
	}

	/*
	 * https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
	 */
	protected static String humanReadableByteCountSI(long bytes) {
	    if (-1000 < bytes && bytes < 1000) {
	        return bytes + " B";
	    }
	    final CharacterIterator ci = new StringCharacterIterator("kMGTPE");
	    while (bytes <= -999_950 || bytes >= 999_950) {
	        bytes /= 1000;
	        ci.next();
	    }
	    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	private long estimateBytes( final DatasetAttributes attrs )
	{
		final long N = Arrays.stream( attrs.getDimensions() ).reduce( 1, (i,v) -> i*v );
		final String typeString = attrs.getDataType().toString();
		long nBytes = -1;
		if( typeString.endsWith( "8" ))
			nBytes = N;
		else if( typeString.endsWith( "16" ))
			nBytes = N*2;
		else if( typeString.endsWith( "32" ))
			nBytes = N*4;
		else if( typeString.endsWith( "64" ))
			nBytes = N*8;

		return nBytes;
	}

}
