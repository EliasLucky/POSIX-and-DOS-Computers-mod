package com.eliaslucky.mc_dos.blocks.computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.api.shell.Pipeline;
import com.eliaslucky.mc_dos.api.shell.PipelineExecutor;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerType.DriveBaySpec;
import com.eliaslucky.mc_dos.blocks.computer.bus.AdjacentBlocksBus;
import com.eliaslucky.mc_dos.blocks.computer.drive.DriveBay;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.items.RemovableMediaItem;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity for a computer block.
 *
 * <p>This is the machine. It owns the virtual filesystem, the
 * environment variables, the kernel, the drive bays, the BIOS
 * configuration, and the boot state. It runs entirely on the server
 * side; the client is a terminal emulator that speaks to it through
 * packets.
 */
public class ComputerBlockEntity extends BlockEntity {
	private ComputerType computerType = ComputerType.IBM_PC_AT;
	private final VirtualFileSystem fileSystem = new VirtualFileSystem();
	private boolean initializedDefaults = false;

	private final Map<String, String> environment = new HashMap<>();
	private Kernel kernel;
	private final List<DriveBay> driveBays = new ArrayList<>();
	public List<DriveBay> driveBays() { return List.copyOf(driveBays); }
	private MachineConfig machineConfig;
	private BootState bootState = BootState.POST;
	
	public ComputerBlockEntity(BlockPos pos, BlockState state) {
		super(AllBlockEntities.COMPUTER_PROGRAMMABLE_BLOCK.get(), pos, state);
	}

	public VirtualFileSystem getFileSystem() {
		return fileSystem;
	}

	public ComputerType getComputerType() {
		return computerType;
	}
	/**
     * Bind this machine to a computer type. Called when the block is
     * placed and again on world load. Sets up files, environment, and
     * drive bays on first call; boots the BIOS on the server.
     *
     * @param type the machine type
     */
	public void setComputerType(ComputerType type) {
		this.computerType = type;
		fileSystem.setPolicy(type.commandProcessor.fileNamePolicy());
		if (!initializedDefaults) {
			setupDefaultFiles();
			setupEnvironment();
			initializedDefaults = true;	
		}

        if (machineConfig == null) {
            machineConfig = type.defaultConfig.get();
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
	        	ExecutableRegistry.Entry exe = ExecutableRegistry.get(proc.osFamily(), fileName);
	            if (exe != null) {
	                node.content = exe.templateContent();
	                node.executeBit = true;
	            } else {
	                // 2. Everything else is the OS's responsibility.
	                String content = proc.defaultFileContent(fileName);
	                node.content = content != null ? content : "";
	            }
	        }
	        dir.addChild(node);
	    }

	    vfs.setCurrentPath(computerType.defaultPath);
	}

	private void setupEnvironment() {
		environment.clear();
		environment.put("COMSPEC", "C:\\COMMAND.COM");
		environment.put("PATH", computerType.commandProcessor.defaultPath());
		environment.put("PROMPT", "$P$G");
}
    /**
     * @return the machine's BIOS configuration, or {@code null} if the
     *         machine has never been booted
     */
    public MachineConfig getMachineConfig() { return machineConfig; }

    public BootState getBootState() { return bootState; }

    public List<String> getPostLines() { return List.copyOf(postLines); }

	public DriveBay driveBay(int index) {
	    return (index >= 0 && index < driveBays.size()) ? driveBays.get(index) : null;
	}

	private void setupDriveBays() {
	    driveBays.clear();
	    for (int i = 0; i < computerType.driveBays.size(); i++) {
	        DriveBaySpec spec = computerType.driveBays.get(i);
	        driveBays.add(new DriveBay(i, spec.type(),
	                spec.dosLetter(), spec.posixDevice(), spec.posixMountPoint()));
	    }
	}
	/**
	 * Try to insert a removable media item into the first compatible,
	 * empty bay.
	 *
	 * @param stack  the stack being inserted (mutated: shrunk by 1)
	 * @param player the inserting player
	 * @return {@code true} if a bay accepted the media
	 */
	public boolean tryInsertMedia(ItemStack stack, Player player) {
	    if (!(stack.getItem() instanceof RemovableMediaItem rmi)) return false;

	    for (DriveBay bay : driveBays) {
	        if (bay.hasMedia()) continue;
	        if (!bay.type().canRead(rmi.media())) continue;

	        VirtualFileSystem.Node root = rmi.readRoot(stack);
	        if (!bay.insert(stack, root)) continue;

	        // Mount into the VFS under the bay's identifiers.
	        if (bay.dosLetter() != null) {
	            fileSystem.mount(bay.dosLetter() + ":", root,
	                    !rmi.writable(), "floppy bay " + bay.index());
	        }
	        if (bay.posixMountPoint() != null && bay.isPosixMounted()) {
	            fileSystem.mount(bay.posixMountPoint(), root,
	                    !rmi.writable(), "floppy bay " + bay.index());
	        }

	        stack.shrink(1);
	        setChanged();
	        return true;
	    }
	    return false;
	}

	/**
	 * Eject the media from the given bay, dropping it into the world or
	 * returning it to the player.
	 *
	 * @param bayIndex the bay to eject from
	 * @param player   the player performing the eject
	 * @return {@code true} if media was ejected
	 */
	public boolean tryEjectMedia(int bayIndex, Player player) {
	    DriveBay bay = driveBay(bayIndex);
	    if (bay == null || !bay.hasMedia()) return false;

	    // Save the current tree back into the item before ejecting.
	    if (bay.insertedStack().getItem() instanceof RemovableMediaItem rmi) {
	        rmi.writeRoot(bay.insertedStack(), bay.mountedRoot());
	    }

	    // Unmount.
	    if (bay.dosLetter() != null) {
	        fileSystem.unmount(bay.dosLetter() + ":");
	    }
	    if (bay.posixMountPoint() != null) {
	        fileSystem.unmount(bay.posixMountPoint());
	    }

	    ItemStack ejected = bay.eject();
	    if (!ejected.isEmpty()) {
	        if (!player.getInventory().add(ejected)) {
	            player.drop(ejected, false);
	        }
	    }
	    setChanged();
	    return true;
	}
	public boolean hasInsertedMedia() {
	    for (DriveBay bay : driveBays) if (bay.hasMedia()) return true;
	    return false;
	}
	
	public String executeLine(String rawLine) {
	    ICommandProcessor proc = computerType.commandProcessor;
	    ShellDialect dialect = proc.shellDialect(kernel);
	    if (dialect == null) {
	        return proc.process(this, rawLine);
	    }
	    Pipeline pipeline = dialect.parse(rawLine);
	    if (pipeline.isEmpty()) return "";

	    StreamResolver resolver = proc.createStreamResolver();
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
		ListTag baysTag = new ListTag();
	    for (DriveBay bay : driveBays) {
	        CompoundTag bayTag = new CompoundTag();
	        bay.save(bayTag);
	        baysTag.add(bayTag);
	    }
	    tag.put("DriveBays", baysTag);
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
		if (tag.contains("DriveBays")) {
	        ListTag baysTag = tag.getList("DriveBays", Tag.TAG_COMPOUND);
	        // Drive bays were created by setupDriveBays; overlay their saved state.
	        // If the count differs (computer type changed), rebuild.
	        if (baysTag.size() != driveBays.size()) {
	            setupDriveBays();
	        }
	        for (int i = 0; i < Math.min(baysTag.size(), driveBays.size()); i++) {
	            driveBays.get(i).load(baysTag.getCompound(i));
	        }
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
