package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server to client in response to a request for the BIOS
 * config. Carries the full machine configuration so the client can
 * display and edit it in the SETUP screen.
 */
public class ClientboundBiosConfigPacket {
    private final BlockPos pos;
    private final MachineConfig config;
    private final String biosName;
    private final String setupScreenId;

    public ClientboundBiosConfigPacket(BlockPos pos, MachineConfig config, String biosName, String setupScreenId) {
        this.pos = pos;
        this.config = config;
        this.biosName = biosName;
        this.setupScreenId = setupScreenId;
    }

    public ClientboundBiosConfigPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.biosName = buffer.readUtf();

        long time = buffer.readLong();
        MachineConfig.FloppyType floppyA = MachineConfig.FloppyType.values()[buffer.readByte()];
        MachineConfig.FloppyType floppyB = MachineConfig.FloppyType.values()[buffer.readByte()];
        MachineConfig.DiskType hd1 = MachineConfig.DiskType.values()[buffer.readByte()];
        MachineConfig.DiskType hd2 = MachineConfig.DiskType.values()[buffer.readByte()];
        int baseMem = buffer.readVarInt();
        int extMem = buffer.readVarInt();
        boolean copro = buffer.readBoolean();
        MachineConfig.DisplayType display = MachineConfig.DisplayType.values()[buffer.readByte()];

        this.config = new MachineConfig(
                time, floppyA, floppyB, hd1, hd2,
                baseMem, extMem, copro, display);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(biosName);

        buffer.writeLong(config.systemTime());
        buffer.writeByte(config.floppyA().ordinal());
        buffer.writeByte(config.floppyB().ordinal());
        buffer.writeByte(config.hardDisk1().ordinal());
        buffer.writeByte(config.hardDisk2().ordinal());
        buffer.writeVarInt(config.baseMemoryKb());
        buffer.writeVarInt(config.extendedMemoryKb());
        buffer.writeBoolean(config.mathCoprocessor());
        buffer.writeByte(config.primaryDisplay().ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().screen instanceof ComputerTerminalScreen screen) {
                screen.onBiosConfigReceived(config, biosName);
            }
        }));
        ctx.setPacketHandled(true);
    }
}
