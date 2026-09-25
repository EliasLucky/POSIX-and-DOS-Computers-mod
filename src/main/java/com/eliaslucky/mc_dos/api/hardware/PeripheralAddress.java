package com.eliaslucky.mc_dos.api.hardware;

import net.minecraft.core.BlockPos;

/**
 * A stable identifier for a {@link Peripheral} found on the bus.
 *
 * <p>Peripherals come and go as players place and break blocks, but
 * within a single {@link PeripheralBus#scan()} the addresses are stable
 * and unique. Drivers use them to remember which hardware they bound to.
 *
 * <h2>Slot numbering</h2>
 * The {@code slot} field is assigned per {@link #deviceClass()} during
 * the scan. If two printers are adjacent to a computer, they get slots
 * 0 and 1 in that class. Two mccmd blocks get slots 0 and 1 in the
 * mccmd class, independent of the printers. This lets a driver say
 * "bind to the second printer" without caring which physical side of
 * the computer it is on.
 *
 * <h2>Stability</h2>
 * A given {@code PeripheralAddress} is guaranteed stable across
 * {@code get()} calls within the same session. It is <em>not</em>
 * persisted — after a world reload, slot numbers may be reassigned.
 * Store the {@link #worldPos()} if you need to survive a reload.
 *
 * @param deviceClass the peripheral's {@link Peripheral#deviceClass()}
 * @param vendorId    the peripheral's {@link Peripheral#vendorId()}
 * @param productId   the peripheral's {@link Peripheral#productId()}
 * @param slot        zero-based index within the device class
 * @param worldPos    the block position, for reference and persistence
 *
 * @see PeripheralBus#scan()
 * @see PeripheralBus#get(PeripheralAddress)
 */
public record PeripheralAddress( String deviceClass, String vendorId, String productId, int slot, BlockPos worldPos) {
    @Override
    public String toString() {
        return deviceClass + ":" + vendorId + ":" + productId + "@" + slot;
    }
}