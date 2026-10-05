# Machine Types

## 1.0 Purpose

This document describes the `MachineType` interface, the built-in
catalogue of machines, and the procedure for registering a custom
machine. It explains how a machine type supplies the BIOS, the
kernel, the command processor, the drive bays, and the default
files.

The intended audience is addon developers shipping a new machine.

## 2.0 Overview

A machine type is a descriptor for one model of computer. It
answers the question "what kind of machine is this?" and supplies
every OS-specific component the machine needs to boot. Two machines
of the same model share a machine type; two machines of different
models have different types.

Machine types are stored in `MachineTypeRegistry`, keyed by a
string ID. The ID is what gets persisted to NBT; changing it after
release breaks existing saves.

## 3.0 The MachineType Interface

Interface name: `com.eliaslucky.mc_dos.blocks.computer.MachineType`

Methods, with their purposes:

- `id () returns String` — Unique identifier, e.g.
  `"mc_dos:ibm_pc_at"`. Persisted to NBT.

- `modelName () returns String` — Model name shown in the terminal
  title bar.

- `cpuName () returns String` — CPU description, e.g.
  `"Intel 80286 @ 8 MHz"`.

- `osVersion () returns String` — OS version string returned by
  `VER`.

- `busType () returns String` — Bus description, e.g. `"ISA"` or
  `"PCI / AGP"`.

- `textColor () returns int` — Terminal foreground color.

- `osBootLines () returns List<String>` — Lines printed after POST
  completes and before the shell prompt appears.

- `defaultFiles () returns List<String>` — Files seeded into the
  VFS at first boot.

- `commandProcessor () returns ICommandProcessor` — Factory for
  the shell and kernel.

- `defaultPath () returns String` — Initial working directory.

- `bios () returns Bios` — Firmware layer. May be `null` for a
  machine with no BIOS (unusual; most machines have one).

- `driveBays () returns List<DriveBaySpec>` — Physical drive bays
  fitted to this machine.

- `defaultConfig () returns Supplier<MachineConfig>` — Factory for
  the machine's factory-default BIOS configuration.

## 4.0 The Built-in Catalogue

The mod ships with the following machine types:

| ID                     | Model                          |
|------------------------|--------------------------------|
| `mc_dos:ibm_pc_at`     | IBM Personal Computer AT       |

The enum `ComputerType` implements `MachineType` and holds the
built-in definitions. The enum's values are registered
automatically at class load.

## 5.0 The MachineTypeRegistry

Class name: `com.eliaslucky.mc_dos.blocks.computer.MachineTypeRegistry`

Methods:

- `register (MachineType) returns void` — Add a machine type.
  Called by addons during `FMLCommonSetupEvent`.

- `get (String) returns MachineType` — Look up a machine type by
  ID. Returns `null` if not found.

- `exists (String) returns boolean` — Check for a type without
  retrieving it.

- `all () returns Map<String, MachineType>` — Immutable view of
  every registered type.

The registry is seeded from `ComputerType.values()` at class load.
Addon types are registered after the mod's own types.

## 6.0 Drive Bay Specifications

Record name: `com.eliaslucky.mc_dos.blocks.computer.MachineType.DriveBaySpec`

A drive bay is described by four fields:

- `type` — The `DriveType` enumeration value: floppy, CD, DVD, and
  so on.

- `dosLetter` — The DOS drive letter assigned to this bay, or
  `null` if the bay is not mapped to a letter.

- `posixDevice` — The POSIX device path, e.g. `"/dev/fd0"`, or
  `null`.

- `posixMountPoint` — The POSIX default mount point, e.g.
  `"/mnt/floppy"`, or `null`.

The machine's `driveBays()` method returns a list of
`DriveBaySpec`. At first boot, each spec becomes a `DriveBay`
instance on the block entity.

## 7.0 The BIOS and Machine Configuration

The BIOS is supplied by the `bios()` method. It handles power-on
self test and the SETUP screen. The BIOS reads and writes a
`MachineConfig` record, which carries:

- System time.
- Floppy drive types.
- Hard disk types.
- Base memory in kilobytes.
- Extended memory in kilobytes.
- Math coprocessor presence.
- Primary display type.

The machine type's `defaultConfig()` supplies the factory-default
configuration. The BIOS may modify the configuration at runtime;
the modified version is persisted to NBT.

## 8.0 Writing a New Machine Type

### 8.1 Step by Step

1. Create a class that implements `MachineType`.
2. Implement every method. Most return short strings; the
   interesting ones are `commandProcessor`, `bios`, `defaultFiles`,
   and `driveBays`.
3. Write a command processor if the machine uses a new OS. If it
   uses an existing OS, reuse the command processor from the
   corresponding built-in type.
4. Write a BIOS if the machine has custom firmware. Otherwise
   reuse one of the built-in BIOSes.
5. Register the type during `FMLCommonSetupEvent`.
6. Add a machine block that passes the type to its constructor. See
   `block_initialization.md` for details.

### 8.2 A Worked Example

The following class defines a minimal machine:

    public final class MyMachine implements MachineType {

        public static final MyMachine INSTANCE = new MyMachine();

        private MyMachine() {}

        @Override public String id()          { return "myaddon:my_machine"; }
        @Override public String modelName()   { return "My Machine 1"; }
        @Override public String cpuName()     { return "Custom CPU @ 4 MHz"; }
        @Override public String osVersion()   { return "MyOS 1.0"; }
        @Override public String busType()     { return "Custom Bus"; }
        @Override public int    textColor()   { return 0x00FF00; }
        @Override public String defaultPath() { return "C:\\"; }

        @Override public List<String> osBootLines() {
            return List.of("MyOS 1.0 starting...", "");
        }

        @Override public List<String> defaultFiles() {
            return List.of("COMMAND.COM", "AUTOEXEC.BAT");
        }

        @Override public ICommandProcessor commandProcessor() {
            return new MyCommandProcessor();
        }

        @Override public Bios bios() { return new MyBios(); }

        @Override public List<DriveBaySpec> driveBays() {
            return List.of(new DriveBaySpec(DriveType.FDD_1_44M,
                                            "A", "/dev/fd0", "/mnt/floppy"));
        }

        @Override public Supplier<MachineConfig> defaultConfig() {
            return () -> MachineConfig.ibmAt(System.currentTimeMillis());
        }
    }

Register it in mod setup:

    event.enqueueWork(() -> MachineTypeRegistry.register(MyMachine.INSTANCE));

## 9.0 Persistence

The machine type ID is written to the block entity's NBT under the
key `MachineType`. On load, the block entity resolves the ID
through `MachineTypeRegistry.get`. If the ID is unknown, the load
falls back to a default type and the machine loses its identity.

Because the ID is the persistence key, treat it as immutable. If
you must rename a machine type, register a redirect from the old
ID to the new one during mod setup so old saves continue to load.

## 10.0 Reference

### 10.1 Classes and Interfaces

- `MachineType` — Descriptor for one computer model.
- `ComputerType` — Enumeration of built-in types.
- `MachineTypeRegistry` — Global registry.
- `MachineConfig` — BIOS-level configuration.
- `DriveBaySpec` — Description of one drive bay.
- `DriveType` — Enumeration of drive types.
- `ICommandProcessor` — Shell and command language.
- `Bios` — Firmware layer.

### 10.2 Field Reference

| Field            | Type                | Purpose                         |
|------------------|---------------------|---------------------------------|
| `id`             | `String`            | Persistence key. Immutable.     |
| `modelName`      | `String`            | Shown in window title.          |
| `cpuName`        | `String`            | Shown by BIOS POST.             |
| `osVersion`      | `String`            | Returned by `VER`.              |
| `busType`        | `String`            | Shown in system information.    |
| `textColor`      | `int` (ARGB)        | Terminal default foreground.    |
| `osBootLines`    | `List<String>`      | Post-POST startup messages.     |
| `defaultFiles`   | `List<String>`      | Files installed at first boot.  |
| `defaultPath`    | `String`            | Initial working directory.      |

### 10.3 Common Errors

| Symptom                             | Likely cause                                    |
|-------------------------------------|-------------------------------------------------|
| Machine boots to a black screen     | `commandProcessor()` returned `null`.           |
| Prompt shows wrong path             | `defaultPath()` mismatched with a mounted volume.|
| Default files never appear          | `defaultFiles()` empty, or `initializedDefaults` guard already set. |
| Save fails to load after rename     | Machine type ID changed without a redirect.     |
