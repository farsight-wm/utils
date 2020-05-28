package farsight.utils.config.xml;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class AbstractXMLCodableComponent<T extends AbstractXMLCodableComponent<T>> {
	
	public abstract void encodeTo(XMLStreamWriter w) throws XMLStreamException;
	
	/* encode utils */
	
	protected void writeAttribute(XMLStreamWriter w, String name, String value) throws XMLStreamException {
		if(value != null)
			w.writeAttribute(name, value);
	}
	
    protected static Node firstChild(Node node, String name) {
    	if(node == null)
    		return null;
    	NodeList children = node.getChildNodes();
    	int len = children.getLength();
    	for(int i = 0; i < len; i++) {
    		Node child = children.item(i);
    		if(child.getNodeType() == Node.ELEMENT_NODE && (name == null || name.equals(child.getLocalName())))
    			return child;
    	}
    	return null;
    }
    
    protected static void vistChildElements(Node node, Consumer<Node> visitor) {
    	vistChildElements(node, null, visitor);
    }
    
    protected static void vistChildElements(Node node, String name, Consumer<Node> visitor) {
    	if(node == null)
    		return;
    	NodeList children = node.getChildNodes();
    	int len = children.getLength();
    	for(int i = 0; i < len; i++) {
    		Node child = children.item(i);
    		if(child.getNodeType() == Node.ELEMENT_NODE  && (name == null || name.equals(child.getLocalName())))
    			visitor.accept(child);
    	}
    }
    
    
    protected static String getAttribute(NamedNodeMap attrs, String key, String defaultValue) {
    	Node value = attrs.getNamedItem(key);
    	return value == null ? defaultValue : value.getTextContent(); 
    }
    
    protected static String getAttribute(NamedNodeMap attrs, String key) {
    	return getAttribute(attrs, key, null);
    }
    

}
