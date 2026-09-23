package com.eliaslucky.mc_dos.blocks.computer;

import java.util.List;

import com.eliaslucky.mc_dos.blocks.computer.processors.Dos6CommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.LinuxCommandProcessor;

public enum ComputerType {
	IBM_PC_AT(
		"IBM Personal Computer AT (Model 5170)",
		"IBM Personal Computer DOS Version 3.30",
		0xFFFFFF/*0x00FF00*/,
		List.of(
				"COMMAND.COM",
				"AUTOEXEC.BAT",
				"CONFIG.SYS",
				"DOS/",
				"DOS/QBASIC.EXE"),
		List.of(
	            "IBM Personal Computer AT",
	            "IBM BIOS Version C1.00",
	            "Copyright IBM Corp. 1981, 1984, 1986",
	            "",
	            "0640 KB OK",
	            "",
	            "601-Diskette Error",
	            "(Run SETUP)",
	            "",
	            "Press <F1> to continue",
	            "",
	            "Starting MS-DOS...",
	            "",
	            "HIMEM is testing extended memory...done.",
	            "",
	            "IBM Personal Computer DOS",
	            "Version 3.30",
	            "(C)Copyright IBM Corp. 1981, 1988",
	            "(C)Copyright Microsoft Corp 1981, 1988",
	            ""
	        ),
		new Dos6CommandProcessor(),
		"C:\\"
	),

	PENTIUM_4_LINUX(
		"Pentium 4 ACPI BIOS Revision 1008",
		"Debian GNU/Linux 3.0 (woody)",
		0xFFFFFF,
		List.of("bin/", "etc/", "home/", "var/"),
		List.of(
	            "Phoenix - AwardBIOS v6.00PG",
	            "Copyright (C) 1984-2003, Phoenix Technologies, LTD",
	            "An Energy Star Ally",
	            "",
	            "Main Processor   : Intel(R) Pentium(R) 4 CPU 2.40GHz",
	            "Memory Test      : 524288K OK",
	            "",
	            "IDE Channel 0 Master : ST340014A   3.16",
	            "IDE Channel 0 Slave  : None",
	            "IDE Channel 1 Master : None",
	            "IDE Channel 1 Slave  : None",
	            "",
	            "Press DEL to enter SETUP, F12 for Boot Menu",
	            "",
	            "Booting from Hard Disk...",
	            "",
	            "LILO 22.2 boot:",
	            "Loading Linux 2.4.20-8 ............",
	            "Loading initrd ....................",
	            "",
	            "Debian GNU/Linux 3.0 (woody)",
	            "Kernel 2.4.20-8 on an i686",
	            "",
	            "p4-server login: root (automatic login)",
	            "",
	            "Last login: Mon Nov 18 09:14:22 from 10.0.0.1",
	            "",
	            "Welcome to Debian GNU/Linux 3.0",
	            ""
	        ),
		new LinuxCommandProcessor(),
		"/"
	);

	public final String modelName;
	public final String osVersion;
	public final int textColor;
	public final List<String> defaultFiles;
	public final List<String> bootSequence;
	public final ICommandProcessor commandProcessor;
	public final String defaultPath;

	ComputerType(String modelName, String osVersion, int textColor, List<String> defaultFiles, List<String> bootSequence, ICommandProcessor commandProcessor, String defaultPath) {
		this.modelName = modelName;
		this.osVersion = osVersion;
		this.textColor = textColor;
		this.defaultFiles = defaultFiles;
		this.bootSequence = bootSequence;
		this.commandProcessor = commandProcessor;
		this.defaultPath = defaultPath;
	}
}
