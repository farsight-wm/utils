package farsight.utils;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

public class ManifestUtils {
	
	public static Manifest getManifest(Class<?> clazz) throws IOException {
		String className = clazz.getName().replace('.', '/') + ".class";
		String classPath = clazz.getClassLoader().getResource(className).toString();
		if(classPath.startsWith("jar")) {
			String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) +
					"/META-INF/MANIFEST.MF";
			return new Manifest(new URL(manifestPath).openStream());
		} else {
			return null;
		}
	}
	
	public static String getAttribute(Manifest manifest, String key) {
		return manifest.getMainAttributes().getValue(key);
	}

}
