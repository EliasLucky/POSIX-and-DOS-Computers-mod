package com.eliaslucky.mc_dos.blocks.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.fs.PosixFileNamePolicy;

public class VirtualFileSystem {
	private FileNamePolicy policy;
	private final Node root;
	private Node currentDir;
	private String currentPath = "/";
	public record MountPoint(String id, Node rootNode, boolean removable) {}

    public void mount(String id, Node rootNode, boolean removable) { ... }
    public void unmount(String id) { ... }
    public boolean isMounted(String id) { ... }
    public MountPoint findMount(String id) { ... }
    
	/** Default to POSIX until an OS is bound. */
	public VirtualFileSystem() {
		this(PosixFileNamePolicy.INSTANCE);
	}
	
	public VirtualFileSystem(FileNamePolicy policy) {
		this.policy = policy;
		this.root = new Node("/", true);
		this.currentDir = root;
	}

	public FileNamePolicy getPolicy()		   { return policy; }
	public void setPolicy(FileNamePolicy p)    { this.policy = p; }
	public String canonicalize(String rawName) { return policy.canonicalize(rawName); }
	public Node newFile(String rawName) {
		return new Node(policy.canonicalize(rawName), false);
	}
	public Node newDirectory(String rawName) {
		return new Node(policy.canonicalize(rawName), true);
	}
	public Node getRoot() { return root; }
	public Node getCurrentDir() { return currentDir; }
	public String getCurrentPath() { return currentPath; }
	public void setCurrentPath(String path) {
		Node resolved = resolvePath(path);
		if (resolved == null || !resolved.isDirectory) {
	        // Invalid path — silently keep the current directory.
	        // DOS would print "Invalid directory" here, but that's the
	        // caller's job (doCd). The VFS itself refuses to move.
	        return;
	    }
	    this.currentDir  = resolved;
	    this.currentPath = getAbsolutePath(resolved);
		//this.currentPath = path;
		//Node resolved = resolvePath(path);
		//if (resolved != null && resolved.isDirectory) {
		//	this.currentDir = resolved;
		//}
	}

	public Node resolvePath(String path) {
		if (path == null || path.trim().isEmpty()) return currentDir;

		String cleanPath = path.trim().replace('\\', '/');
		Node startNode = currentDir;

		// DOS drive letter or standard / root
		if (cleanPath.matches("(?i)^[A-Z]:.*")) {
			startNode = root;
			int colonIndex = cleanPath.indexOf(':');
			cleanPath = cleanPath.substring(colonIndex + 1);
		} else if (cleanPath.startsWith("/")) {
			startNode = root;
		}

		if (cleanPath.startsWith("/")) {
			cleanPath = cleanPath.substring(1);
		}

		if (cleanPath.isEmpty()) return root;

		for (String segment : cleanPath.split("/+")) {
			if (segment.isEmpty() || segment.equals(".")) continue;
			if (segment.equals("..")) {
				if (startNode.parent != null) startNode = startNode.parent;
				continue;
			}
			String key = policy.lookupKey(segment);
			Node child = startNode.children.get(key);
			if (child == null) return null;
			startNode = child;
		}
		return startNode;
	}

	public String getAbsolutePath(Node node) {
	    String sep    = policy.pathSeparator();
	    String prefix = policy.rootPrefix();

	    if (node == null || node == root) {
	        // Root: DOS "C:\", POSIX "/"
	        return prefix.isEmpty() ? sep : prefix + sep;
	    }

	    StringBuilder sb = new StringBuilder();
	    Node curr = node;
	    while (curr != null && curr != root) {
	        sb.insert(0, sep + curr.name);
	        curr = curr.parent;
	    }

	    return prefix.isEmpty() ? sb.toString() : prefix + sb.toString();
	}

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		tag.put("Root", root.save());
		tag.putString("CurrentPath", currentPath);
		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		if (tag.contains("Root")) {
			Node loadedRoot = Node.load(tag.getCompound("Root"),null);
			this.root.children.clear();
			this.root.children.putAll(loadedRoot.children);
			reparentChildren(this.root);
		}
		this.currentDir = root;
	    this.currentPath = "/";
		//if (tag.contains("CurrentPath")) {
		//	this.currentPath = tag.getString("CurrentPath");
		//	Node found = resolvePath(this.currentPath);
		//	this.currentDir = (found != null && found.isDirectory) ? found : root;
		//}
	}
	
	private static void reparentChildren(Node parent) {
	    for (Node child : parent.children.values()) {
	        child.parent = parent;
	        reparentChildren(child);
	    }
	}
	
	/**
	 * Normalize a user-supplied filename into MS-DOS 8.3 short form.
	 * Uppercases, strips disallowed characters, truncates the base to 8
	 * and the extension to 3 at the last dot.
	 */
	public static String toShortName(String raw) {
		if (raw == null || raw.isEmpty()) return "";

		// Take only the last path component.
		String s = raw.replace('\\', '/');
		int slash = s.lastIndexOf('/');
		if (slash >= 0) s = s.substring(slash + 1);

		s = s.toUpperCase(Locale.ROOT);

		String base, ext = "";
		int dot = s.lastIndexOf('.');
		if (dot >= 0) {
			base = s.substring(0, dot);
			ext  = s.substring(dot + 1);
		} else {
			base = s;
		}

		// Strip characters MS-DOS doesn't allow in filenames.
		// Allowed: A-Z 0-9 ! # $ % & ' ( ) - @ ^ _ ` { } ~
		base = base.replaceAll("[^A-Z0-9!#$%&'()\\-@^_`{}~]", "");
		ext  = ext.replaceAll("[^A-Z0-9!#$%&'()\\-@^_`{}~]", "");

		if (base.length() > 8) base = base.substring(0, 8);
		if (ext.length()  > 3) ext	= ext.substring(0, 3);

		if (base.isEmpty() && ext.isEmpty()) return "";
		return ext.isEmpty() ? base : base + "." + ext;
	}

	public static class Node {
		public String name;
		public boolean isDirectory;
		public String content;
		public Node parent;
		public Map<String, Node> children = new HashMap<>();

		public long createdTime  = System.currentTimeMillis();
		public long modifiedTime = System.currentTimeMillis();
		
	    /** POSIX execute bit. Ignored under DOS (a .EXE runs because of its extension). */
	    public boolean executeBit   = false;

		public Node(String name, boolean isDirectory) {
			this.name = (name == null) ? "" : name;
			this.isDirectory = isDirectory;
			this.content = "";
		}

		public void addChild(Node child) {
			child.parent = this;
			children.put(child.name, child);
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Name", name);
			tag.putBoolean("IsDir", isDirectory);
			tag.putString("Content", content);
			tag.putLong("Created",	createdTime);
			tag.putLong("Modified", modifiedTime);
			tag.putBoolean("Exec",  executeBit);

			ListTag childrenList = new ListTag();
			for (Node child : children.values()) {
				childrenList.add(child.save());
			}
			tag.put("Children", childrenList);
			return tag;
		}

		public static Node load(CompoundTag tag, Node parentNode) {
			Node node = new Node(tag.getString("Name"), tag.getBoolean("IsDir"));
			node.content = tag.getString("Content");
			node.createdTime  = tag.contains("Created")  ? tag.getLong("Created")  : System.currentTimeMillis();
			node.modifiedTime = tag.contains("Modified") ? tag.getLong("Modified") : node.createdTime;
			node.executeBit   = tag.contains("Exec")     && tag.getBoolean("Exec");
			node.parent = parentNode;

			ListTag childrenList = tag.getList("Children", Tag.TAG_COMPOUND);
			for (int i = 0; i < childrenList.size(); i++) {
				Node child = Node.load(childrenList.getCompound(i),node);
				node.children.put(child.name, child);
			}
			return node;
		}
	}
}
