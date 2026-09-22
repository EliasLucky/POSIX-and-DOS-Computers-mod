package com.eliaslucky.mc_dos.blocks.computer;

import java.util.List;

import com.eliaslucky.mc_dos.blocks.computer.processors.Dos6CommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.LinuxCommandProcessor;

public enum ComputerType {
	IBM_PC_AT(
		"IBM Personal Computer AT (Model 5170)",
		"IBM Personal Computer Basic C1.10",
		"IBM Personal Computer DOS Version 3.30",
		"640 KB System RAM / 60288 KB Free",
		0xFFFFFF/*0x00FF00*/,
		List.of("COMMAND.COM", "AUTOEXEC.BAT", "CONFIG.SYS"),
		"IBM PC/AT POST Memory Test: 640K OK",
		new Dos6CommandProcessor(),
		"C:\\"
	),

	PENTIUM_4_LINUX(
		"Pentium 4 ACPI BIOS Revision 1008",
		"Intel(R) Pentium(R) 4 CPU 2.40GHz",
		"Debian GNU/Linux 3.0 (woody)",
		"512 MB System RAM",
		0xFFFFFF,
		List.of("bin/", "etc/", "home/", "var/"),
		"Welcome to GNU/Linux Debian 3.0",
		new LinuxCommandProcessor(),
		"/"
	);

	public final String modelName;
	public final String biosString;
	public final String osVersion;
	public final String memoryString;
	public final int textColor;
	public final List<String> defaultFiles;
	public final String bootMessage;
	public final ICommandProcessor commandProcessor;
	public final String defaultPath;

	ComputerType(String modelName, String biosString, String osVersion, String memoryString, int textColor, List<String> defaultFiles, String bootMessage, ICommandProcessor commandProcessor, String defaultPath) {
		this.modelName = modelName;
		this.biosString = biosString;
		this.osVersion = osVersion;
		this.memoryString = memoryString;
		this.textColor = textColor;
		this.defaultFiles = defaultFiles;
		this.bootMessage = bootMessage;
		this.commandProcessor = commandProcessor;
		this.defaultPath = defaultPath;
	}
}
