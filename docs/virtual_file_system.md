# Virtual File System

## 1.0 Purpose

This document describes the `VirtualFileSystem` class, the `Node`
tree, the mount system, and the naming rules that govern path
resolution. It explains how volumes are attached, how transient and
persistent data are distinguished, and how the file system is
serialized.

The intended audience is addon developers who need to read or write
files outside the standard shell commands.

## 2.0 Overview

Every machine owns one `VirtualFileSystem` instance. The file
system is a tree of `Node` objects. Each node is either a directory
or a file. Directories hold child nodes in a map keyed by the
canonical form of the child's name. Files hold a string of content.

Volumes are attached to the tree by *mounts*. A mount associates a
root node with an identifier, which is either a DOS drive letter
(`"C:"`), a POSIX mount point (`"/mnt/floppy"`), or an opaque name.
Path resolution walks the tree from a starting node, but a mount
redirects the walk to a different tree when the identifier matches.

## 3.0 The Node Record

Class name: `com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem.Node`

Fields:

- `name` — The canonical name of this node.
- `isDirectory` — Whether the node is a directory.
- `content` — File content. Empty for directories.
- `parent` — The parent node, or `null` for a root.
- `children` — Map of child nodes, keyed by name.
- `createdTime` — Creation timestamp in milliseconds.
- `modifiedTime` — Last modification timestamp.
- `executeBit` — POSIX execute bit. Ignored under DOS.

Methods:

- `addChild (Node) returns void` — Attach a child. Sets the
  child's parent pointer.
- `save () returns CompoundTag` — Serialize this node and all
  descendants.
- `load (CompoundTag, Node) returns Node` — Deserialize a node
  from NBT.

## 4.0 The VirtualFileSystem Class

Class name: `com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem`

### 4.1 Construction

The no-argument constructor uses the POSIX file name policy. The
one-argument constructor accepts a `FileNamePolicy`. In practice
the policy is set later by the machine type's command processor,
so the constructor's choice is a placeholder.

### 4.2 Path Resolution

Method: `resolvePath (String) returns Node`

The method accepts three forms of path:

1. **DOS drive letter.** `A:\FOO\BAR.TXT`. The drive letter and
   colon are used as a mount ID; the rest is walked from the
   mount's root node.

2. **POSIX absolute.** `/mnt/floppy/FILE.TXT`. The longest mount
   prefix is identified; the remainder is walked from the mount's
   root.

3. **Relative.** `FILE.TXT` or `..\OTHER.TXT`. The walk starts
   from the current directory.

The walk normalizes `..` and `.` segments but does not permit
walking above the root of a mount.

### 4.3 Directory Operations

- `setCurrentPath (String) returns boolean` — Change the working
  directory. Returns `false` if the path does not resolve to a
  directory.

- `createDirectory (String) returns FileOpResult` — Create a
  directory. Fails if the parent does not exist.

- `removeDirectory (String) returns FileOpResult` — Remove an
  empty directory.

### 4.4 File Operations

- `writeFile (String, String) returns FileOpResult` — Write a
  file, creating it if necessary.

- `deleteFile (String) returns FileOpResult` — Delete a file.

- `renameFile (String, String) returns FileOpResult` — Rename a
  file within its parent.

- `copyFile (String, String) returns FileOpResult` — Copy a file.

Every operation returns a `FileOpResult`, which carries either
success or a `FileError` value with an associated path.

## 5.0 Mounts

Class name: `com.eliaslucky.mc_dos.blocks.computer.fs.Mount`

A mount associates a root node with a mount ID. Fields:

- `id` — The mount identifier. A DOS drive letter, a POSIX path,
  or an opaque string.
- `rootNode` — The root of the mounted tree.
- `readOnly` — Whether the mount rejects writes.
- `source` — A human description, shown by diagnostic commands.
- `capacityBytes` — Byte limit, or 0 for unlimited.
- `maxEntries` — Entry limit, or 0 for unlimited.
- `persistent` — Whether the block entity saves this mount.

### 5.1 Mounting Conventions

`mountPersistent (String, Node) returns Mount` — Attach a
persistent volume. The volume is saved into the block entity's
NBT and restored on world load. The primary hard disk uses this
form.

`mountTransient (String, Node, boolean, String, long, int)
returns Mount` — Attach a transient volume. The volume is not
saved with the block entity. Floppies, CDs, and channel-attached
disks are all transient; the item stack in the drive bay or the
addon's block entity is responsible for persisting the content.

### 5.2 The Distinction

The persistence distinction matters because a transient mount's
content is owned by something else. If a floppy's tree were saved
into the block entity as well as into the item stack, the two
copies would drift after ejection. The rule is: a block entity
saves only what it owns.

## 6.0 File Name Policies

Interface name: `com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy`

A policy supplies two methods:

- `canonicalize (String) returns String` — Convert a raw name to
  its canonical form. Returns the empty string if the name is
  invalid.

- `lookupKey (String) returns String` — Convert a canonical name
  to the key used in directory maps. Allows the map to be
  case-insensitive even when the display form is not.

Built-in policies:

| Policy                 | OS family | Behavior                                    |
|------------------------|-----------|---------------------------------------------|
| `PosixFileNamePolicy`  | unix, linux | Case-sensitive, no length limit.          |
| `DosFileNamePolicy`    | dos       | Case-insensitive, 8.3 short name format.    |

The DOS policy upper-cases names, strips disallowed characters,
and truncates the base to eight characters and the extension to
three.

## 7.0 Serialization

### 7.1 Persistent Volumes

`serializeNBT () returns CompoundTag` walks every persistent
mount and writes:

- The mount ID.
- The read-only flag.
- The source description.
- The capacity and entry limits.
- The full node tree, serialized recursively.

Transient mounts are not written. The current working directory is
written separately.

### 7.2 Load

`deserializeNBT (CompoundTag) returns void` clears the mount map,
restores every persistent mount, and then resolves the saved
working directory. If the working directory no longer exists — for
example, because a mount was removed — the loader falls back to
the first available mount's root, then to the phantom VFS root.

## 8.0 Working With the File System

### 8.1 Reading a File

    Node node = computer.getFileSystem().resolvePath("C:\\CONFIG.SYS");
    if (node != null && !node.isDirectory) {
        String content = node.content;
    }

### 8.2 Writing a File

    FileOpResult result = computer.getFileSystem()
            .writeFile("C:\\AUTOEXEC.BAT", content);
    if (result.success()) computer.setChanged();

Always call `setChanged` after a successful write. The block entity
does not save itself automatically.

### 8.3 Creating a Directory

    computer.getFileSystem().createDirectory("C:\\DRIVERS");

Directory creation fails if the parent does not exist. Create
parents in order.

### 8.4 Mounting a Volume

    VirtualFileSystem.Node root = new VirtualFileSystem.Node("/", true);
    // ... populate root ...
    computer.getFileSystem().mountTransient(
            "D:", root, false, "second hard disk", 0, 0);

The ID must not collide with an existing mount. Use
`findMount(id)` first to check.

## 9.0 Extending the File System

To add a new file name policy:

1. Implement `FileNamePolicy`.
2. Implement `canonicalize` and `lookupKey`.
3. Set the policy on the VFS via `setPolicy`.

To add a new volume type:

1. Construct a `Node` tree for the volume's content.
2. Register a mount with a unique ID.
3. Choose `mountPersistent` or `mountTransient` based on who owns
   the content.
4. On removal, call `unmount` before discarding the tree.

## 10.0 Reference

### 10.1 Classes and Interfaces

- `VirtualFileSystem` — The file system itself.
- `VirtualFileSystem.Node` — A file or directory.
- `Mount` — A volume attached to the tree.
- `FileNamePolicy` — Naming rules.
- `PosixFileNamePolicy` — POSIX naming.
- `FileOpResult` — Outcome of a file operation.
- `FileError` — Enumeration of failure modes.

### 10.2 File Errors

| Error               | Meaning                                          |
|---------------------|--------------------------------------------------|
| `FILE_NOT_FOUND`    | The path did not resolve to any node.            |
| `DIRECTORY_PROBLEM` | A parent is missing, or a directory is not empty.|
| `ACCESS_DENIED`     | The operation is not permitted on this node.     |
| `WRITE_PROTECTED`   | The mount is read-only.                          |
| `INVALID_NAME`      | The name is empty after canonicalization.        |

### 10.3 Common Errors

| Symptom                            | Likely cause                                      |
|------------------------------------|---------------------------------------------------|
| File written but not visible       | `setChanged` not called; block entity not saved.  |
| Mounted volume disappears on load  | Volume mounted as transient instead of persistent.|
| Case-sensitive name failure on DOS | A custom policy not installed on the VFS.         |
| Drive letter does not resolve      | Mount ID mismatch; check capitalization.          |
