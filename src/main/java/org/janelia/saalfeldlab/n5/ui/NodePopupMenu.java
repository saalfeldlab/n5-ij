package org.janelia.saalfeldlab.n5.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Type;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.janelia.saalfeldlab.n5.N5TreeNode;
import org.janelia.saalfeldlab.n5.N5TreeNode.JTreeNodeWrapper;
import org.janelia.saalfeldlab.n5.metadata.N5Metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.imglib2.realtransform.AffineTransform3D;

public class NodePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1431304870893536697L;

	protected final DatasetSelectorDialog n5SelectionDialog;

	protected final JMenuItem showMetadata;

	protected final PopupListener popupListener;

	private Point clickPt;

	private JTree tree;

	private JFrame metadataFrame;

	private JTextArea metadataTextArea;

	private final Gson gson;

	private final ObjectMapper objMapper;

	private final DefaultPrettyPrinter prettyPrinter;

	public NodePopupMenu(final DatasetSelectorDialog n5SelectionDialog) {

		this.n5SelectionDialog = n5SelectionDialog;
		this.tree = n5SelectionDialog.getJTree();

		popupListener = new PopupListener();

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(AffineTransform3D.class, new NodePopupMenu.AffineTransform3DGsonAdapter());
		gson = gsonBuilder.create();
		objMapper = new ObjectMapper();
		objMapper.enable(SerializationFeature.INDENT_OUTPUT);

		prettyPrinter = new DefaultPrettyPrinter();
		DefaultPrettyPrinter.Indenter i = new DefaultIndenter("  ", "\n");
		prettyPrinter.indentArraysWith(i);
		prettyPrinter.indentObjectsWith(i);
		objMapper.setDefaultPrettyPrinter(prettyPrinter);

		showMetadata = new JMenuItem("Show Metadata");
		showMetadata.addActionListener(e -> showDialog());
		add(showMetadata);

		buildMetadataFrame();
	}
	
	public PopupListener getPopupListener() {
		return popupListener;
	}

	public void setupListeners() {
		n5SelectionDialog.getJTree().addMouseListener(popupListener);
	}
	
	public void showDialog()
	{
        if( popupListener.selPath != null ) {
        	System.out.println( popupListener.selPath.getPath());
        	Object o = popupListener.selPath.getLastPathComponent();
        	if( o instanceof N5TreeNode )
        	{
        		final N5TreeNode node = (N5TreeNode)o;
        		setText( node );
        	}
        	else if ( o instanceof JTreeNodeWrapper )
        	{
        		final JTreeNodeWrapper wrapper = (JTreeNodeWrapper)o;
        		setText( wrapper.getNode());
        	}
			else
				System.out.println( o.getClass());
        }
        metadataFrame.setVisible(true);
	}
	
	public void setText(final N5TreeNode node) {
		N5Metadata meta = node.getMetadata();
		String jsonTxt = gson.toJson(node.getMetadata());
		String jsonPretty = jsonTxt;
		try {
			HashMap tmpObj = objMapper.readValue( jsonTxt, HashMap.class );
			jsonPretty = objMapper.writer(prettyPrinter).writeValueAsString(tmpObj);
		} catch (JsonMappingException e) { } catch (JsonProcessingException e) { }
		
		System.out.println(jsonPretty);
		metadataTextArea.setText(jsonPretty);
	}

	public class PopupListener extends MouseAdapter {

		TreePath selPath;

		public void mousePressed(MouseEvent e) {

			if( SwingUtilities.isRightMouseButton(e)) {
				clickPt = e.getPoint();
				Component c = e.getComponent();

		        selPath = tree.getPathForLocation(e.getX(), e.getY());
				NodePopupMenu.this.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	public JFrame buildMetadataFrame()
	{
		metadataFrame = new JFrame("Metadata");
		metadataFrame.setPreferredSize(new Dimension( 400, 400 ));
		metadataFrame.setMinimumSize(new Dimension( 200, 200 ));

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add( new JLabel("Metadata"));

		metadataTextArea = new JTextArea();
		metadataTextArea.setEditable( false );

		final JScrollPane textView = new JScrollPane( metadataTextArea );
		panel.add( textView, BorderLayout.CENTER );

		metadataFrame.add(panel);
        return metadataFrame;
	}
	
	public static class AffineTransform3DGsonAdapter implements JsonDeserializer<AffineTransform3D>, JsonSerializer<AffineTransform3D> {

		@Override
		public JsonElement serialize(AffineTransform3D src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.add("matrix", context.serialize(src.getRowPackedCopy()));
			return obj;
		}

		@Override
		public AffineTransform3D deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			final AffineTransform3D affine = new AffineTransform3D();
			final double[] mtx = context.deserialize( json.getAsJsonObject().get("a"), double[].class );
			affine.set(mtx);
			return affine;

		}
		
	}

}
