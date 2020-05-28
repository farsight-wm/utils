package farsight.utils.config.xml;

import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.wm.data.IData;

import farsight.utils.config.Configuration;
import farsight.utils.config.ConfigurationStore;

public class XMLCodableKeyValueComponent extends AbstractXMLCodableComponent<XMLCodableKeyValueComponent> implements Configuration {
	
	private final Configuration store;
	private final String name;
	
	public XMLCodableKeyValueComponent(String name) {
		this.store = new ConfigurationStore();
		this.name = name;
	}
	
	public XMLCodableKeyValueComponent(String string, Configuration store) {
		this.name = string;
		this.store = store;
	}

	@Override
	public void encodeTo(XMLStreamWriter w) throws XMLStreamException {
		w.writeStartElement(name);
		for(Entry<String, String> set: store.entrySet()) {
			String key = set.getKey();
			String value = set.getValue();
			
			if(key != null && !key.equals("") && value != null) {
				w.writeEmptyElement("value");
				w.writeAttribute("key", set.getKey());
				w.writeAttribute("value", value);
			}
		}
		w.writeEndElement();
	}
	
	public static XMLCodableComponentDecoder<XMLCodableKeyValueComponent> getDecoder() {
		return new XMLCodableComponentDecoder<XMLCodableKeyValueComponent>() {
			@Override
			public XMLCodableKeyValueComponent decodeFrom(Node element) {
				String name = element.getLocalName();
				final XMLCodableKeyValueComponent component = new XMLCodableKeyValueComponent(name);

				vistChildElements(element, "value", child -> {
					NamedNodeMap attrs = child.getAttributes();
					String key = getAttribute(attrs, "key");
					String value = getAttribute(attrs, "value");
					component.put(key, value);
				});
				
				
				return component;
			}
		};
	}
	
	@Override
	public boolean containsKey(String key) {
		return store.containsKey(key);
	}

	@Override
	public void put(String key, String value) {
		store.put(key, value);
	}
	
	@Override
	public String get(String key) {
		return store.get(key, key);
	}
	
	@Override
	public String get(String key, String defaultValue) {
		return store.get(key, defaultValue);
	}

	@Override
	public String remove(String key) {
		return store.remove(key);
	}

	@Override
	public Configuration fill(IData source, int depth) {
		store.fill(source, depth);
		return this;
	}

	@Override
	public Configuration fill(String[] keyValues) {
		store.fill(keyValues);
		return this;
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return store.entrySet();
	}

}
