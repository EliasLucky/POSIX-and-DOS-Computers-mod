package com.eliaslucky.mc_dos.blocks.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.eliaslucky.mc_dos.blocks.computer.fs.FileError;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileOpResult;
import com.eliaslucky.mc_dos.blocks.computer.fs.Mount;
import com.eliaslucky.mc_dos.blocks.computer.fs.PosixFileNamePolicy;

public class VirtualFileSystem {
	private FileNamePolicy policy;
	private final Node root;
	private Node currentDir;
	private String currentPath = "/";

	private final Map<String, Mount> mounts = new LinkedHashMap<>();
	
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

	public Node getRoot() { return root; }
	public Node getCurrentDir() { return currentDir; }
	public String getCurrentPath() { return currentPath; }
	/**
	 * Change the current directory.
	 *
	 * @param path the target path
	 * @return {@code true} if the path resolved to a directory and the
	 *		   working directory was updated; {@code false} otherwise
	 */
	public boolean setCurrentPath(String path) {
		Node resolved = resolvePath(path);
		if (resolved == null || !resolved.isDirectory) {
			return false;
		}
		this.currentDir  = resolved;
		this.currentPath = getAbsolutePath(resolved);
		return true;
	}

	public Node resolvePath(String path) {
		if (path == null || path.trim().isEmpty()) return currentDir;

		String clean = path.trim().replace('\\', '/');

		// DOS drive letter: "A:\FOO"
		if (clean.matches("(?i)^[A-Z]:.*")) {
			String drive = clean.substring(0, 2).toUpperCase(Locale.ROOT);
			Mount m = mounts.get(drive);
			if (m == null) return null;
			return resolveFrom(m.rootNode(), clean.substring(2), policy);
		}

		// POSIX absolute path: check mounts first
		if (clean.startsWith("/")) {
			String best = null;
			for (String id : mounts.keySet()) {
				if (!id.startsWith("/")) continue;
				if (clean.equals(id) || clean.startsWith(id + "/")) {
					if (best == null || id.length() > best.length()) best = id;
				}
			}
			if (best != null) {
				Mount m = mounts.get(best);
				String rest = clean.substring(best.length());
				return resolveFrom(m.rootNode(), rest, policy);
			}
			return null;
		}

		// Relative path from currentDir
		return resolveFrom(currentDir, clean, policy);
	}

	/** Walk a path string from a starting node. */
	private static Node resolveFrom(Node start, String path, FileNamePolicy policy) {
		if (path.isEmpty() || path.equals("/")) return start;
		String p = path.startsWith("/") ? path.substring(1) : path;

		Node current = start;
		for (String segment : p.split("/+")) {
			if (segment.isEmpty() || segment.equals(".")) continue;
			if (segment.equals("..")) {
				if (current.parent != null) current = current.parent;
				continue;
			}
			Node child = current.children.get(policy.lookupKey(segment));
			if (child == null) return null;
			current = child;
		}
		return current;
	}

	public String getAbsolutePath(Node node) {
		if (node == null) return policy.pathSeparator();

		// Find the topmost ancestor.
		Node top = node;
		while (top.parent != null) top = top.parent;

		// Mounted tree: find which mount owns it.
		for (Mount m : mounts.values()) {
			if (m.rootNode() == top) {
				String sub = buildPathFrom(node, top, policy.pathSeparator(), "");
				return m.id() + sub;
			}
		}
		return "?"; // orphaned node
	}

	private static String buildPathFrom(Node node, Node stopAt, String sep, String prefix) {
		if (node == stopAt) return prefix.isEmpty() ? sep : prefix + sep;
		StringBuilder sb = new StringBuilder();
		Node curr = node;
		while (curr != null && curr != stopAt) {
			sb.insert(0, sep + curr.name);
			curr = curr.parent;
		}
		return prefix.isEmpty() ? sb.toString() : prefix + sb.toString();
	}
	/**
	 * Write content to a file, creating it if necessary.
	 * Updates mount usage accounting.
	 *
	 * @param path	  path to the file
	 * @param content the new content
	 * @return an ok result, or a failure describing why
	 */
	public FileOpResult writeFile(String path, String content) {
		Node node = resolvePath(path);
		boolean create = (node == null);

		Node parent;
		String name;
		if (create) {
			int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
			if (slash < 0) { parent = currentDir; name = path; }
			else {
				String parentPath = path.substring(0, slash);
				name = path.substring(slash + 1);
				parent = parentPath.isEmpty() ? root : resolvePath(parentPath);
			}
			if (parent == null || !parent.isDirectory)
				return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, path);
			name = canonicalize(name);
			if (name.isEmpty())
				return FileOpResult.fail(FileError.INVALID_NAME, path);
		} else {
			if (node.isDirectory)
				return FileOpResult.fail(FileError.ACCESS_DENIED, path);
			parent = node.parent;
			name = node.name;
		}

		Mount mount = findMountFor(parent);
		if (mount != null) {
			FileOpResult check = mount.checkWrite(name, content.length());
			if (!check.success()) return check;
		}

		if (create) {
			Node fresh = new Node(name, false);
			fresh.content = content;
			parent.addChild(fresh);
			if (mount != null) mount.usage().addEntry(content.length());
		} else {
			long delta = (long) content.length() - node.content.length();
			node.content = content;
			node.modifiedTime = System.currentTimeMillis();
			if (mount != null)
				mount.usage().changeSize(delta);
		}
		return FileOpResult.ok();
	}

	public FileOpResult createDirectory(String path) {
		Node existing = resolvePath(path);
		if (existing != null)
			return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, path);

		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		Node parent;
		String name;
		if (slash < 0) { parent = currentDir; name = path; }
		else {
			String parentPath = path.substring(0, slash);
			name = path.substring(slash + 1);
			parent = parentPath.isEmpty() ? root : resolvePath(parentPath);
		}
		if (parent == null || !parent.isDirectory)
			return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, path);
		name = canonicalize(name);
		if (name.isEmpty())
			return FileOpResult.fail(FileError.INVALID_NAME, path);

		Mount mount = findMountFor(parent);
		if (mount != null) {
			FileOpResult check = mount.checkWrite(name, 0);
			if (!check.success()) return check;
		}

		parent.addChild(new Node(name, true));
		if (mount != null) mount.usage().addEntry(0);
		return FileOpResult.ok();
	}

	public FileOpResult deleteFile(String path) {
		Node node = resolvePath(path);
		if (node == null)
			return FileOpResult.fail(FileError.FILE_NOT_FOUND, path);
		if (node.isDirectory)
			return FileOpResult.fail(FileError.ACCESS_DENIED, path);
		if (node.parent == null)
			return FileOpResult.fail(FileError.ACCESS_DENIED, path);

		Mount mount = findMountFor(node.parent);
		if (mount != null && mount.readOnly())
			return FileOpResult.fail(FileError.WRITE_PROTECTED, path);

		int bytes = node.content.length();
		node.parent.children.remove(node.name);
		if (mount != null) mount.usage().removeEntry(bytes);
		return FileOpResult.ok();
	}

	public FileOpResult removeDirectory(String path) {
		Node node = resolvePath(path);
		if (node == null || !node.isDirectory)
			return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, path);
		if (node == root) return FileOpResult.fail(FileError.ACCESS_DENIED, path);
		if (!node.children.isEmpty())
			return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, "not empty: " + path);

		Mount mount = findMountFor(node.parent);
		if (mount != null && mount.readOnly())
			return FileOpResult.fail(FileError.WRITE_PROTECTED, path);

		node.parent.children.remove(node.name);
		if (mount != null) mount.usage().removeEntry(0);
		return FileOpResult.ok();
	}

	public FileOpResult renameFile(String from, String to) {
		Node src = resolvePath(from);
		if (src == null) return FileOpResult.fail(FileError.FILE_NOT_FOUND, from);
		if (src.parent == null) return FileOpResult.fail(FileError.ACCESS_DENIED, from);

		String newName = canonicalize(to);
		if (newName.isEmpty()) return FileOpResult.fail(FileError.INVALID_NAME, to);
		if (src.parent.children.containsKey(newName))
			return FileOpResult.fail(FileError.DIRECTORY_PROBLEM, to);

		Mount mount = findMountFor(src.parent);
		if (mount != null && mount.readOnly())
			return FileOpResult.fail(FileError.WRITE_PROTECTED, from);

		src.parent.children.remove(src.name);
		src.name = newName;
		src.parent.children.put(newName, src);
		return FileOpResult.ok();
	}

	public FileOpResult copyFile(String from, String to) {
		Node src = resolvePath(from);
		if (src == null || src.isDirectory)
			return FileOpResult.fail(FileError.FILE_NOT_FOUND, from);

		return writeFile(to, src.content);
	}
	/**
	 * Attach a volume to the file system. This is the full-featured
	 * variant; callers that want to make the persistent/transient
	 * distinction explicit should use {@link #mountPersistent} or
	 * {@link #mountTransient} instead.
	 *
	 * @param id			mount identifier
	 * @param rootNode		root of the mounted tree
	 * @param readOnly		whether the volume rejects writes
	 * @param source		human description
	 * @param capacityBytes byte limit, or 0 for unlimited
	 * @param maxEntries	entry limit, or 0 for unlimited
	 * @param persistent	whether the block entity owns and saves this mount
	 * @return the new mount
	 */
	public Mount mount(String id, Node rootNode, boolean readOnly, String source,
					   long capacityBytes, int maxEntries, boolean persistent) {
		Mount m = new Mount(id, rootNode, readOnly, source,
				capacityBytes, maxEntries, persistent);
		mounts.put(id, m);
		return m;
	}

	/**
	 * Attach a persistent volume. Persistent volumes are saved into the
	 * block entity's NBT and restored on load.
	 *
	 * <p>Called by the block entity during setup for the primary
	 * hard disk. An addon that ships a machine with two internal disks
	 * calls this twice with different IDs.
	 *
	 * @param id	   mount identifier, e.g. {@code "C:"} or {@code "/"}
	 * @param rootNode root of the volume
	 * @return the new mount
	 */
	public Mount mountPersistent(String id, Node rootNode) {
		return mount(id, rootNode, false, "persistent volume", 0, 0, true);
	}

	/**
	 * Attach a transient volume. Transient volumes are <em>not</em>
	 * saved with the block entity — the object that owns them is
	 * responsible for persisting their content.
	 *
	 * <p>Floppies, CDs, and channel-attached disks are all transient.
	 * The item stack in the drive bay saves the floppy's tree; the
	 * addon's own block entity saves the channel disk's tree.
	 *
	 * @param id			mount identifier
	 * @param rootNode		root of the volume
	 * @param readOnly		whether the volume rejects writes
	 * @param source		human description
	 * @param capacityBytes byte limit, or 0 for unlimited
	 * @param maxEntries	entry limit, or 0 for unlimited
	 * @return the new mount
	 */
	public Mount mountTransient(String id, Node rootNode, boolean readOnly, String source,
								long capacityBytes, int maxEntries) {
		return mount(id, rootNode, readOnly, source,
				capacityBytes, maxEntries, false);
	}

	/** Remove a mount. The mounted tree is untouched; the caller owns it. */
	public void unmount(String id) { mounts.remove(id); }

	/** @return the mount with this ID, or {@code null}. */
	public Mount findMount(String id) { return mounts.get(id); }

	/**
	 * @param node a node in the VFS
	 * @return the mount whose tree contains {@code node}, or {@code null}
	 *		   if the node is not on any mounted volume
	 */
	public Mount findMountFor(VirtualFileSystem.Node node) {
		if (node == null) return null;
		VirtualFileSystem.Node top = node;
		while (top != null && top.parent != null) top = top.parent;
		for (Mount m : mounts.values()) {
			if (m.rootNode() == top) return m;
		}
		return null;
	}

	/** @return all current mounts, immutable view. */
	public Map<String, Mount> mounts() { return Map.copyOf(mounts); }

	/**
	 * Serialize the persistent volumes and the current working directory.
	 *
	 * <p>Only mounts with {@code persistent == true} are written. A
	 * floppy's tree lives on the item stack that holds it; the block
	 * entity must not duplicate that data here, or the two copies
	 * would drift after ejection.
	 *
	 * @return the NBT representation
	 */
	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		ListTag mountsTag = new ListTag();
		for (Mount m : mounts.values()) {
			if (!m.persistent()) continue;

			CompoundTag one = new CompoundTag();
			one.putString("Id", m.id());
			one.putBoolean("ReadOnly", m.readOnly());
			one.putString("Source", m.source());
			one.putLong("Capacity", m.capacityBytes());
			one.putInt("MaxEntries", m.maxEntries());
			one.put("Tree", m.rootNode().save());
			mountsTag.add(one);
		}
		tag.put("PersistentMounts", mountsTag);
		tag.putString("CurrentPath", currentPath);
		return tag;
	}

	/**
	 * Restore persistent volumes from NBT.
	 *
	 * <p>Transient mounts are not restored here the block entity
	 * re-attaches them as it re-loads its drive bays (or the addon
	 * re-attaches its own volumes). The VFS ends up in a state where
	 * only the persistent volumes are known, and the caller layers
	 * the rest on top.
	 *
	 * @param tag the saved state
	 */
	public void deserializeNBT(CompoundTag tag) {
		mounts.clear();

		ListTag mountsTag = tag.getList("PersistentMounts", Tag.TAG_COMPOUND);
		for (int i = 0; i < mountsTag.size(); i++) {
			CompoundTag one = mountsTag.getCompound(i);

			Node tree = Node.load(one.getCompound("Tree"), null);
			reparentChildren(tree);

			mount(
					one.getString("Id"),
					tree,
					one.getBoolean("ReadOnly"),
					one.getString("Source"),
					one.getLong("Capacity"),
					one.getInt("MaxEntries"),
					true);
		}

		// Restore the working directory. If it no longer resolves —
		// because the volume layout changed, or the mount is missing —
		// fall back to any available mount root, then to the phantom
		// VFS root as a last resort.
		String saved = tag.getString("CurrentPath");
		if (saved != null && setCurrentPath(saved)) return;

		for (Mount m : mounts.values()) {
			currentDir = m.rootNode();
			currentPath = m.id();
			return;
		}
		currentDir = root;
		currentPath = "/";
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
		public boolean executeBit	= false;

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
			tag.putBoolean("Exec",	executeBit);

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
			node.executeBit   = tag.contains("Exec")	 && tag.getBoolean("Exec");
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
