package farsight.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.wm.app.b2b.server.ns.Namespace;
import com.wm.lang.ns.NSName;
import com.wm.lang.ns.NSNode;
import com.wm.lang.ns.NSPackage;

/**
 * This class provides the forgotten getters for efficiently interacting with
 * IS's namespace.
 * 
 * It provides three unmodifiably map views to important static Maps:
 * <ul>
 * <li><code>public static final Map<NSName, NSNode> nodes;</code></li>
 * <li><code>public static final Map<NSName, NSPackage> packages;</code></li>
 * <li><code>public static final Map<String, NSName> nsnames;</code></li>
 * </ul>
 * All maps are public final
 * 
 * Its save but a bit cheated ;)
 * 
 * Since SAG does not provide access to the ConcurrentHashMaps from the
 * Namespace, this static class gets (unmodifiable) versions of them via
 * Reflection.
 */
public class NamespaceUtils {
	
	// -- static data --

	public static final Map<NSName, NSNode> nodes;
	public static final Map<String, NSPackage> packages;
	public static final Map<String, NSName> nsnames;

	static {
		nodes = getUnmodifiableMap("nodes");
		packages = getUnmodifiableMap("packages");
		nsnames = getUnmodifiableNames();
	}

	@SuppressWarnings("unchecked")
	private static <T1, T2> Map<T1, T2> getUnmodifiableMap(String field) {
		try {
			Object o = ReflectionUtils.getFieldIfExistent(field, Namespace.current());
			return Collections.unmodifiableMap(Map.class.cast(o));
		} catch (Exception e) {
			// too bad - but should not happen if this class is loaded after
			// class Namespace!
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, NSName> getUnmodifiableNames() {
		try {
			return Collections.unmodifiableMap(
					(Map<String, NSName>) ReflectionUtils.getStaticFieldIfExistent("names", NSName.class));
		} catch (Exception e) {
			// should not happen
			return null;
		}
	}
	
	// -- convenience API --

	public static NSName getNSNameIfExistent(String name) {
		return nsnames.get(name);
	}

	public static boolean existsNSName(String name) {
		return nsnames.containsKey(name);
	}
	
	public static NSNode getNode(String name) {
		NSName nsname = getNSNameIfExistent(name);
		return nsname == null ? null : nodes.get(nsname);
	}
	
	public static NSPackage getPackage(String name) {
		return packages.get(name);
	}


	// -- experimental API --

	private abstract static class LookupIterator<Ti, To> implements Iterator<To> {
		private final Iterator<Ti> src;
		private To next = null;
		public LookupIterator(Iterator<Ti> src) {
			this.src = src;
			next();
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public To next() {
			To current = this.next;
			To next = null;
			while(src.hasNext() && next == null)
				next = filter(src.next());
			this.next = next;
			return current;
		}

		protected abstract To filter(Ti in);
	}
	
	public static <T extends NSNode> Iterable<T> getNodes(Class<T> type) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new LookupIterator<NSNode, T>(nodes.values().iterator()) {
					protected T filter(NSNode in) {
						return type.isInstance(in) ? type.cast(in) : null;
					}
				};
			}
		};
	}
	
	public static <T extends NSNode> Iterable<T> getNodes(Class<T> type, String pkgName) {
		if(pkgName == null)
			return getNodes(type);
		NSPackage pkg = getPackage(pkgName);
		if(pkg == null)
			return Collections.emptyList();
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new LookupIterator<NSNode, T>(nodes.values().iterator()) {
					protected T filter(NSNode in) {
						return type.isInstance(in) && in.getPackage() == pkg ? type.cast(in) : null;
					}
				};
			}
		};
	}
	


	


}
