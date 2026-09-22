package com.eliaslucky.furniture.blocks.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class VirtualFileSystem {
	private final Node root;
	private Node currentDir;
	private String currentPath = "/";

	public VirtualFileSystem() {
		root = new Node("/", true);
		currentDir = root;
	}

	public Node getRoot() { return root; }
	public Node getCurrentDir() { return currentDir; }
	public String getCurrentPath() { return currentPath; }
	public void setCurrentPath(String path) {
		this.currentPath = path;
		Node resolved = resolvePath(path);
		if (resolved != null && resolved.isDirectory) {
			this.currentDir = resolved;
		}
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

		String[] segments = cleanPath.split("/+");
		Node current = startNode;

		for (String segment : segments) {
			if (segment.isEmpty() || segment.equals(".")) {
				continue;
			}
			if (segment.equals("..")) {
				if (current.parent != null) {
					current = current.parent;
				}
				continue;
			}

			// Case-insensitive lookup for DOS compatibility
			Node child = null;
			for (Map.Entry<String, Node> entry : current.children.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(segment)) {
					child = entry.getValue();
					break;
				}
			}

			if (child == null) {
				return null; // Segment not found
			}
			current = child;
		}

		return current;
	}

	public String getAbsolutePath(Node node) {
		return getAbsolutePath(node, currentPath.startsWith("C:") || currentPath.contains("\\"));
	}

	public String getAbsolutePath(Node node, boolean isDos) {
		if (node == root || node == null) return isDos ? "C:\\" : "/";

		StringBuilder sb = new StringBuilder();
		Node curr = node;
		while (curr != null && curr != root) {
			String separator = isDos ? "\\" : "/";
			sb.insert(0, separator + curr.name);
			curr = curr.parent;
		}

		if (isDos) {
			return "C:" + sb.toString();
		}
		return sb.length() == 0 ? "/" : sb.toString();
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
		}
		if (tag.contains("CurrentPath")) {
			this.currentPath = tag.getString("CurrentPath");
			Node found = resolvePath(this.currentPath);
			this.currentDir = (found != null && found.isDirectory) ? found : root;
		}
	}

	public static class Node {
		public String name;
		public boolean isDirectory;
		public String content;
		public Node parent;
		public Map<String, Node> children = new HashMap<>();

		public long createdTime  = System.currentTimeMillis();
		public long modifiedTime = System.currentTimeMillis();

		public Node(String name, boolean isDirectory) {
			this.name = name;
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
