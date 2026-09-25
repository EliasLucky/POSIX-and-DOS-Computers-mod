package com.eliaslucky.mc_dos.api.hardware;

import java.util.List;

/**
 * The hardware bus of a single computer. Enumerates attached
 * peripherals and resolves addresses back to their instances.
 *
 * <p>The bus is created once per boot and shared by every driver that
 * the kernel loads for that machine. It is the only interface drivers
 * use to find their hardware — they never scan the Minecraft world
 * directly.
 *
 * <h2>Default implementation</h2>
 * The built-in {@code AdjacentBlocksBus} returns every
 * {@link Peripheral}-implementing block entity adjacent to the computer
 * (the six orthogonal neighbours). A future "extension cable" block
 * would provide an alternative implementation that walks a chain of
 * blocks.
 *
 * <h2>Driver usage</h2>
 * <pre>{@code
 * List<PeripheralAddress> matches = ctx.bus().scan().stream()
 *         .filter(a -> a.deviceClass().equals("mccmd"))
 *         .toList();
 * if (matches.isEmpty()) return DriverInitResult.FAILED;
 *
 * Peripheral p = ctx.bus().get(matches.get(0));
 * ctx.registerDevice("MCCMD", DeviceHandler.of(p));
 * }</pre>
 *
 * @see Peripheral
 * @see PeripheralAddress
 */
public interface PeripheralBus {
    /**
     * Enumerate all peripherals reachable from this computer.
     *
     * <p>The returned list is stable within a single scan (same order
     * each call until the world changes). Slot numbers are assigned
     * per device class in the order peripherals appear.
     *
     * @return an immutable, possibly-empty list of addresses
     */
    List<PeripheralAddress> scan();

    /**
     * Resolve an address to the live peripheral. May return
     * {@code null} if the block was removed between a scan and this
     * call — drivers should handle that case rather than assume the
     * peripheral is always present.
     *
     * @param addr an address previously returned by {@link #scan()}
     * @return the peripheral, or {@code null} if it is no longer present
     */
    Peripheral get(PeripheralAddress addr);
}