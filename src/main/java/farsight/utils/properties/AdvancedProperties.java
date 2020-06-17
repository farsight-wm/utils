package farsight.utils.properties;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AdvancedProperties {
	protected static enum NodeType { EMPTY_LINE, COMMENT, VALUE, GROUP }
	private static final String LINE_SEPARATOR = System.lineSeparator();
	private static final String KEY_SEPARATOR = "=";
	private static final char PATH_SEPARATOR = '.';
	
	@FunctionalInterface
	public static interface PropertyTreeConsumer {
		public void accept(String key, String path, String value);
	}
	
	public static class Parser {
		
		private final StreamTokenizer tokenizer;
		private boolean isEOF = false;
		private boolean isLineBreak = false;
		private boolean nextESC = false, isESC = false;
		private final StringBuilder buf = new StringBuilder();
		private final AdvancedProperties properties;
		
		public Parser(Reader in, AdvancedProperties properties) {
			this.properties = properties;
			tokenizer = new StreamTokenizer(in);
			tokenizer.resetSyntax();
			tokenizer.wordChars(0, 255);
			for(char c: new char[] {'#', '!', ':', '=', ' ', '\t', '\r', '\n', '\\'})
				tokenizer.ordinaryChar(c);
		}

		private void next() throws IOException {
			isESC = nextESC;
			isEOF = tokenizer.nextToken() == StreamTokenizer.TT_EOF;
			isLineBreak = tokenizer.ttype == '\r' || tokenizer.ttype == '\n';
			nextESC = tokenizer.ttype == '\\' && !isESC;
		}
		
		private boolean is(char ch) {
			return !isESC && tokenizer.ttype == ch;
		}
		
		public AdvancedProperties parse() throws IOException {
			next();
			while(!isEOF)
				parseLine();
			return properties;
		}

		private void parseLine() throws IOException{
			//ignore whitespace
			while(!isEOF && (is(' ') || is('\t')))
				next();
			
			if(is('#') || is('!')) { //is comment
				parseComment();
			} else if(isLineBreak) { //is empty line
				parseLineBreak();
				properties.putEmptyLine();
			} else {
				parseAssignment(); // is assignment
			}
		}

		private void parseAssignment() throws IOException {
			//key
			while(!isEOF && !(is(' ') || is (':') || is('=')))
				consume();
			String path = flush();
			//separator
			parseSeparator();
			//value
			String value = consumeLine(true);
			properties.put(path, value);
		}
		
		private void parseComment() throws IOException {
			String comment = consumeLine(false);
			properties.putComment(comment);
		}
		
		private String consumeLine(boolean multiline) throws IOException {
			while(!isEOF) {
				if(isLineBreak && multiline && isESC) {
					buf.append(parseLineBreak());
				} else {
					if(isLineBreak) {
						break;
					}
					consume();
				}
			}
			parseLineBreak();
			return flush();
		}


		private String parseLineBreak() throws IOException {
			if(tokenizer.ttype == '\r') {
				next();
				if(tokenizer.ttype == '\n') {
					next();
					return "\r\n";
				} else {
					return "\r";
				}
			} else if(tokenizer.ttype == '\n') {
				next();
				return "\n";
			}
			return "";
		}

		private String flush() {
			String value = buf.toString();
			buf.setLength(0);
			return value;
		}

		private void consume() throws IOException {
			if(tokenizer.ttype == StreamTokenizer.TT_WORD) {
				if(isESC) buf.append('\\'); //auto append
				buf.append(tokenizer.sval);
			} else if(tokenizer.ttype >= 0 && (!is('\\') || isESC))
				buf.append((char)tokenizer.ttype);
			next();
		}
		
		private void parseSeparator() throws IOException {
			switch(tokenizer.ttype) {
			case ' ':
				next();
				if(is(':') || is('=')) {
					next();
					if(is(' ')) next();
				}
				break;
			case ':':
				next();
				break;
			case '=':
				next();
				break;
			}
		}
	}
	
	private static final class Node {
		protected NodeType type;
		protected HashMap<String, Node> children = null;
		protected String value = null;
		
		protected Node(NodeType type) {
			this.type = type;
		}
		
		public String toString(String path) {
			switch (type) {
			case COMMENT:
				return value + LINE_SEPARATOR;
			case VALUE:
			case GROUP:
				if(path == null)
					path = "$ROOT_VALUE$";
				String key = path.replaceAll("([:= ])", "\\\\$1");
				String val = value == null ? "" : value.replaceAll("(\\r\\n|\\r|\\n)", "\\\\$1");
				return key + KEY_SEPARATOR + val + LINE_SEPARATOR;
				case EMPTY_LINE:
			default:
				return LINE_SEPARATOR;
			}
		}
		
		public void toString(String prefix, StringBuilder buf) {
			if(hasValue() || type == NodeType.EMPTY_LINE)
				buf.append(toString(prefix));
			if(hasChildren())
				children.forEach((key, node) -> node.toString(prefix == null ? key : prefix + PATH_SEPARATOR + key, buf));
		}
		
		protected boolean hasValue() {
			return value != null;
		}
		
		protected Node setValue(String value) {
			this.value = value;
			return this;
		}
		
		protected boolean hasChildren() {
			return children != null && !children.isEmpty();
		}
		
		protected Node getChild(String key) {
			return children == null ? null : children.get(key);
		}
		
		public Node createChild(String segment) {
			if(children == null)
				children = new LinkedHashMap<>();
			type = NodeType.GROUP;
			Node child = new Node(NodeType.VALUE);
			children.put(segment, child);
			return child;
		}

		public void forEach(PropertyTreeConsumer consumer) {
			if(!hasChildren())
				return;
			forEach(consumer, null);
		}
		
		private void forEach(PropertyTreeConsumer consumer, String prefix) {
			for(Entry<String, Node> set: children.entrySet()) {
				Node node = set.getValue();
				String key = set.getKey(); 
				String path = prefix == null ? set.getKey() : prefix + PATH_SEPARATOR + key;
				if(node.hasValue())
					consumer.accept(key, path, node.value);
				if(node.hasChildren())
					node.forEach(consumer, path);
			}
		}
	}
	
	private static class NodePair {
		public final String key;
		public final Node node;
		
		public NodePair(String key, Node node) {
			this.key = key;
			this.node = node;
		}
	}
	
	private static class NodeTree {
		private final Node root;
		
		public NodeTree(Node node) {
			this.root = node;
		}

		protected LinkedList<String> createPath(String path) {
			LinkedList<String> result = new LinkedList<>();
			int i1 = 0, i2 = 0;
			while(i2 != -1) {
				i2 = path.indexOf(PATH_SEPARATOR, i1);
				if(i2 == -1) {
					result.add(path.substring(i1, path.length()));
				} else {
					result.add(path.substring(i1, i2));
					i1 = i2 + 1;
				}
			}
			return result;
		}

		public Node getNode(String path) {
			Node node = root;
			for(String segment: createPath(path)) {
				node = node.getChild(segment);
				if(node == null)
					break;
			}
			return node;
		}
		
		public Node createNode(String path) {
			Node parent = root ,child = null;
			for(String segment: createPath(path)) {
				child = parent.getChild(segment);
				if(child == null) {
					child = parent.createChild(segment);
				}
				parent = child;
			}
			return parent;
		}
		
		public void putComment(String comment) {
		}

		public void putEmptyLine() {
		}
		
		public String toString() {
			StringBuilder buf = new StringBuilder();
			root.toString(null, buf);
			return buf.toString();
		}

		public Node remove(String path, boolean truncate) {
			Node node = root, child = null;
			LinkedList<NodePair> pairs = new LinkedList<>();
			
			//find node to remove
			for(String part: createPath(path)) {
				child = node.getChild(part);
				if(child == null)
					return null; //not present
				
				pairs.addFirst(new NodePair(part, node));
				node = child;
			}
			//delete node and all empty parents
			boolean first = true;
			for(NodePair pair: pairs) {
				if(truncate || !child.hasChildren()) {
					//remove
					pair.node.children.remove(pair.key);
					truncate = false;
				} else {
					if(first) {
						pair.node.value = null; //remove node value
					}
					break; //no more cleanup
				}
				first = false;
				child = pair.node;
			}
			
			return node; //deleted node
			
		}

	}
	
	private static class PreservingOrderTree extends NodeTree {
		
		private final ArrayList<NodePair> order = new ArrayList<>();

		public PreservingOrderTree(Node node) {
			super(node);
		}
		
		@Override
		public Node createNode(String path) {
			Node node = getNode(path);
			if(node != null) {
				if(!node.hasValue())
					order.add(new NodePair(path, node));
				return node;	
			}
			node = super.createNode(path);
			order.add(new NodePair(path, node));
			return node;
		}
		
		public void putComment(String comment) {
			order.add(new NodePair(null, new Node(NodeType.COMMENT).setValue(comment)));
		}

		public void putEmptyLine() {
			order.add(new NodePair(null, new Node(NodeType.EMPTY_LINE)));
		}
		
		@SuppressWarnings("unlikely-arg-type")
		public Node remove(final String path, boolean truncate) {
			if(truncate) {
				final String prefix = path + PATH_SEPARATOR;
				Object remover = new Object() {
					public boolean equals(Object other) {
						if(other == null || !(other instanceof NodePair))
							return false;
						NodePair pair = (NodePair) other;
						return pair.key != null && (path.equals(pair.key) || pair.key.startsWith(prefix));   
					};
				};
				
				while(order.remove(remover));
				
			} else {
				order.remove(new Object() {
					@Override
					public boolean equals(Object other) {
						if(other == null || !(other instanceof NodePair))
							return false;
						NodePair pair = (NodePair) other;
						return path.equals(pair.key);
					}
				});
			}
			return super.remove(path, truncate);
		}
		
		public String toString() {
			StringBuilder buf = new StringBuilder();
			for(NodePair entry: order)
				buf.append(entry.node.toString(entry.key));
			return buf.toString();
		}
		
	}
	
	// Start of AdvncedProperties implementation
	private final NodeTree tree;
	
	public AdvancedProperties() {
		this(false);
	}

	public AdvancedProperties(boolean preserveComments) {
		this(new Node(NodeType.GROUP), preserveComments);
	}
	
	private AdvancedProperties(Node root, boolean preserveComments) {
		tree = preserveComments ? new PreservingOrderTree(root) : new NodeTree(root);
	}
	
	// public API
	
	
	public void put(String path, Object value) {
		tree.createNode(path).setValue(String.valueOf(value));
	}
	
	public void putNonNull(String path, Object value) {
		if(value != null)
			put(path, value);
	}
	
	public void putMap(String prefix, Map<String, String> map) {
		if(map == null)
			return;
		
		if(prefix == null || prefix.isEmpty())
			prefix = "";
		else
			if(prefix.charAt(prefix.length() - 1) != '.')
				prefix += '.';

		for(Entry<String, String> entry: map.entrySet()) {
			putNonNull(prefix + entry.getKey(), entry.getValue());
		}
	}
	
	
	public void putComment(String comment) {
		String trimmed = comment.trim();
		if(trimmed.isEmpty()) {
			putEmptyLine();
			return;
		}
		char first = trimmed.charAt(0);
		if(first != '#' && first !='!')
			comment = "#" + comment;
		
		tree.putComment(comment);
	}
	
	public void putEmptyLine() {
		tree.putEmptyLine();
	}
	
	public String get(String path) {
		Node node = tree.getNode(path);
		return node == null ? null : node.value;
	}
	
	public String get(String... segments) {
		return get(createPath(segments));
	}
	
	public String getDefault(String path, String defaultValue) {
		String value = get(path);
		return value == null ? defaultValue : value;
	}
	
	public String getFirst(String... paths) {
		String value = null;
		for(String path: paths) {
			value = get(path);
			if(value != null) return value;
		}
		return null;
	}
	
	public AdvancedProperties getSubProperties(String path, boolean create) {
		Node subNode = create ? tree.createNode(path) : tree.getNode(path);
		return subNode == null ? null : new AdvancedProperties(subNode, false);
		
	}
	
	public void remove(String path, boolean removeGroup) {
		tree.remove(path, removeGroup);
	}
	
	public String toString() {
		return tree.toString();
	}
	
	public void load(Reader input) throws IOException {
		new Parser(input, this).parse();
	}
	
	// static API
	
	public static AdvancedProperties create() {
		return new AdvancedProperties();
	}
	
	public static AdvancedProperties create(Reader input) throws IOException {
		return new Parser(input, new AdvancedProperties()).parse();
	}
	
	public static AdvancedProperties create(String filename) throws IOException {
		return create(Files.newBufferedReader(Paths.get(filename)));
	}

	public Map<String, String> getAsStringMap(String path) {
		Node node = tree.getNode(path);
		if(node == null || !node.hasChildren())
			return null;
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		node.forEach((key, treepath, value) -> map.put(treepath, value));
		return map;
	}

	public static String createPath(String... segments) {
		return String.join("" + PATH_SEPARATOR, segments);
	}

	public Set<String> getKeys(String path) {
		Node node = tree.getNode(path);
		if(node == null || !node.hasChildren())
			return Collections.emptySet();
		return node.children.keySet();
	}

	public boolean getBoolean(String path, boolean defaultValue) {
		String value = get(path);
		if(value == null)
			return defaultValue;
		return value.equalsIgnoreCase("true");
	}

	public boolean containsValue(String path) {
		Node node = tree.getNode(path);
		return node != null && node.hasValue();
	}
	
	public boolean containsGroup(String path) {
		Node node = tree.getNode(path);
		return node != null && node.hasChildren();
	}
	
	public void forEach(PropertyTreeConsumer consumer) {
		tree.root.forEach(consumer);
	}

}
