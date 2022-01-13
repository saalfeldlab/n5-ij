package org.janelia.saalfeldlab.n5.ui;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.metadata.N5DatasetMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;
import org.janelia.saalfeldlab.n5.metadata.imagej.N5ImagePlusMetadata;

import ij.ImagePlus;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.stream.Collectors;

public class N5DatasetTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = -4245251506197982653L;

	protected static final String thinSpace = "&#x2009;";

//	private static final String times = "&#x2715;";
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

	public void setRootName( String rootName ) {
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
			if ( node.getMetadata() != null )
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
	  N5Metadata meta = node.getMetadata();
	  if ( meta != null && meta instanceof N5DatasetMetadata )
		type = ((N5DatasetMetadata)node.getMetadata()).getAttributes().getDataType();
	  else
		return "";

	  if ( node.getMetadata() instanceof N5ImagePlusMetadata ) {
		  N5ImagePlusMetadata ijMeta = (N5ImagePlusMetadata)node.getMetadata();
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

	public String getParameterString( final N5TreeNode node ) {

	  N5Metadata meta = node.getMetadata();
	  if ( meta == null || !(meta instanceof N5DatasetMetadata ) )
		return "";

	  final DatasetAttributes attributes = ((N5DatasetMetadata)node.getMetadata()).getAttributes();
	  final String dimString = String.join(dimDelimeter,
			  Arrays.stream(attributes.getDimensions())
					  .mapToObj(d -> Long.toString(d))
					  .collect(Collectors.toList()));

	  return dimString + ", " + attributes.getDataType();
	}

	protected String memString( N5TreeNode node )
	{
	    N5Metadata meta = node.getMetadata();
	    if ( meta == null || !(meta instanceof N5DatasetMetadata ) )
		  return "";

		final DatasetAttributes attributes = ((N5DatasetMetadata)node.getMetadata()).getAttributes();
		long nBytes = estimateBytes(attributes);
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
	    CharacterIterator ci = new StringCharacterIterator("kMGTPE");
	    while (bytes <= -999_950 || bytes >= 999_950) {
	        bytes /= 1000;
	        ci.next();
	    }
	    return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}

	private long estimateBytes( DatasetAttributes attrs )
	{
		long N = Arrays.stream( attrs.getDimensions() ).reduce( 1, (i,v) -> i*v );
		String typeString = attrs.getDataType().toString();
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
