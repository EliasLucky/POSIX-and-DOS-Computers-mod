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
 * <p>The server replies with a {@link ClientboundBiosConfigPacket}
 * carrying the machine's current {@link MachineConfig} and the
 * identifier of the BIOS's setup screen. The client uses the
 * identifier to look up the correct {@code TerminalApplication} in
 * {@code BiosSetupRegistry}, so no BIOS name matching is needed on the
 * client.
 *
 * <p>State transition: {@code POST} → {@code SETUP}. If the machine is
 * not in POST — because the countdown already expired, the player
 * skipped it, or another player already opened SETUP — the request is
 * ignored. That matches the real BIOS behaviour of only honouring the
 * DEL key during the POST window.
 *
 * @since 1.0
 */
public class ServerboundRequestBiosConfigPacket {

    /** The block position of the computer whose BIOS is being opened. */
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

            // Only honour the request during POST. Anything else means
            // the window has already passed, or someone else is in SETUP.
            if (computer.getBootState() != BootState.POST) {
                return;
            }

            // Transition to SETUP so the shell stays inactive while the
            // player is editing BIOS settings.
            computer.setBootState(BootState.SETUP);

            // Reply with the machine's configuration and the identifier
            // of the setup screen the client should open. The BIOS knows
            // its own screen ID — that's the whole point of the field.
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