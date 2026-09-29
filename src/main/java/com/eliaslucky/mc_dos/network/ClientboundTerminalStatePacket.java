package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientboundTerminalStatePacket {
    public enum Phase { POST, RUNNING, SETUP }

    private final BlockPos pos;
    private final Phase phase;
    private final String currentPath;

    // POST-only
    private final List<String> postLines;
    private final int countdownSeconds;

    // SETUP-only
    private final MachineConfig biosConfig;
    private final String biosName;
    private final String setupScreenId;

    public ClientboundTerminalStatePacket(BlockPos pos, Phase phase, String currentPath,
                                           List<String> postLines, int countdownSeconds,
                                           MachineConfig biosConfig, String biosName,
                                           String setupScreenId) {
        this.pos = pos;
        this.phase = phase;
        this.currentPath = currentPath == null ? "" : currentPath;
        this.postLines = postLines == null ? List.of() : List.copyOf(postLines);
        this.countdownSeconds = countdownSeconds;
        this.biosConfig = biosConfig;
        this.biosName = biosName;
        this.setupScreenId = setupScreenId;
    }

    public ClientboundTerminalStatePacket(FriendlyByteBuf b) {
        this.pos = b.readBlockPos();
        this.phase = Phase.values()[b.readByte()];
        this.currentPath = b.readUtf();

        List<String> lines = List.of();
        int count = 0;
        MachineConfig cfg = null;
        String name = null;
        String id = null;

        switch (phase) {
            case POST -> {
                int n = b.readVarInt();
                List<String> pl = new java.util.ArrayList<>(n);
                for (int i = 0; i < n; i++) pl.add(b.readUtf());
                lines = pl;
                count = b.readVarInt();
            }
            case SETUP -> {
                cfg  = ServerboundBootActionPacket.readConfig(b);
                name = b.readUtf();
                id   = b.readUtf();
            }
            case RUNNING -> { /* nothing extra */ }
        }

        this.postLines = lines;
        this.countdownSeconds = count;
        this.biosConfig = cfg;
        this.biosName = name;
        this.setupScreenId = id;
    }

    public void encode(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
        b.writeByte(phase.ordinal());
        b.writeUtf(currentPath);

        switch (phase) {
            case POST -> {
                b.writeVarInt(postLines.size());
                for (String line : postLines) b.writeUtf(line);
                b.writeVarInt(countdownSeconds);
            }
            case SETUP -> {
                ServerboundBootActionPacket.writeConfig(b, biosConfig);
                b.writeUtf(biosName);
                b.writeUtf(setupScreenId);
            }
            case RUNNING -> { /* nothing extra */ }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().screen instanceof ComputerTerminalScreen screen) {
                screen.onTerminalState(phase, currentPath, postLines, countdownSeconds,
                        biosConfig, biosName, setupScreenId);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
