package farsight.utils.config;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.wm.data.IData;
import com.wm.data.IDataCursor;

public class ConfigurationStore implements Configuration {

	private LinkedHashMap<String, String> store = new LinkedHashMap<String, String>();
	
	@Override
	public Configuration fill(IData source, int depth) {
		fill(source, depth, null);
		return this;
	}
	
	private void fill(IData data, int depth, String prefix) {
		if(depth == 0) return;
		IDataCursor c = data.getCursor();
		while(c.next()) {
			String key = prefix == null ? c.getKey() : prefix + "." + c.getKey();
			Object value = c.getValue();
			if(value instanceof String) {
				put(key, (String)value);
			} else if(value instanceof IData) {
				fill((IData)value, depth < 0 ? -1: depth - 1, key);
			}
		}
	}
	
	@Override
	public Configuration fill(String[] keyValues) {
		for(int i = 0; i < keyValues.length - 1; i += 2) {
			put(keyValues[i], keyValues[i + 1]);
		}
		return this;
	}
	
	@Override
	public String get(String key, String defaultValue) {
		String value = get(key);
		return value == null ? defaultValue : value;
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder("[ConfigurationStore:\n");
		for(java.util.Map.Entry<String, String> set: entrySet()) {
			b.append("\t" + set.getKey() + ": " + set.getValue()+ "\n");
		}
		b.append("]\n");
		return b.toString();
	}

	@Override
	public String get(String key) {
		return store.get(key);
	}

	@Override
	public boolean containsKey(String key) {
		return store.containsKey(key);
	}

	@Override
	public String remove(String key) {
		return store.remove(key);
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return store.entrySet();
	}

	@Override
	public void put(String key, String value) {
		store.put(key, value);		
	}
	
}
 