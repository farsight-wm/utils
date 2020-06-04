package farsight.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lightweight property loader.
 * 
 * May be exchanged for more complex systems e.g. Apache Commons Configuration later.
 */
public class PropertyLoader {
	
	public static class Builder {
		
		private Properties properties = new Properties();
		
		private Builder() {
			
		}
		
		private void add(InputStream is, String path, boolean mandatory) {
			if(is == null && mandatory) {
				throw new RuntimeException("Mandatory configuariton file not found: " + path);
			}
			
			try {
				Properties toAdd = new Properties();
				toAdd.load(is);				
				properties.putAll(toAdd);
			} catch (Exception e) {
				if(mandatory)
					throw new RuntimeException("Failed to load mandatory configuration: " + path, e);
			}
		}

		public Builder addClasspath(String path, boolean mandatory) {
			add(PropertyLoader.class.getClassLoader().getResourceAsStream(path), path, mandatory);
			return this;
		}

		public Builder addFile(String path, boolean mandatory) {
			Path p = Paths.get(path);
			if(Files.exists(p) && Files.isReadable(p)) {
				try {
					add(Files.newInputStream(p), path, mandatory);
				} catch (IOException e) {
					throw new RuntimeException("Cannot open file: " + path, e);
				}
			} else if(mandatory) {
				throw new RuntimeException("Cannot open file: " + path);
			}
			return this;
		}

		public PropertyLoader build() {
			return new PropertyLoader(properties);
		}
		
	}
	
	private final Properties properties;

	protected PropertyLoader(Properties properties) {
		this.properties = properties;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static String getSystemProperty(String propertyName, String defaultValue) {
		return System.getProperty(propertyName, defaultValue);
	}

	public String getString(String key) {
		return properties.getProperty(key);
	}

	public int getInt(String key) {
		String value = properties.getProperty(key);
		return Integer.valueOf(value);
	}
	
	
	

}
