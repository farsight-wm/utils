package farsight.utils.config.xml;

import org.w3c.dom.Node;

public interface XMLCodableComponentDecoder<T extends AbstractXMLCodableComponent<T>> {
	
	public abstract T decodeFrom(Node element);

}
