# Peripheral Programming

## 1.0 Purpose

This document describes peripherals of the "POSIX and DOS Computers" mod.
It explains what a peripheral is, how the kernel discovers attached hardware,
how a peripheral becomes a device through a driver, and how to write
a peripheral block.

## 2.0 Overview

A peripheral is a block that provides bytes to a computer. The block
implements the `Peripheral` interface on its block entity. When the
block is placed adjacent to a computer, the computer's kernel may find
it during boot and bind it to a driver.

A peripheral does not know which operating system is running, nor does
it know whether a driver has been loaded for it. It exposes a byte
stream and a small control channel. Interpretation is the driver's
responsibility.

## 3.0 The Peripheral Interface

Interface name: `com.eliaslucky.mc_dos.api.hardware.Peripheral`

Methods:

- `deviceClass () returns String` - The hardware class. Lowercase,
  no spaces. Used by drivers to match hardware. Examples:
  `"mccmd"`, `"printer"`, `"plotter"`.

- `vendorId () returns String` - The vendor. By convention the mod ID
  of the mod that supplies the peripheral.

- `productId () returns String` - The product identifier. Distinguishes
  hardware revisions.

- `description () returns String` - Human-readable description shown
  in device listings.

- `write (byte[] data) returns void` - Push data to the device. This
  method must be non-blocking. A device that needs to process data on
  a later server tick should queue the data internally.

- `read (int maxBytes) returns byte[]` - Return up to `maxBytes` of
  pending output. Return an empty array if nothing is available.

- `ioctl (int cmd, byte[] arg) returns int` - Device-specific control
  call. Command codes are defined by the driver that owns the device.
  Return `-1` if the device does not support the requested command.

- `isReady () returns boolean` - Whether the peripheral can accept
  I/O. Block entities typically return `false` on the client side.

- `hasData () returns boolean` - Whether `read` would return
  non-empty data.

## 4.0 The Peripheral Bus

Interface name: `com.eliaslucky.mc_dos.api.hardware.PeripheralBus`

The bus enumerates peripherals reachable from a computer. The default
implementation, `AdjacentBlocksBus`, scans the six orthogonally
adjacent blocks.

Methods:

- `scan () returns List<PeripheralAddress>` - Enumerate peripherals.
  The returned list is stable within a single scan.

- `get (PeripheralAddress addr) returns Peripheral` - Resolve an
  address to the live peripheral. Returns `null` if the block has been
  removed.

### 4.1 PeripheralAddress

Record name: `com.eliaslucky.mc_dos.api.hardware.PeripheralAddress`

Components:

- `deviceClass (String)` - Peripheral class.
- `vendorId (String)` - Peripheral vendor.
- `productId (String)` - Peripheral product.
- `slot (int)` - Zero-based index within the device class.
- `worldPos (BlockPos)` - Block position.

The `slot` component is assigned per `deviceClass`. If two printers
are adjacent to a computer, they receive slots 0 and 1 in the printer
class. Two mccmd blocks receive slots 0 and 1 in the mccmd class,
independently of any printers.

## 5.0 Driver Loading

Peripherals become visible to applications only after a driver binds
to them. Drivers are loaded by the kernel at boot time, subject to
the loading model of the operating system.

### 5.1 MS-DOS

The DOS kernel reads `C:\CONFIG.SYS`. Each `DEVICE=` line names a
`.SYS` file and, optionally, load parameters. The file is a marker;
the driver's code is a Java class registered in `DriverRegistry`.

For a `DEVICE=` line to succeed, the following conditions must hold:

- The `.SYS` file must exist in the VFS at the specified path.
- `DriverRegistry` must contain a factory registered under the family
  `"dos"` and the basename of the file.
- The driver's `init` method must return `OK`.

### 5.2 UNIX v7

The UNIX v7 kernel scans the peripheral bus once at boot. For each
distinct `deviceClass` present, the kernel attempts to load a driver
from the `"unix"` family. The first matching peripheral is bound.

### 5.3 Linux

The Linux kernel scans the peripheral bus at boot. For each
peripheral, the kernel loads a driver from the `"linux"` family. A
driver may bind to every matching peripheral.

## 6.0 The DeviceHandler Adapter

Applications do not access a peripheral directly. They access a
`DeviceHandler` that the driver has registered. The default handler,
produced by `DeviceHandler.of(peripheral)`, forwards each method to
the peripheral without modification.

A driver that needs to transform data may implement its own
`DeviceHandler`. Common transformations include protocol framing,
data compression, or log capture.

## 7.0 Writing a Peripheral

The following steps describe the procedure for adding a peripheral.

Step 1. Create a block class that extends `Block` and implements
`EntityBlock`. Provide `newBlockEntity` and, if the peripheral
processes data on a later tick, `getTicker`.

Step 2. Create a block entity class that extends `BlockEntity` and
implements `Peripheral`.

Step 3. Implement the peripheral methods. Typical behavior:

- `deviceClass`, `vendorId`, `productId`, `description` return
  constant strings.
- `write` appends incoming bytes to an internal queue.
- `read` returns pending output.
- `ioctl` returns `-1` unless the device has commands of its own.
- `isReady` returns false on the client side.
- `hasData` reflects the state of the internal output queue.

Step 4. Register the block and block entity type in the standard
Minecraft registries.

Step 5. Write a driver for each operating system family in which the
peripheral should appear.

Step 6. Register the drivers in `ModDrivers` and call
`ModDrivers.register` from `FMLCommonSetupEvent`.

Example peripheral:

    public class PlotterBlockEntity extends BlockEntity
            implements Peripheral {

        private final Deque<String> plotQueue = new ArrayDeque<>();
        private final Deque<String> outputQueue = new ArrayDeque<>();

        public PlotterBlockEntity(BlockPos pos, BlockState state) {
            super(AllBlockEntities.PLOTTER.get(), pos, state);
        }

        @Override public String deviceClass() { return "plotter"; }
        @Override public String vendorId()    { return "myaddon"; }
        @Override public String productId()   { return "plotter_v1"; }
        @Override public String description() { return "Pen plotter"; }

        @Override
        public boolean isReady() {
            return level != null && !level.isClientSide();
        }

        @Override public boolean hasData() { return !outputQueue.isEmpty(); }

        @Override
        public synchronized void write(byte[] data) {
            String text = new String(data, StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                if (!line.isEmpty()) plotQueue.addLast(line);
            }
        }

        @Override
        public synchronized byte[] read(int maxBytes) {
            if (outputQueue.isEmpty()) return new byte[0];
            String line = outputQueue.pollFirst();
            return line.getBytes(StandardCharsets.UTF_8);
        }

        @Override public int ioctl(int cmd, byte[] arg) { return -1; }

        public static void tick(Level level, BlockPos pos, BlockState state,
                                PlotterBlockEntity be) {
            if (level.isClientSide()) return;
            be.processQueue();
        }

        private synchronized void processQueue() {
            while (!plotQueue.isEmpty()) {
                String line = plotQueue.pollFirst();
                outputQueue.addLast("PLOT: " + line);
            }
        }
    }

## 8.0 Reference

### 8.1 Interfaces

- `Peripheral` - Byte stream and control channel.
- `PeripheralBus` - Enumeration of attached peripherals.
- `PeripheralAddress` - Record identifying a peripheral.
- `DeviceHandler` - Application-facing interface.
- `Driver` - OS-side adapter for a peripheral.
- `DriverContext` - Boot-time environment for a driver.
- `DriverRegistry` - Global registry of driver factories.

### 8.2 Lifecycle Summary

| Event                    | Action by peripheral            |
|--------------------------|---------------------------------|
| Block placed             | Nothing. Block entity created.  |
| Computer boots           | Kernel may scan the bus.        |
| Driver binds             | Driver calls `registerDevice`.  |
| Application reads/writes | Peripheral receives I/O.        |
| Data queued              | Peripheral processes on tick.   |
| Block removed            | Peripheral is garbage collected.|

### 8.3 Conventions

- Device class names are lowercase and contain no spaces.
- Vendor identifiers match the mod ID.
- Product identifiers are lowercase and use underscores.
- The `write` method is non-blocking. Long-running work belongs in
  the ticker.
- The `read` method returns an empty array when no data is available.
  It never returns `null`.
