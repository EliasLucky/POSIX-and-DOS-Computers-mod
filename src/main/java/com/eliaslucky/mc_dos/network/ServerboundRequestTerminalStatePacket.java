package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.blocks.computer.BootState;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when a terminal screen opens, to request
 * the machine's current boot state and POST output.
 *
 * <p>This packet exists because the client opens the screen and the
 * server sends the state packet in the same tick. If the state packet
 * arrives first  which happens under load or with even slight network
 * latency	the client's packet handler runs while
 * {@code Minecraft.getInstance().screen} is still the previous screen,
 * and the packet is dropped.
 *
 * @since 1.5
 */
public class ServerboundRequestTerminalStatePacket {
	/**
	 * How long the client should show the "Press DEL to enter SETUP"
	 * countdown. Matches the value the client uses locally for the
	 * same purpose.
	 */
	public static final int DEFAULT_COUNTDOWN_SECONDS = 5;

	/** The block position of the computer whose terminal is opening. */
	private final BlockPos pos;

	/**
	 * @param pos the computer block position
	 */
	public ServerboundRequestTerminalStatePacket(BlockPos pos) {
		this.pos = pos;
	}

	public ServerboundRequestTerminalStatePacket(FriendlyByteBuf buffer) {
		this.pos = buffer.readBlockPos();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
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

			computer.powerOn();

			// Reply with the machine's current state.
			ModMessages.sendToPlayer(
					new ClientboundTerminalStatePacket(
							this.pos,
							computer.getBootState() == BootState.POST,
							computer.getPostLines(),
							DEFAULT_COUNTDOWN_SECONDS,
							computer.getFileSystem().getCurrentPath()),
					player);
		});
		ctx.setPacketHandled(true);
	}
}
