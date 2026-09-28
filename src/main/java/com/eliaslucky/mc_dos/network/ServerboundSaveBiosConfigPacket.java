package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.blocks.computer.BootState;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when the player saves changes in a BIOS
 * SETUP screen.
 *
 * <p>The server stores the new {@link MachineConfig} on the block
 * entity, moves the machine into {@link BootState#RUNNING}, and sends
 * back a fresh {@link ClientboundTerminalStatePacket} so the client
 * knows the POST phase is over and the shell prompt can appear.
 *
 * <p>If the machine is not currently in {@link BootState#SETUP} — for
 * example, another player has already dismissed the setup screen or
 * the block was unloaded — the request is silently ignored.
 *
 * @since 1.5
 */
public class ServerboundSaveBiosConfigPacket {
	private final BlockPos pos;
	private final MachineConfig config;

	/**
	 * @param pos	 the computer block whose config is being saved
	 * @param config the new configuration
	 */
	public ServerboundSaveBiosConfigPacket(BlockPos pos, MachineConfig config) {
		this.pos = pos;
		this.config = config;
	}

	public ServerboundSaveBiosConfigPacket(FriendlyByteBuf buffer) {
		this.pos = buffer.readBlockPos();

		long time = buffer.readLong();
		MachineConfig.FloppyType floppyA =
				MachineConfig.FloppyType.values()[buffer.readByte()];
		MachineConfig.FloppyType floppyB =
				MachineConfig.FloppyType.values()[buffer.readByte()];
		MachineConfig.DiskType hd1 =
				MachineConfig.DiskType.values()[buffer.readByte()];
		MachineConfig.DiskType hd2 =
				MachineConfig.DiskType.values()[buffer.readByte()];
		int baseMem = buffer.readVarInt();
		int extMem	= buffer.readVarInt();
		boolean coprocessor = buffer.readBoolean();
		MachineConfig.DisplayType display =
				MachineConfig.DisplayType.values()[buffer.readByte()];

		this.config = new MachineConfig(
				time, floppyA, floppyB, hd1, hd2,
				baseMem, extMem, coprocessor, display);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);

		buffer.writeLong(this.config.systemTime());
		buffer.writeByte(this.config.floppyA().ordinal());
		buffer.writeByte(this.config.floppyB().ordinal());
		buffer.writeByte(this.config.hardDisk1().ordinal());
		buffer.writeByte(this.config.hardDisk2().ordinal());
		buffer.writeVarInt(this.config.baseMemoryKb());
		buffer.writeVarInt(this.config.extendedMemoryKb());
		buffer.writeBoolean(this.config.mathCoprocessor());
		buffer.writeByte(this.config.primaryDisplay().ordinal());
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;

			if (!(player.serverLevel().getBlockEntity(this.pos) instanceof ComputerBlockEntity computer)) {
				return;
			}

			if (computer.getBootState() != BootState.SETUP) {
				return;
			}

			computer.setMachineConfig(this.config);
			computer.powerOn();

			ModMessages.sendToPlayer(new ClientboundTerminalStatePacket(this.pos, false,  computer.getPostLines(), 5, computer.getFileSystem().getCurrentPath()), player);
		});
		ctx.setPacketHandled(true);
	}
}
