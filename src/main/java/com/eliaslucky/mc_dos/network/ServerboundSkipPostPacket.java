package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.blocks.computer.BootState;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.List;

/**
 * Sent from client to server when the POST countdown finishes or the
 * player presses a key to skip it. Moves the machine from POST into
 * RUNNING so the shell prompt becomes active.
 */
public class ServerboundSkipPostPacket {
	private final BlockPos pos;

	public ServerboundSkipPostPacket(BlockPos pos) {
		this.pos = pos;
	}

	public ServerboundSkipPostPacket(FriendlyByteBuf buffer) {
		this.pos = buffer.readBlockPos();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;
			if (!(player.serverLevel().getBlockEntity(pos) instanceof ComputerBlockEntity computer)) return;
			if (computer.getBootState() != BootState.POST) return;

			computer.setBootState(BootState.RUNNING);

			ModMessages.sendToPlayer(
				new ClientboundTerminalStatePacket(
					pos, false, List.of(), 0, computer.getFileSystem().getCurrentPath()),
				player);

			List<String> bootLines = computer.getMachineType().osBootLines();
			if (!bootLines.isEmpty()) {
				String text = String.join("\n", bootLines) + "\n";
				ModMessages.sendToPlayer(
					new ClientboundTerminalOutputPacket(text,computer.getFileSystem().getCurrentPath()),
					player);
			}
		});
		ctx.setPacketHandled(true);
	}
}
