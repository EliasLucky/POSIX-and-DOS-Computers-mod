package com.eliaslucky.mc_dos.blocks.computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.api.shell.Pipeline;
import com.eliaslucky.mc_dos.api.shell.PipelineExecutor;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.MachineType.DriveBaySpec;
import com.eliaslucky.mc_dos.blocks.computer.bus.AdjacentBlocksBus;
import com.eliaslucky.mc_dos.blocks.computer.drive.DriveBay;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.items.RemovableMediaItem;
import com.eliaslucky.mc_dos.api.hardware.DriverRegistry;

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
	private MachineType machineType = ComputerType.IBM_PC_AT;
	private final VirtualFileSystem fileSystem = new VirtualFileSystem();
	private boolean initializedDefaults = false;

	private final Map<String, String> environment = new HashMap<>();
	private Kernel kernel;
	private final List<DriveBay> driveBays = new ArrayList<>();

	private MachineConfig machineConfig;
	private BootState bootState = BootState.POST;
	private List<String> postLines = new ArrayList<>();

	private UUID activeUser = null;

	public ComputerBlockEntity(BlockPos pos, BlockState state) {
		super(AllBlockEntities.COMPUTER_PROGRAMMABLE_BLOCK.get(), pos, state);
	}

	// Accessors
	public VirtualFileSystem getFileSystem() { return fileSystem; }
	public MachineType getMachineType()		 { return machineType; }
	public Kernel getKernel()				 { return kernel; }
	public Map<String, String> getEnvironment() { return environment; }
	public MachineConfig getMachineConfig()  { return machineConfig; }
	public BootState getBootState()			 { return bootState; }
	public List<String> getPostLines()		 { return List.copyOf(postLines); }
	public List<DriveBay> driveBays()		 { return List.copyOf(driveBays); }

	public DriveBay driveBay(int index) {
		return (index >= 0 && index < driveBays.size()) ? driveBays.get(index) : null;
	}

	// Machine type binding

	/**
	 * Bind this machine to a machine type. Called when the block is
	 * placed and again on world load. Sets up files, environment, and
	 * drive bays on first call.
	 *
	 * @param type the machine type
	 */
	public void setMachineType(MachineType type) {
		this.machineType = type;
		fileSystem.setPolicy(type.commandProcessor().fileNamePolicy());

		if (!initializedDefaults) {
			setupDefaultFiles();
			setupEnvironment();
			setupDriveBays();
			initializedDefaults = true;
		}

		if (machineConfig == null) {
			machineConfig = type.defaultConfig().get();
		}
		setChanged();
	}
	/**
	 * Power the machine on. Runs POST, boots the kernel, and moves the
	 * boot state to {@link BootState#POST}.
	 *
	 * Cold boot start
	 */
	public void powerOn() {
		if (machineType == null) return;
		if (machineConfig == null) {
			machineConfig = machineType.defaultConfig().get();
		}
		seedRegisteredDriverFiles();
		fileSystem.setCurrentPath(machineType.defaultPath());
		bootState = BootState.POST;
		bootFromBios();
	}
	/**
	 * Ensure every driver registered for the machine's OS family has its
	 * file rpesent in the VFS.
	 */
	private void seedRegisteredDriverFiles() {
		String family = machineType.commandProcessor().osFamily();
		for (DriverRegistry.Entry e : DriverRegistry.forFamily(family)) {
			if (!e.hasFile()) continue; // UNIX 7 got no file
			ensureVfsFile(e.installPath(), e.templateContent());
		}
	}
	private void ensureVfsFile(String path, String content) {
		if (path == null || path.isEmpty()) return;

		String normalized = path.replace('\\', '/');
		String[] segments = normalized.split("/",-1);

		VirtualFileSystem.Node dir;
		int startIdx;

		if (normalized.startsWith("/")) {
			// POSIX absolute: "/lib/modules/.../mccmd.ko"
			dir = fileSystem.resolvePath("/");
			startIdx = 1;
		}
		else if (segments.length > 0 && segments[0].endsWith(":")) {
			// DOS drive: "C:\DRIVERS\MCCMD.SYS" (mount btw)
			dir = fileSystem.resolvePath(segments[0] + "\\");
			startIdx = 1;
		}
		else {
			// relative. should NOT happen for driver install paths at ALL
			dir = fileSystem.getCurrentDir();
			startIdx = 0;
		}
		if (dir == null | !dir.isDirectory) return;

		for (int i = startIdx; i < segments.length-1; i++) {
			String raw = segments[i];
			if (raw.isEmpty()) continue;
			String seg = fileSystem.canonicalize(raw);
			if (seg.isEmpty()) continue;

			VirtualFileSystem.Node child = dir.children.get(seg);
			if (child == null) {
				child = new VirtualFileSystem.Node(seg,true);
				dir.addChild(child);
			}
			if (!child.isDirectory) return; // can't descend. give up quietly
			dir = child;
		}

		String rawName = segments[segments.length-1];
		String name = fileSystem.canonicalize(rawName);
		if (name.isEmpty()) return;
		if (dir.children.containsKey(name)) return;

		VirtualFileSystem.Node file = new VirtualFileSystem.Node(name,false);
		file.content = content == null ? "" : content;
		dir.addChild(file);
	}
	public void setBootState(BootState state) {
		this.bootState = state;
		setChanged();
	}

	public void setMachineConfig(MachineConfig config) {
		this.machineConfig = config;
		setChanged();
	}

	/**
	 * Run the BIOS POST and boot the OS kernel.
	 */
	private void bootFromBios() {
		if (kernel != null) {
			kernel.shutdown();
			kernel = null;
		}

		Bios bios = machineType.bios();
		if (bios != null) {
			PeripheralBus bus = new AdjacentBlocksBus(level, worldPosition);
			postLines = new ArrayList<>(bios.runPost(this, bus, machineConfig));
		} else {
			postLines = new ArrayList<>();
		}

		bootKernel();

		if (bootState != BootState.RUNNING) {
			bootState = BootState.POST;
		}
	}

	private void bootKernel() {
		ICommandProcessor proc = machineType.commandProcessor();
		Kernel newKernel = proc.createKernel();
		if (newKernel == null) return;

		PeripheralBus bus = new AdjacentBlocksBus(level, worldPosition);
		newKernel.boot(bus, fileSystem);
		this.kernel = newKernel;
	}

	// Default setup
	private void setupDefaultFiles() {
		VirtualFileSystem.Node primaryTree = new VirtualFileSystem.Node("/", true);

		ICommandProcessor proc = machineType.commandProcessor();

		for (String filePath : machineType.defaultFiles()) {
			String normalized = filePath.replace('\\', '/');
			String[] segments = normalized.split("/");
			boolean isDir = filePath.endsWith("/") || filePath.endsWith("\\");

			VirtualFileSystem.Node dir = primaryTree;
			for (int i = 0; i < segments.length - 1; i++) {
				String segName = fileSystem.canonicalize(segments[i]);
				VirtualFileSystem.Node existing = dir.children.get(segName);
				if (existing == null) {
					existing = new VirtualFileSystem.Node(segName, true);
					dir.addChild(existing);
				}
				dir = existing;
			}

			String fileName = fileSystem.canonicalize(segments[segments.length - 1]);
			if (dir.children.containsKey(fileName)) continue;

			VirtualFileSystem.Node node = new VirtualFileSystem.Node(fileName, isDir);

			if (!isDir) {
				ExecutableRegistry.Entry exe =
						ExecutableRegistry.get(proc.osFamily(), fileName);
				if (exe != null) {
					node.content = exe.templateContent();
					node.executeBit = true;
				} else {
					String content = proc.defaultFileContent(fileName);
					node.content = content != null ? content : "";
				}
			}
			dir.addChild(node);
		}

		// Mount the primary tree at the ID that matches the machine type's
		// default path. DOS paths start with a drive letter ("C:\"),
		// POSIX paths start with "/" — the primary mount point follows the
		// same convention.
		String primaryMountId = machineType.defaultPath().startsWith("/") ? "/" : "C:";
		fileSystem.mountPersistent(primaryMountId, primaryTree);

		// Set the working directory into the newly mounted tree.
		fileSystem.setCurrentPath(machineType.defaultPath());
	}

	private void setupEnvironment() {
		environment.clear();
		environment.put("COMSPEC", "C:\\COMMAND.COM");
		environment.put("PATH", machineType.commandProcessor().defaultPath());
		environment.put("PROMPT", "$P$G");
	}

	private void setupDriveBays() {
		driveBays.clear();
		for (int i = 0; i < machineType.driveBays().size(); i++) {
			DriveBaySpec spec = machineType.driveBays().get(i);
			driveBays.add(new DriveBay(i, spec.type(),
					spec.dosLetter(), spec.posixDevice(), spec.posixMountPoint()));
		}
	}

	// Removable media

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

			if (bay.dosLetter() != null) {
				fileSystem.mountTransient(
						bay.dosLetter() + ":",
						root,
						!rmi.writable(),
						"floppy bay " + bay.index(),
						rmi.media().capacityBytes(),
						rmi.media().maxEntries());
			}
			if (bay.posixMountPoint() != null && bay.isPosixMounted()) {
				fileSystem.mountTransient(
						bay.posixMountPoint(),
						root,
						!rmi.writable(),
						"floppy bay " + bay.index(),
						rmi.media().capacityBytes(),
						rmi.media().maxEntries());
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

		if (bay.insertedStack().getItem() instanceof RemovableMediaItem rmi) {
			rmi.writeRoot(bay.insertedStack(), bay.mountedRoot());
		}

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

	// Command execution
	public String executeLine(String rawLine) {
		ICommandProcessor proc = machineType.commandProcessor();
		ShellDialect dialect = proc.shellDialect(kernel);
		if (dialect == null) {
			return proc.process(this, rawLine);
		}
		Pipeline pipeline = dialect.parse(rawLine);
		if (pipeline.isEmpty()) return "";

		StreamResolver resolver = proc.createStreamResolver();
		return new PipelineExecutor(resolver).execute(pipeline, this);
	}

	public String processCommand(String rawInput) {
		return machineType.commandProcessor().process(this, rawInput);
	}

	// Tick
	public static void tick(Level level, BlockPos pos, BlockState state,
							ComputerBlockEntity entity) {
		if (!level.isClientSide() && entity.activeUser != null) {
			Player player = level.getPlayerByUUID(entity.activeUser);
			if (player == null
					|| player.distanceToSqr(pos.getX() + 0.5,
											pos.getY() + 0.5,
											pos.getZ() + 0.5) > 64.0) {
				entity.activeUser = null;
				entity.setChanged();
			}
		}
	}

	// NBT
	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putString("MachineType", machineType.id());
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

		tag.putString("BootState", bootState.name());
		if (machineConfig != null) {
			tag.put("MachineConfig", serializeConfig(machineConfig));
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		if (tag.contains("MachineType")) {
			MachineType resolved = MachineTypeRegistry.get(tag.getString("MachineType"));
			if (resolved == null) {
				resolved = ComputerType.IBM_PC_AT;
			}
			this.machineType = resolved;
		}

		fileSystem.setPolicy(machineType.commandProcessor().fileNamePolicy());
		this.initializedDefaults = tag.getBoolean("InitializedDefaults");

		if (tag.contains("FileSystem")) {
			fileSystem.deserializeNBT(tag.getCompound("FileSystem"));
		}
		else if (!initializedDefaults) {
			// Very edge case: entity was saved before defaults were created
			setupDefaultFiles();
			setupEnvironment();
			initializedDefaults = true;
		}

		if (tag.contains("Environment")) {
			environment.clear();
			CompoundTag envTag = tag.getCompound("Environment");
			for (String k : envTag.getAllKeys()) environment.put(k, envTag.getString(k));
		}

		if (tag.contains("BootState")) {
			try {
				this.bootState = BootState.valueOf(tag.getString("BootState"));
			} catch (IllegalArgumentException e) {
				this.bootState = BootState.POST;
			}
		}

		if (tag.contains("MachineConfig")) {
			this.machineConfig = deserializeConfig(tag.getCompound("MachineConfig"));
		}
		if (machineConfig == null) {
			machineConfig = machineType.defaultConfig().get();
		}

		setupDriveBays();
		if (tag.contains("DriveBays")) {
			ListTag baysTag = tag.getList("DriveBays", Tag.TAG_COMPOUND);
			for (int i = 0; i < Math.min(baysTag.size(), driveBays.size()); i++) {
				driveBays.get(i).load(baysTag.getCompound(i));
			}
		}

		if (bootState == BootState.POST || bootState == BootState.SETUP) {
			bootState = BootState.POST;
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (kernel != null) {
			kernel.shutdown();
			kernel = null;
		}
	}

	// MachineConfig serialization
	private static CompoundTag serializeConfig(MachineConfig c) {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Time", c.systemTime());
		tag.putString("FloppyA", c.floppyA().name());
		tag.putString("FloppyB", c.floppyB().name());
		tag.putString("HD1", c.hardDisk1().name());
		tag.putString("HD2", c.hardDisk2().name());
		tag.putInt("BaseMem", c.baseMemoryKb());
		tag.putInt("ExtMem", c.extendedMemoryKb());
		tag.putBoolean("Coprocessor", c.mathCoprocessor());
		tag.putString("Display", c.primaryDisplay().name());
		return tag;
	}

	private static MachineConfig deserializeConfig(CompoundTag tag) {
		try {
			return new MachineConfig(
					tag.getLong("Time"),
					MachineConfig.FloppyType.valueOf(tag.getString("FloppyA")),
					MachineConfig.FloppyType.valueOf(tag.getString("FloppyB")),
					MachineConfig.DiskType.valueOf(tag.getString("HD1")),
					MachineConfig.DiskType.valueOf(tag.getString("HD2")),
					tag.getInt("BaseMem"),
					tag.getInt("ExtMem"),
					tag.getBoolean("Coprocessor"),
					MachineConfig.DisplayType.valueOf(tag.getString("Display")));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// User occupation
	public boolean isUsed() { return activeUser != null; }
	public UUID getActiveUser() { return activeUser; }

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
