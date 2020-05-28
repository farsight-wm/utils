package farsight.utils.idata;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;
import com.wm.util.coder.IDataCoder;
import com.wm.util.coder.IDataXMLCoder;

public class DataBuilder {
	
	private static final char DEFAULT_PATH_SEPARATOR = '/';
	
	private final IData root;
	private char defaultSeparator;
	
	public DataBuilder() {
		this(IDataFactory.create());
	}
	
	public DataBuilder(IData data) {
		this(data, DEFAULT_PATH_SEPARATOR);
	}

	public DataBuilder(IData data, char defaultSeparator) {
		this.root = data == null ? IDataFactory.create() : data;
		this.defaultSeparator = defaultSeparator;
	}
	
	public static DataBuilder create() {
		return new DataBuilder();
	}
	
	public static DataBuilder wrap(IData data) {
		return new DataBuilder(data);
	}
	
	// internal helpers
	
	private static IData cloneIData(IData source, boolean deepClone) {
		if(deepClone) {
			try {
				return IDataUtil.deepClone(source);
			} catch (IOException e) {
				//IDataUtil uses (de-)serialization for deep cloning
				throw new RuntimeException("Deep cloning failed, this should not happen!", e);
			}
		} else {
			return IDataUtil.clone(source);
		}
	}
	
	private <T> T defaultValue(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	private Class<?> getArrayTypeFor(Object value) {
		if(value instanceof String) {
			return String.class;
		} else 
			return Object.class;
	}

	private <T> T cast(Object o, Class<T> type) {
		if(o == null)
			return null;
		if(type.isAssignableFrom(o.getClass()))
			return type.cast(o);
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> T[] enlargeArray(T[] src, int size) {
		T[] enlarged = (T[]) Array.newInstance(src.getClass().getComponentType(), size);
		System.arraycopy(src, 0, enlarged, 0, src.length);
		return enlarged;
	}

	private void setItem(IData src, String segment, Object value) {
		String key;
		int index = -1;
		int p = segment.indexOf('[');
		if(p == -1) {
			key = segment;
		} else {
			key = segment.substring(0, p);
			index = Integer.parseInt(segment.substring(p + 1, segment.length() - 1), 10);
		}
		
		IDataCursor c = src.getCursor();
		if(index == -1) {
			findSpot(c, key, true);
			c.setValue(value);
		} else {
			Class<?> type = getArrayTypeFor(value);
			if(findArrayTypedSpot(c, key, type, true)) {
				//array of correct type found
				Object[] array = (Object[]) c.getValue();
				if(array.length > index) {
					array[index] = value;
				} else {
					Object[] enlarged = enlargeArray(array, index + 1);
					enlarged[index] = value;
					c.setValue(enlarged);
				}
			} else {
				Object[] array = (Object[]) Array.newInstance(type, index + 1);
				array[index] = value;
				c.setValue(array);
			}
		}
	}

	private Object getItem(IData src, String segment) {
		String key;
		int index = -1;
		int p = segment.indexOf('[');
		if(p == -1) {
			key = segment;
		} else {
			key = segment.substring(0, p);
			index = Integer.parseInt(segment.substring(p + 1, segment.length() - 1), 10);
		}
		
		IDataCursor c = src.getCursor();
		if(index == -1) {
			return findSpot(c, key, false) ? c.getValue() : null;
		} else {
			if(findTypedSpot(c, key, Object[].class, false)) {
				Object[] array = (Object[]) c.getValue();
				if(array.length > index)
					return array[index];
			}
			return null;
		}
	}

	private IData getNextSegemnt(IData src, String segment, boolean create) {
		String key;
		int index = -1;
		int p = segment.indexOf('[');
		if(p == -1) {
			key = segment;
		} else {
			key = segment.substring(0, p);
			index = Integer.parseInt(segment.substring(p + 1, segment.length() - 1), 10);
		}
		return index == -1 ? getChild(src, key, create) : getChildItem(src, key, index, create);
	}

	private IData getChildItem(IData data, String key, int index, boolean create) {
		IDataCursor c = data.getCursor();
		if(findTypedSpot(c, key, IData[].class, create)) {
			//array already exists
			IData[] array = (IData[]) c.getValue();
			if(array.length > index) {
				//index is present
				IData res = array[index];
				if(create && res == null) {
					res = IDataFactory.create();
					array[index] = res;
				}
				return res;
			} else {
				//index not present
				if(create) {
					//enlarge array
					IData[] enlarged = new IData[index + 1];
					System.arraycopy(array, 0, enlarged, 0, array.length);
					c.setValue(enlarged);
					return enlarged[index] = IDataFactory.create();
				}
				return null;
			}
		} else {
			//array does not exist
			if(create) {
				IData[] array = new IData[index + 1];
				c.setValue(array);
				return array[index] = IDataFactory.create();
			}
			return null;
		}
	}

	private IData getChild(IData data, String key, boolean create) {
		IDataCursor c = data.getCursor();
		if(findTypedSpot(c, key, IData.class, create))
			return (IData) c.getValue();
		if(create) {
			IData child = IDataFactory.create();
			c.setValue(child);
			return child;
		} else {
			return null;
		}
	}

	private boolean findSpot(IDataCursor c, String key, boolean create) {
		if(c.first(key)) {
			return true;
		} else {
			if(create) {
				c.last();
				c.insertAfter(key, null);
				return true;
			}
			return false;
		}
	}

	private boolean findTypedSpot(IDataCursor c, String key, Class<?> type, boolean create) {
		if(findSpot(c, key, create)) {
			Object value = c.getValue();
			return value != null && type.isAssignableFrom(value.getClass());
		}
		return false;
	}

	private boolean findArrayTypedSpot(IDataCursor c, String key, Class<?> type, boolean create) {
		if(findSpot(c, key, create)) {
			Object value = c.getValue();
			return value != null && (value instanceof Object[]) && type.isAssignableFrom(value.getClass().getComponentType());
		}
		return false;
	}
	
	
	// clone utils

	public DataBuilder asClone() {
		return asClone(false);
	}
	
	public DataBuilder asClone(boolean deep) {
		return wrap(cloneIData(root, deep));
	}
	
	// IData access
	
	public IData build() {
		return root;
	}
	
	public IData getIData() {
		return build();
	}
	
	// nested access
	
	public DataBuilder withDefaultSeparator(char defaultSeparator) {
		this.defaultSeparator = defaultSeparator;
		return this;
	}
	
	public Object read(String path) {
		return read(path, defaultSeparator);
	}
	
	public Object read(String path, Object defaultValue) {
		return defaultValue(read(path), defaultValue);
	}
	
	public <T> T read(String path, Class<T> type) {
		return cast(read(path), type);
	}

	public <T> T read(String path, Class<T> type, T defaultValue) {
		return defaultValue(read(path, type), defaultValue);
	}

	public <T> T read(String path, Class<T> type, char separator) {
		return cast(read(path, separator), type);
	}
	
	public <T> T read(String path, Class<T> type, T defaultValue, char separator) {
		return defaultValue(read(path, type, separator), defaultValue);
	}

	
	public Object read(String path, char separator) {
		int offset = 0, pos = path.indexOf(separator);
		IData cur = root;
		while(pos > 0) {
			String segment = path.substring(offset, pos);
			cur = getNextSegemnt(cur, segment, false);
			if(cur == null)
				return null;
			
			//next segment
			offset = pos + 1;
			pos = path.indexOf(separator, offset);
		}
		return getItem(cur, path.substring(offset));
	}
	
	
	public DataBuilder insert(String path, Object value) {
		return insert(path, value, defaultSeparator);
	}
	
	public DataBuilder insert(String path, Object value, char separator) {
		int offset = 0, pos = path.indexOf(separator);
		IData cur = root;
		while(pos > 0) {
			String segment = path.substring(offset, pos);
			cur = getNextSegemnt(cur, segment, true);
			
			//next segment
			offset = pos + 1;
			pos = path.indexOf(separator, offset);
		}
		setItem(cur, path.substring(offset), value);
		return this;
	}
	
	public DataBuilder put(String key, Object value) {
		IDataCursor c = root.getCursor();
		if(c.first(key)) {
			c.setValue(value);
		} else {
			c.last();
			c.insertAfter(key, value);
		}
		return this;
	}
	
	public DataBuilder put(Map<String, ? extends Object> values) {
		for(Entry<String, ? extends Object> set: values.entrySet()) {
			put(set.getKey(), set.getValue());
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public DataBuilder putRecursive(Map<String, ? extends Object> values) {
		for(Entry<String, ? extends Object> set: values.entrySet()) {
			Object o = set.getValue();
			if(o instanceof Map) {
				try {
					o = create().putRecursive((Map<String, ? extends Object>) o).build();
				} catch (Exception e) {
					//ignore and put o directly 
				}
			}
			put(set.getKey(), o);
		}
		return this;
	}
	
	public boolean containsKey(String key) {
		return root.getCursor().first(key);
	}
	
	public boolean containsPath(String path) {
		return containsPath(path, defaultSeparator);
	}

	public boolean containsPath(String path, char separator) {
		return read(path, separator) != null;
	}
	
	public Object get(String key) {
		IDataCursor c = root.getCursor();
		if(c.first(key))
			return c.getValue();
		return null;
	}
	
	public Object get(String key, Object defaultValue) {
		return defaultValue(get(key), defaultValue);
	}	
	
	public <T> T get(String key, Class<T> type) {
		return cast(get(key), type);
	}
	
	public <T> T get(String key, T defaultValue, Class<T> type) {
		return defaultValue(get(key, type), defaultValue);
	}
	
	public void remove(String key) {
		IDataCursor c = root.getCursor();
		while(c.first(key))
			c.delete();
	}

	public void removeValue(Object value) {
		IDataCursor c = root.getCursor();
		if(c.first()) while(c.getValue() == value ? c.delete() : c.next());
	}

	public DataBuilder replace(IData root) {
		clear();
		IDataCursor into = this.root.getCursor(),
				from = root.getCursor();
		while(from.next()) {
			into.insertAfter(from.getKey(), from.getValue());
		}
		return this;
	}

	public DataBuilder clear() {
		IDataCursor c = root.getCursor();
		c.first();
		while(c.delete());
		return this;
	}

	public DataBuilder filter(String... keys) {
		HashSet<String> preserve = new HashSet<>();
		preserve.addAll(Arrays.asList(keys));
		IDataCursor c = root.getCursor();
		//if has at all data, keep iterating or deleting
		if(c.first()) while(preserve.contains(c.getKey()) ? c.next() : c.delete());
		return this;
	}

	public DataBuilder merge(IData other, boolean dominant) {
		if(dominant) {
			IDataUtil.merge(other, root);
		} else { 
			IDataUtil.merge(root, other);
			replace(other);
		}
		return this;
	}
	
	// debug utils
	
	public DataBuilder dump(OutputStream os, IDataCoder coder) throws IOException {
		coder.encode(os, root);
		return this;
	}
	
	public DataBuilder dump(OutputStream os) throws IOException {
		return dump(os, new IDataXMLCoder("UTF-8"));
	}
	
	public DataBuilder dump() {
		try {
			dump(System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public String toString() {
		return "DataBuilder:\n" + root.toString() + "\n]\n";
	}

	public boolean isEmpty() {
		return !root.getCursor().first();
	}

}
