package com.eliaslucky.mc_dos.blocks.peripheral.mccmd;

import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.api.hardware.Peripheral;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

public class MinecraftCommandTranslatorBlockEntity extends BlockEntity implements Peripheral {
    private final ByteArrayOutputStream pendingInput = new ByteArrayOutputStream();
    private final Deque<String> outputLines = new ArrayDeque<>();

    public MinecraftCommandTranslatorBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.MCCMD_BLOCK.get(), pos, state);
    }

    // Peripheral
    @Override public String deviceClass() { return "mccmd"; }
    @Override public String vendorId()    { return "mc_dos"; }
    @Override public String productId()   { return "mccmd_v1"; }
    @Override public String description() { return "Minecraft command translator"; }
    @Override public boolean isReady()    { return level != null && !level.isClientSide(); }

    @Override
    public synchronized void write(byte[] data) {
        pendingInput.writeBytes(data);
    }

    @Override
    public synchronized byte[] read(int maxBytes) {
        StringBuilder sb = new StringBuilder();
        while (!outputLines.isEmpty() && sb.length() < maxBytes) {
            sb.append(outputLines.pollFirst());
            if (!outputLines.isEmpty()) sb.append('\n');
        }
        byte[] out = sb.toString().getBytes(StandardCharsets.UTF_8);
        if (out.length > maxBytes) {
            byte[] clipped = new byte[maxBytes];
            System.arraycopy(out, 0, clipped, 0, maxBytes);
            return clipped;
        }
        return out;
    }

    @Override public synchronized boolean hasData() { return !outputLines.isEmpty(); }

    @Override
    public int ioctl(int cmd, byte[] arg) {
        // 0 = no-op, 1 = flush, 2 = reset
        switch (cmd) {
            case 1: pendingInput.reset(); return 0;
            case 2: outputLines.clear();  return 0;
            default: return -1;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MinecraftCommandTranslatorBlockEntity be) {
        if (level.isClientSide()) return;
        be.processPendingCommands((ServerLevel) level);
    }

    private synchronized void processPendingCommands(ServerLevel level) {
        if (pendingInput.size() == 0) return;

        String text = pendingInput.toString(StandardCharsets.UTF_8);
        pendingInput.reset();

        for (String raw : text.split("\n")) {
            String cmd = raw.trim();
            if (cmd.isEmpty()) continue;

            // Strip an optional leading "/" so both "say hi" and "/say hi" work.
            if (cmd.startsWith("/")) cmd = cmd.substring(1);

            try {
                MinecraftServer server = level.getServer();
                if (server == null) continue;

                CommandSourceStack source = server.createCommandSourceStack()
                        .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                        .withLevel(level)
                        .withSuppressedOutput();

                int result = server.getCommands().performPrefixedCommand(source, cmd);
                outputLines.addLast("ok " + result);
            } catch (Exception ex) {
                outputLines.addLast("err " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }
    }

    // Buffered data is transient; no commands in flight.
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }
}