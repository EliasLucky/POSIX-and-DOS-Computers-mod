package com.eliaslucky.mc_dos.blocks.computer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.blocks.computer.processors.AbstractDosCommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.exec.ExecutableRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ComputerBlockEntity extends BlockEntity {
	private ComputerType computerType = ComputerType.IBM_PC_AT;
	private final VirtualFileSystem fileSystem = new VirtualFileSystem();
	private boolean initializedDefaults = false;

	private final Map<String, String> environment = new HashMap<>();
	private Kernel kernel;

	public ComputerBlockEntity(BlockPos pos, BlockState state) {
		super(AllBlockEntities.COMPUTER_PROGRAMMABLE_BLOCK.get(), pos, state);
	}

	public VirtualFileSystem getFileSystem() {
		return fileSystem;
	}

	public ComputerType getComputerType() {
		return computerType;
	}

	public void setComputerType(ComputerType type) {
		this.computerType = type;
		fileSystem.setPolicy(type.commandProcessor.fileNamePolicy());
		if (!initializedDefaults) {
			setupDefaultFiles();
			setupEnvironment();
			initializedDefaults = true;	
		}
		if (!level.isClientSide()) {
		    bootKernel();
		}
		setChanged();
	}
	
	private void bootKernel() {
	    if (kernel != null) {
	        kernel.shutdown();
	        kernel = null;
	    }
	    Kernel newKernel = computerType.commandProcessor.createKernel();
	    if (newKernel == null) return;

	    PeripheralBus bus = new AdjacentBlocksBus(level, worldPosition);
	    newKernel.boot(bus, fileSystem);
	    this.kernel = newKernel;

	    for (String line : newKernel.getBootLog()) {
	        // Route to terminal output if you want them visible
	        // (the terminal screen reads from getBootLog on open)
	    }
	}
	
	public Kernel getKernel() { return kernel; }

	public Map<String, String> getEnvironment() { return environment; }
	
	private void setupDefaultFiles() {
		VirtualFileSystem vfs = fileSystem;
	    vfs.getRoot().children.clear();

	    ICommandProcessor proc = computerType.commandProcessor;

	    for (String filePath : computerType.defaultFiles) {
	        String normalized = filePath.replace('\\', '/');
	        String[] segments = normalized.split("/");
	        boolean isDir = filePath.endsWith("/") || filePath.endsWith("\\");

	        VirtualFileSystem.Node dir = vfs.getRoot();
	        for (int i = 0; i < segments.length - 1; i++) {
	            String segName = vfs.canonicalize(segments[i]);
	            VirtualFileSystem.Node existing = dir.children.get(segName);
	            if (existing == null) {
	                existing = new VirtualFileSystem.Node(segName, true);
	                dir.addChild(existing);
	            }
	            dir = existing;
	        }

	        String fileName = vfs.canonicalize(segments[segments.length - 1]);
	        if (dir.children.containsKey(fileName)) continue;

	        VirtualFileSystem.Node node = new VirtualFileSystem.Node(fileName, isDir);

	        if (!isDir) {
	            // 1. Executables come from the registry, with the MZ header.
	            ExecutableRegistry.Entry exe = ExecutableRegistry.get(fileName);
	            if (exe != null) {
	                node.content = exe.templateContent();
	            } else {
	                // 2. Everything else is the OS's responsibility.
	                String content = proc.defaultFileContent(fileName);
	                node.content = content != null ? content : "";
	            }
	        }
	        dir.addChild(node);
	    }

	    vfs.setCurrentPath(computerType.defaultPath);
		/*fileSystem.setCurrentPath(computerType.defaultPath);
		for (String filePath : computerType.defaultFiles) {
			boolean isDir = filePath.endsWith("/") || filePath.endsWith("\\");
			String cleanName = filePath.replaceAll("[/\\\\]", "");
			VirtualFileSystem.Node child = new VirtualFileSystem.Node(cleanName, isDir);
			fileSystem.getCurrentDir().addChild(child);
		}*/
	}

	private void setupEnvironment() {
		environment.clear();
		environment.put("COMSPEC", "C:\\COMMAND.COM");
		environment.put("PATH", computerType.commandProcessor.defaultPath());
		environment.put("PROMPT", "$P$G");
}
	
	public String executeLine(String rawLine) {
	    ShellDialect dialect = computerType.commandProcessor.shellDialect(kernel);
	    if (dialect == null) {
	        // No dialect — fall back to direct processing, no pipes.
	        return computerType.commandProcessor.process(this, rawLine);
	    }
	    Pipeline pipeline = dialect.parse(rawLine);
	    if (pipeline.isEmpty()) return "";

	    StreamResolver resolver = new DosStreamResolver();   // override per OS later
	    return new PipelineExecutor(resolver).execute(pipeline, this);
	}

	/** for internal use and the immediate pane. */
	public String processCommand(String rawInput) {
		return computerType.commandProcessor.process(this, rawInput);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ComputerBlockEntity entity) {
		if (!level.isClientSide() && entity.activeUser != null) {
			Player player = level.getPlayerByUUID(entity.activeUser);
			if (player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
				entity.activeUser = null;
				entity.setChanged();
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putString("ComputerType", computerType.name());
		tag.putBoolean("InitializedDefaults", initializedDefaults);
		tag.put("FileSystem", fileSystem.serializeNBT());
		
		CompoundTag envTag = new CompoundTag();
		environment.forEach(envTag::putString);
		tag.put("Environment", envTag);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("ComputerType")) {
			try {
				this.computerType = ComputerType.valueOf(tag.getString("ComputerType"));
			}
			catch (IllegalArgumentException e) {
				this.computerType = ComputerType.IBM_PC_AT;
			}
		}
		this.initializedDefaults = tag.getBoolean("InitializedDefaults");
		if (tag.contains("FileSystem")) {
			fileSystem.deserializeNBT(tag.getCompound("FileSystem"));
		}
		if (tag.contains("Environment")) {
			environment.clear();
			CompoundTag envTag = tag.getCompound("Environment");
			for (String k : envTag.getAllKeys()) environment.put(k, envTag.getString(k));
		}
	}

	private UUID activeUser = null;

	public boolean isUsed() {
	    return activeUser != null;
	}

	public UUID getActiveUser() {
	    return activeUser;
	}

	public boolean tryOccupy(Player player) {
	    if (activeUser == null || activeUser.equals(player.getUUID())) {
	        activeUser = player.getUUID();
	        setChanged();
	        return true;
	    }
	    return false;
	}

	public void releaseUser(Player player) {
	    if (activeUser != null && activeUser.equals(player.getUUID())) {
	        activeUser = null;
	        setChanged();
	    }
	}
}
