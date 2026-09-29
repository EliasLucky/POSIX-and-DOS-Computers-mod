package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.blocks.computer.BootState;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * A boot-related action from the client.
 *
 * @since 1.5
 */
public class ServerboundBootActionPacket {
	public enum Action {
		/** Screen just opened; send current phase and POST lines. */
		REQUEST_STATE,
		/** Countdown ended or the player pressed a key at POST. */
		SKIP_POST,
		/** Player pressed DEL during POST. */
		ENTER_SETUP,
		/** Player saved and exited BIOS SETUP. */
		SAVE_BIOS
	}

	/** Match the current client countdown, for REQUEST_STATE. */
	public static final int DEFAULT_COUNTDOWN_SECONDS = 5;

	private final BlockPos pos;
	private final Action   action;
	private final MachineConfig config;    // only for SAVE_BIOS

	/** @param pos target computer; @param action what the player did. */
	public ServerboundBootActionPacket(BlockPos pos, Action action) {
		this(pos, action, null);
	}

	/** Full constructor; {@code config} only used for {@link Action#SAVE_BIOS}. */
	public ServerboundBootActionPacket(BlockPos pos, Action action, MachineConfig config) {
		this.pos = pos;
		this.action = action;
		this.config = config;
	}

	public ServerboundBootActionPacket(FriendlyByteBuf buffer) {
		this.pos	= buffer.readBlockPos();
		this.action = Action.values()[buffer.readByte()];
		this.config = buffer.readBoolean() ? readConfig(buffer) : null;
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
		buffer.writeByte(this.action.ordinal());
		buffer.writeBoolean(this.config != null);
		if (this.config != null) writeConfig(buffer, this.config);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;

			if (!(player.serverLevel().getBlockEntity(this.pos)
					instanceof ComputerBlockEntity computer)) {
				return;
			}

			switch (this.action) {
				case REQUEST_STATE -> handleRequestState(player, computer);
				case SKIP_POST	   -> handleSkipPost(player, computer);
				case ENTER_SETUP   -> handleEnterSetup(player, computer);
				case SAVE_BIOS	   -> handleSaveBios(player, computer);
			}
		});
		ctx.setPacketHandled(true);
	}

	// Action handlers
	private void handleRequestState(ServerPlayer player, ComputerBlockEntity computer) {
		// Every screen open is a cold boot.
		computer.powerOn();

		ModMessages.sendToPlayer(new ClientboundTerminalStatePacket(
				this.pos, ClientboundTerminalStatePacket.Phase.POST,
				computer.getFileSystem().getCurrentPath(),
				computer.getPostLines(), DEFAULT_COUNTDOWN_SECONDS,
				null, null, null), player);
	}

	private void handleSkipPost(ServerPlayer player, ComputerBlockEntity computer) {
		if (computer.getBootState() != BootState.POST) return;

		computer.setBootState(BootState.RUNNING);

		// Tell the client POST is over.
		ModMessages.sendToPlayer(new ClientboundTerminalStatePacket(
				this.pos, ClientboundTerminalStatePacket.Phase.RUNNING,
				computer.getFileSystem().getCurrentPath(),
				List.of(), 0, null, null, null), player);

		// Send OS boot lines so the terminal prints them.
		List<String> bootLines = computer.getMachineType().osBootLines();
		if (!bootLines.isEmpty()) {
			ModMessages.sendToPlayer(new ClientboundTerminalOutputPacket(
					String.join("\n", bootLines) + "\n",
					computer.getFileSystem().getCurrentPath()), player);
		}
	}

	private void handleEnterSetup(ServerPlayer player, ComputerBlockEntity computer) {
		if (computer.getBootState() != BootState.POST) return;

		computer.setBootState(BootState.SETUP);

		ModMessages.sendToPlayer(new ClientboundTerminalStatePacket(
				this.pos, ClientboundTerminalStatePacket.Phase.SETUP,
				computer.getFileSystem().getCurrentPath(),
				List.of(), 0,
				computer.getMachineConfig(),
				computer.getMachineType().bios().name(),
				computer.getMachineType().bios().setupScreenId()), player);
	}

	private void handleSaveBios(ServerPlayer player, ComputerBlockEntity computer) {
		if (computer.getBootState() != BootState.SETUP) return;
		if (this.config == null) return;

		computer.setMachineConfig(this.config);

		computer.powerOn();

		ModMessages.sendToPlayer(new ClientboundTerminalStatePacket(
				this.pos, ClientboundTerminalStatePacket.Phase.POST,
				computer.getFileSystem().getCurrentPath(),
				computer.getPostLines(), DEFAULT_COUNTDOWN_SECONDS,
				null, null, null), player);
	}

	// Config wire helpers
	public static MachineConfig readConfig(FriendlyByteBuf b) {
		long time = b.readLong();
		MachineConfig.FloppyType fA = MachineConfig.FloppyType.values()[b.readByte()];
		MachineConfig.FloppyType fB = MachineConfig.FloppyType.values()[b.readByte()];
		MachineConfig.DiskType hd1 = MachineConfig.DiskType.values()[b.readByte()];
		MachineConfig.DiskType hd2 = MachineConfig.DiskType.values()[b.readByte()];
		int baseMem = b.readVarInt();
		int extMem	= b.readVarInt();
		boolean copro = b.readBoolean();
		MachineConfig.DisplayType disp = MachineConfig.DisplayType.values()[b.readByte()];
		return new MachineConfig(time, fA, fB, hd1, hd2, baseMem, extMem, copro, disp);
	}

	public static void writeConfig(FriendlyByteBuf b, MachineConfig c) {
		b.writeLong(c.systemTime());
		b.writeByte(c.floppyA().ordinal());
		b.writeByte(c.floppyB().ordinal());
		b.writeByte(c.hardDisk1().ordinal());
		b.writeByte(c.hardDisk2().ordinal());
		b.writeVarInt(c.baseMemoryKb());
		b.writeVarInt(c.extendedMemoryKb());
		b.writeBoolean(c.mathCoprocessor());
		b.writeByte(c.primaryDisplay().ordinal());
	}
}
