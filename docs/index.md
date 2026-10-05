# POSIX and DOS Computers

## Developer Reference

### Publication Index

This index lists every document in the developer reference. Each
entry names the document, identifies its scope, and gives the
primary audience. All documents follow the same conventions:
numbered sections, formal method signatures, and a reference
appendix at the end.

---

## Documents

### 1. Block Initialization

**File:** `block_initialization.md`

Describes how computer blocks and block entities are registered,
bound to machine types, and initialized. Explains the boot state
machine, the default-file setup, the kernel handoff, and
persistence. Includes a sequence diagram of the initialization
path.

**Audience:** Addon developers adding new computer blocks,
peripheral blocks, or custom machine types.

### 2. Driver Programming

**File:** `driver.md`

Describes the driver model. Explains the `Driver` interface, the
`DriverContext` and `DriverInitContext` enum, the
`DriverRegistry`, and the load sequences for MS-DOS, UNIX v7, and
Linux. Includes a worked example of a DOS driver.

**Audience:** Addon developers making custom hardware available to
the simulated machines.

### 3. Machine Types

**File:** `machine_types.md` (UNFINISHED DOCUMENT)

Describes the `MachineType` interface, the built-in catalogue, and
the procedure for registering a custom machine. Covers the BIOS,
kernel, command processor, drive bay, and default file
configuration.

**Audience:** Addon developers shipping a new machine.

### 4. Command Processors

**File:** `shell.md`

Describes the `ICommandProcessor` interface, the shell dialect
abstraction, and the load sequence for a custom operating system.
Covers the prompt, the default file content method, and the stream
resolver.

**Audience:** Addon developers implementing a new shell.

### 5. Executable Registry

**File:** `executable_registry.md` (UNFINISHED DOCUMENT)

Describes the `ExecutableRegistry`, the `ExecutableFormat`
interface, and the procedure for making a file runnable from the
shell. Covers the `Runner` functional interface and the
`APP_LAUNCH` convention.

**Audience:** Addon developers shipping a new program.

### 6. Virtual File System

**File:** `virtual_file_system.md` (UNFINISHED DOCUMENT)

Describes the `VirtualFileSystem` class, the `Node` record, and
the mount system. Explains drive letters, POSIX mount points, and
the transient/persistent distinction.

**Audience:** Addon developers who need to read or write files
outside the standard shell commands.

### 7. BIOS and Setup Screens

**File:** `bios.md`

Describes the `Bios` interface, the `MachineConfig` record, and
the setup screen registry. Explains the POST sequence and the
procedure for registering a custom BIOS setup screen.

**Audience:** Addon developers adding custom firmware or a setup
screen.

### 8. Terminal and TUI Framework

**File:** `tui.md`

Describes the `TerminalApplication` base class, the `TuiScreen`
widget host, and the widget library (`TuiBox`, `TuiDialog`,
`TuiMenu`, `TuiList`, `TuiKeyValueTable`, and others). Explains
cell-based layout and the theme system.

**Audience:** Addon developers writing a new terminal application.

### 9. Peripherals

**File:** `peripheral.md`

Describes the `Peripheral`, `PeripheralBus`, `PeripheralAddress`
interfaces and the procedure for registering a custom peripheral
hardware block. Includes a worked example of peripheral.

**Audience:** Addon developers shipping a new peripheral.

### 9. Networking

**File:** `networking.md` (UNFINISHED WITHIN MOD'S CODEBASE)

Describes the mod's packet channel, the client-to-server and
server-to-client message set, and the conventions for adding a new
packet. Covers the boot action packet, the command packet, and the
file write packet.

**Audience:** Addon developers who need to coordinate client and
server state.

---

## Appendix A. Terminology

| Term              | Definition                                                   |
|-------------------|--------------------------------------------------------------|
| Machine type      | A descriptor for a specific computer model.                  |
| Kernel            | The lowest layer of an operating system.                     |
| Command processor | The shell and command language of an operating system.       |
| Peripheral        | A hardware block that exposes a byte stream.                 |
| Driver            | Code that adapts a peripheral to an operating system.        |
| Block entity      | The server-side state object attached to a block.            |
| Boot state        | One of OFF, POST, SETUP, RUNNING.                            |
| POST              | Power-on self test. The first phase of a boot.               |
| SETUP             | The BIOS configuration screen.                               |
| VFS               | Virtual file system.                                         |
| Mount             | A volume attached to the VFS at a known path or drive letter.|

## Appendix B. Document Conventions

- Method signatures are written as
  `methodName (Type) returns Type`.
- Interface and class names are given in full, including package
  when first introduced.
- Code examples are indented by four spaces.
- Reserved words and identifiers are written in monospace.
- Cross-references use the form "see section N.N".
- Figures are numbered sequentially and referenced by number.

## Appendix C. Publication History

| Revision | Date         | Notes                                    |
|----------|--------------|------------------------------------------|
| 1.0      | Initial      | First consolidated developer reference.  |
| 1.5      | ---          | Revised developer reference.             |
