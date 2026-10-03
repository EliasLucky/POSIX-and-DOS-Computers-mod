# Driver Programming

## 1.0 Purpose

This document page describes how drivers work in the "POSIX and DOS Computers".
It explains what a driver is, how drivers are registered,
how the operating system loads them at boot time, and how to write a
driver for a peripheral device. The intended audience is addon
developers who wish to make their hardware available to the machines
the mod simulates (NOT emulates).

## 2.0 Overview

A driver is code that connects hardware to an operating system. The
hardware side is represented by the `Peripheral` interface, which
exposes a byte stream. The software side is a named device that the
operating system and its applications can open by name.

A driver does not know anything about Minecraft. It operates on the
`PeripheralBus` interface, which enumerates attached hardware, and on
`DriverContext`, which reports the loading environment and provides
methods for registering a device.

Drivers are stored in `DriverRegistry`, a global registry keyed by
operating-system family and driver name. The kernel of each OS family
looks up drivers in this registry at boot time.

## 3.0 The Driver Interface

Interface name: `com.eliaslucky.mc_dos.api.hardware.Driver`

Methods:

- `init (DriverContext) returns DriverInitResult` - Called once at
  boot. The driver should scan the bus for matching hardware, bind to
  one or more `Peripheral` instances, and register each as a device.

- `shutdown () returns void` - Called on shutdown or when the machine
  is powered off. The driver should release peripheral references.

- `name () returns String` - A human-readable name. By convention, the
  name includes the file extension for the target platform:
    - For MS-DOS, a `.SYS` extension, for example `MCCMD.SYS`.
    - For UNIX, a `.c` extension, for example `mccmd.c`.
    - For Linux, a `.ko` extension, for example `mccmd.ko`.

## 4.0 DriverInitResult

Enum name: `com.eliaslucky.mc_dos.api.hardware.DriverInitResult`

Values:

- `OK` - The driver bound to hardware and registered its devices. The
  kernel keeps the driver loaded.

- `FAILED` - The driver could not find matching hardware, or could not
  register a device name. The kernel discards the driver.

- `DEFERRED` - Reserved for future use. Currently treated as `FAILED`.

## 5.0 The DriverContext Interface

Interface name: `com.eliaslucky.mc_dos.api.hardware.DriverContext`

The context is provided by the kernel for the duration of a single
`init` call. Drivers should not retain it.

Methods:

- `bus () returns PeripheralBus` - The peripheral bus of the machine
  being booted.

- `loadParams () returns Map<String, String>` - Parameters supplied at
  load time. The meaning of a parameter is defined by the driver.

- `registerDevice (String name, DeviceHandler handler) returns String`
  - Registers a device with the kernel's device namespace. Returns the
  name under which the device was registered, or `null` on failure.

- `log (String message) returns void` - Writes a boot-time message.

Parameter semantics by OS family:

- MS-DOS: parameters are parsed from the `DEVICE=` line in
  `CONFIG.SYS`. For example:
  `DEVICE=C:\DRIVERS\MCCMD.SYS /SLOT=0`
  produces `{"SLOT": "0"}`.

- UNIX v7: parameters are always empty. Drivers are compiled into the
  kernel in the original operating system.

- Linux: parameters are supplied by the module loader. Currently
  unused.

## 6.0 The DriverRegistry

Class name: `com.eliaslucky.mc_dos.api.hardware.DriverRegistry`

The registry stores driver factories. A factory is called once per
boot to produce a fresh driver instance.

Methods:

- `register (String osFamily, String driverName, DriverFactory factory)
  returns void` - Adds a factory to the registry.

- `load (String osFamily, String driverName) returns Driver` -
  Instantiates a driver. Returns `null` if no factory is registered.

- `exists (String osFamily, String driverName) returns boolean` -
  Checks for a factory without instantiating it.

Parameter conventions:

- `osFamily` - One of `"dos"`, `"unix"`, or `"linux"`. Matched
  case-insensitively.

- `driverName` - Uppercased before use as a key. For DOS, this is the
  `.SYS` basename. For UNIX, the `.c` basename. For Linux, the `.ko`
  basename.

Driver factories are registered during common mod setup, in
`FMLCommonSetupEvent`:

    event.enqueueWork(() -> {
        DriverRegistry.register("dos", "MCCMD", DosMccmdDriver::new);
        DriverRegistry.register("unix", "MCCMD", UnixMccmdDriver::new);
        DriverRegistry.register("linux", "MCCMD", LinuxMccmdDriver::new);
    });

## 7.0 Load Sequence by Operating System

### 7.1 MS-DOS

The DOS kernel reads `C:\CONFIG.SYS` at boot. Each `DEVICE=` line is
processed in order:

1. The path is stripped to its basename. For example,
   `C:\DRIVERS\MCCMD.SYS` yields `MCCMD`.
2. The suffix `.SYS` is removed. The resulting name is used as the
   `driverName` argument to `DriverRegistry.load`.
3. The remaining command-line tokens are parsed as load parameters. A
   token beginning with `/` is a parameter. A parameter of the form
   `/KEY=VALUE` produces a key-value pair. A parameter of the form
   `/KEY` produces the pair `{KEY: "1"}`.
4. A fresh `DosDriverContext` is constructed with the bus, the parsed
   parameters, and the DOS device table.
5. The driver's `init` method is called. On success, the driver is
   stored in the kernel's list of loaded drivers.

If the driver returns `FAILED`, the kernel logs a message and continues
to the next `DEVICE=` line.

### 7.2 UNIX v7

The UNIX v7 kernel scans the peripheral bus once at boot. For each
distinct `deviceClass` present on the bus, the kernel consults
`DriverRegistry.load("unix", deviceClass)`.

If the class has no registered driver, the scan continues. If a driver
is present, the kernel calls its `init` method with a fresh
`UnixDriverContext`.

The UNIX v7 driver model does not support multiple devices of the same
class under a single driver instance. The first matching peripheral is
used.

### 7.3 Linux

The Linux kernel scans the peripheral bus at boot. For each peripheral
of a given class, the kernel calls `DriverRegistry.load("linux",
deviceClass)`. If a driver is found, its `init` method is called with a
fresh `LinuxDriverContext`. The driver may bind to all matching
peripherals, up to a limit imposed by the driver itself.

The kernel registers each peripheral under an indexed device name. The
first device of class `plotter` becomes `/dev/plotter0`, the second
becomes `/dev/plotter1`, and so on.

## 8.0 The DeviceHandler Adapter

Interface name: `com.eliaslucky.mc_dos.api.hardware.DeviceHandler`

Applications read and write to a device through a `DeviceHandler`, not
through the `Peripheral` directly. The `Peripheral` interface has four
I/O methods; the `DeviceHandler` interface also has four I/O methods.
For most drivers the two map one to one.

The interface provides a static factory method for this case:

    static DeviceHandler of (Peripheral peripheral)

The returned handler forwards `onWrite`, `onRead`, and `onIoctl` to the
peripheral, and delegates `hasData` and `description` to it. It is safe
to call when the peripheral is `null`; I/O becomes a no-op.

Drivers that need to transform data on the way through should
implement a custom `DeviceHandler` rather than using the factory.

## 9.0 Writing a Driver

The following steps describe the procedure for adding a new driver.

Step 1. Create a class that implements `Driver`.

Step 2. Implement `name`. Return the platform-specific filename. For
MS-DOS, include the `.SYS` extension.

Step 3. Implement `init`. The method should:

- Read any required parameters from `DriverContext.loadParams`.
- Call `DriverContext.bus().scan()` and filter the results by
  `deviceClass` and, if applicable, by `slot`.
- Retrieve the matching `Peripheral` with `DriverContext.bus().get`.
- Wrap the peripheral with `DeviceHandler.of` and call
  `DriverContext.registerDevice` with a device name.
- Return `OK` on success, `FAILED` otherwise.

Step 4. Implement `shutdown`. Release references to any peripherals.
Do not attempt to unregister devices; the kernel clears its device
table on its own.

Step 5. Register the driver in `ModDrivers` and call `ModDrivers.register`
from `FMLCommonSetupEvent`.

Example DOS driver:

    public class DosPlotterDriver implements Driver {

        private Peripheral peripheral;

        @Override public String name() { return "PLOTTER.SYS"; }

        @Override
        public DriverInitResult init(DriverContext ctx) {
            int slot = Integer.parseInt(
                    ctx.loadParams().getOrDefault("SLOT", "0"));

            PeripheralAddress addr = ctx.bus().scan().stream()
                    .filter(a -> a.deviceClass().equals("plotter"))
                    .filter(a -> a.slot() == slot)
                    .findFirst().orElse(null);

            if (addr == null) return DriverInitResult.FAILED;

            peripheral = ctx.bus().get(addr);
            String name = ctx.registerDevice("PLOT",
                    DeviceHandler.of(peripheral));
            if (name == null) return DriverInitResult.FAILED;

            ctx.log("PLOTTER.SYS installed at slot " + slot);
            return DriverInitResult.OK;
        }

        @Override public void shutdown() { peripheral = null; }
    }

## 10.0 Reference

### 10.1 Classes and Interfaces

- `Driver` - Common driver contract.
- `DriverContext` - Boot-time environment for a single driver.
- `DriverInitResult` - Outcome of `Driver.init`.
- `DriverRegistry` - Global registry of driver factories.
- `DeviceHandler` - Application-facing device interface.
- `Peripheral` - Hardware-facing byte stream interface.
- `PeripheralBus` - Enumeration of attached hardware.

### 10.2 Reserved Device Names (MS-DOS)

The following names cannot be registered by a driver. They are
reserved by the DOS kernel:

- `CON`, `PRN`, `AUX`, `NUL`, `CLOCK$`
- `LPT1`, `LPT2`, `LPT3`
- `COM1`, `COM2`, `COM3`, `COM4`

Device names on MS-DOS are between 1 and 8 characters. No extension
is permitted.

### 10.3 Device Path Conventions by OS

| OS      | Name form          | Example                    |
|---------|--------------------|----------------------------|
| MS-DOS  | Bare name, 1-8 char| `MCCMD`                    |
| UNIX v7 | /dev path          | `/dev/mccmd`               |
| Linux   | /dev path, indexed | `/dev/mccmd0`, `/dev/mccmd1` |
