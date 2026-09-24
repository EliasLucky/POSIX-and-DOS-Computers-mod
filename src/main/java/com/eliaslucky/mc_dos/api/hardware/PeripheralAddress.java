package com.eliaslucky.mc_dos.api.hardware;

import net.minecraft.core.BlockPos;

/**
 * A way to name a peripheral across bus scans. The slot index is
 * per-class so a machine with two printers gets slot 0 and slot 1.
 */
public record PeripheralAddress(
        String deviceClass,
        String vendorId,
        String productId,
        int slot,
        BlockPos worldPos) {

    @Override
    public String toString() {
        return deviceClass + ":" + vendorId + ":" + productId + "@" + slot;
    }
}
