package org.janelia.saalfeldlab.n5.ui;

import java.awt.Component;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5TreeNode.JTreeNodeWrapper;
import org.janelia.saalfeldlab.n5.metadata.N5ImagePlusMetadata;

import ij.ImagePlus;

public class N5DatasetTreeCellRenderer extends DefaultTreeCellRenderer
{
	private static final long serialVersionUID = -4245251506197982653L;

	private static final String thinSpace = "&#x2009;";

//	private static final String times = "&#x2715;";
	private static final String times = "&#xd7;";

	private static final String warningFormat = "<font color=\"rgb(179, 58, 58)\">%s</font>";

	private static final String nameFormat = "<b>%s</b>";

	private static final String dimDelimeter = thinSpace + times + thinSpace;

	private final boolean showConversionWarning;

	public N5DatasetTreeCellRenderer( final boolean showConversionWarning )
	{
		this.showConversionWarning = showConversionWarning;
	}

	@Override
	public Component getTreeCellRendererComponent( final JTree tree, final Object value,
			final boolean sel, final boolean exp, final boolean leaf, final int row, final boolean hasFocus )
	{

		super.getTreeCellRendererComponent( tree, value, sel, exp, leaf, row, hasFocus );

		N5TreeNode node;
		if ( value instanceof JTreeNodeWrapper )
		{
			node = ( ( JTreeNodeWrapper ) value ).getNode();
			if ( node.getMetadata() != null )
			{
				final String conversionString;
				final String convSuffix = conversionSuffix( node );
				if ( showConversionWarning && !convSuffix.isEmpty() )
					conversionString = " " + String.format( warningFormat, conversionSuffix( node ) );
				else
					conversionString = "";

				setText( String.join( "", new String[]{
						"<html>",
						String.format( nameFormat, node.getNodeName() ),
						" (",
						getParameterString( node ),
						conversionString,
						")</html>"
				}));
			}
		}
		return this;
    }

	public static String conversionSuffix( final N5TreeNode node )
	{
		DataType type;
		if ( node.getMetadata() != null )
			type = node.getMetadata().getAttributes().getDataType();
		else
			return "";

		if ( node.getMetadata() instanceof N5ImagePlusMetadata )
		{
			N5ImagePlusMetadata ijMeta = (N5ImagePlusMetadata)node.getMetadata();
			if( ijMeta.getType() == ImagePlus.COLOR_RGB && type == DataType.UINT32 )
				return "(RGB)";
		}

		if ( type == DataType.FLOAT64 )
		{
			return "&#x2192; 32-bit";
		}
		else if( type == DataType.INT8 )
		{
			return "&#x2192; 8-bit";
		}
		else if ( type == DataType.INT32 || type == DataType.INT64 ||
				type == DataType.UINT32 || type == DataType.UINT64 ||
				type == DataType.INT16 )
		{
			return "&#x2192; 16-bit";
		}
		return "";
	}

	public String getParameterString( final N5TreeNode node )
	{
		if( node.getMetadata() == null )
			return "";

		final DatasetAttributes attributes = node.getMetadata().getAttributes();
		final String dimString = String.join( dimDelimeter,
				Arrays.stream(attributes.getDimensions())
					.mapToObj( d -> Long.toString( d ))
					.collect( Collectors.toList() ) );
		return  dimString + ", " + attributes.getDataType();
	}

}
