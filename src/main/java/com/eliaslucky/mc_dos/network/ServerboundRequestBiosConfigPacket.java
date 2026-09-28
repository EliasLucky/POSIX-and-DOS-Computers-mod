package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.blocks.computer.BootState;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when the player presses the SETUP key
 * during POST.
 *
 * <p>The server responds with a {@link ClientboundBiosConfigPacket}
 * containing the machine's current {@code MachineConfig} and its BIOS
 * name, so the client can open the matching SETUP screen.
 *
 * <p>The state transition is: {@code POST} → {@code SETUP}. If the
 * machine is not in POST — because the countdown already expired, or
 * because another player opened SETUP first — the request is silently
 * ignored. That matches the real BIOS behaviour of only honouring the
 * DEL key during the POST window.
 *
 * @since 1.5
 */
public class ServerboundRequestBiosConfigPacket {
	/** The position of the computer block whose BIOS is being opened. */
	private final BlockPos pos;

	/**
	 * @param pos the computer block position
	 */
	public ServerboundRequestBiosConfigPacket(BlockPos pos) {
		this.pos = pos;
	}

	public ServerboundRequestBiosConfigPacket(FriendlyByteBuf buffer) {
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

			if (computer.getBootState() != BootState.POST) {
				return;
			}

			computer.setBootState(BootState.SETUP);

			// Send the machine's current configuration and BIOS name.
			// The client picks the SETUP screen by BIOS name.
			ModMessages.sendToPlayer(
					new ClientboundBiosConfigPacket(
							this.pos,
							computer.getMachineConfig(),
							computer.getMachineType().bios().name(),
					computer.getMachineType().bios().setupScreenId()),
					player);
		});
		ctx.setPacketHandled(true);
	}
}
