package com.eliaslucky.mc_dos.api.hardware;

public interface PeripheralBus {
    /** Enumerate peripherals visible to this computer, in a stable order. */
    List<PeripheralAddress> scan();

    Peripheral get(PeripheralAddress addr);
}

/** A stable way to name a peripheral across scans. */
public record PeripheralAddress(
        String deviceClass,
        String vendorId,
        String productId,
        int slot,                     // Nth peripheral of this class on the bus
        BlockPos worldPos             // actual block, for reference
) {}
