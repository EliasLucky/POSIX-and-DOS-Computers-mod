package com.eliaslucky.mc_dos.blocks.computer.bus;

import com.eliaslucky.mc_dos.api.hardware.Peripheral;
import com.eliaslucky.mc_dos.api.hardware.PeripheralAddress;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class AdjacentBlocksBus implements PeripheralBus {

    private final Level level;
    private final BlockPos computerPos;

    public AdjacentBlocksBus(Level level, BlockPos computerPos) {
        this.level = level;
        this.computerPos = computerPos;
    }

    @Override
    public List<PeripheralAddress> scan() {
        Map<String, Integer> slotCounters = new HashMap<>();
        List<PeripheralAddress> found = new ArrayList<>();

        for (Direction d : Direction.values()) {
            BlockPos adj = computerPos.relative(d);
            BlockEntity be = level.getBlockEntity(adj);
            if (!(be instanceof Peripheral p)) continue;
            if (!p.isReady()) continue;

            int slot = slotCounters.merge(p.deviceClass(), 1, Integer::sum) - 1;
            found.add(new PeripheralAddress(
                    p.deviceClass(),
                    p.vendorId(),
                    p.productId(),
                    slot,
                    adj));
        }
        return found;
    }

    @Override
    public Peripheral get(PeripheralAddress addr) {
        BlockEntity be = level.getBlockEntity(addr.worldPos());
        return (be instanceof Peripheral p) ? p : null;
    }
}