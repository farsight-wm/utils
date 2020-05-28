package farsight.utils.config;

import java.util.Map;
import java.util.Set;

import com.wm.data.IData;

public interface Configuration {

	Configuration fill(IData source, int depth);
	Configuration fill(String[] keyValues);

	String get(String key, String defaultValue);
	String get(String key);
	void put(String key, String value);
	String remove(String key); 

	boolean containsKey(String key);

	Set<Map.Entry<String, String>> entrySet();

}