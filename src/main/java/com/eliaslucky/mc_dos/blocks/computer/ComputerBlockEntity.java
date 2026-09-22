package com.eliaslucky.mc_dos.blocks.computer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.blocks.computer.processors.AbstractDosCommandProcessor;

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
		if (!initializedDefaults) {
			setupDefaultFiles();
			setupEnvironment();
			initializedDefaults = true;
			setChanged();		
		}
	}

	public Map<String, String> getEnvironment() { return environment; }
	
	private void setupDefaultFiles() {
		fileSystem.setCurrentPath(computerType.defaultPath);
		for (String filePath : computerType.defaultFiles) {
			boolean isDir = filePath.endsWith("/") || filePath.endsWith("\\");
			String cleanName = filePath.replaceAll("[/\\\\]", "");
			VirtualFileSystem.Node child = new VirtualFileSystem.Node(cleanName, isDir);
			fileSystem.getCurrentDir().addChild(child);
		}
	}

	private void setupEnvironment() {
		environment.clear();
		environment.put("COMSPEC", "C:\\COMMAND.COM");
		environment.put("PATH", computerType.commandProcessor instanceof AbstractDosCommandProcessor dos ? dos.defaultPath() : "");
		environment.put("PROMPT", "$P$G");
}

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
