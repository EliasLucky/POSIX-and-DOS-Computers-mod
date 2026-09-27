package com.eliaslucky.mc_dos.blocks.computer;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;

import java.util.List;
import java.util.function.Supplier;

/**
 * A type of machine. The interface behind {@link ComputerType} and
 * any addon-provided machine descriptors.
 *
 * <p>Every field of {@code ComputerType} appears here as a method.
 * The enum implements this interface so its values can be stored in
 * {@link MachineTypeRegistry} alongside addon types.
 *
 * @since 1.5
 */
public interface MachineType {
    /** @return unique identifier, e.g. {@code "mc_dos:ibm_pc_at"} */
    String id();

    /** @return the model name shown in the terminal window title. */
    String modelName();

    /** @return the CPU description. */
    String cpuName();

    /** @return the OS version string, returned by {@code VER}. */
    String osVersion();

    /** @return the bus type, e.g. {@code "ISA"} or {@code "PCI / AGP"}. */
    String busType();

    /** @return the terminal foreground color. */
    int textColor();

    /** @return files to seed into the VFS at first boot. */
    List<String> defaultFiles();

    /** @return the shell and kernel factory for this machine. */
    ICommandProcessor commandProcessor();

    /** @return initial working directory. */
    String defaultPath();

    /** @return the BIOS firmware. */
    Bios bios();

    /** @return physical drive bays fitted to this machine. */
    List<ComputerType.DriveBaySpec> driveBays();

    /** @return factory for this machine's factory-default configuration. */
    Supplier<MachineConfig> defaultConfig();
}
