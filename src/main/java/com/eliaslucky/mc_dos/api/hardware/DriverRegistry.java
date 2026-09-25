package com.eliaslucky.mc_dos.api.hardware;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central registry for {@link Driver} implementations.
 *
 * <p>Addons register their drivers here during mod setup. The kernel
 * looks them up by (OS family, driver name) at boot.
 *
 * <h2>Registration</h2>
 * Call from {@code FMLCommonSetupEvent}, inside {@code event.enqueueWork}:
 * <pre>{@code
 * event.enqueueWork(() -> {
 *     DriverRegistry.register("dos",   "MCCMD", DosMccmdDriver::new);
 *     DriverRegistry.register("unix",  "MCCMD", UnixMccmdDriver::new);
 *     DriverRegistry.register("linux", "MCCMD", LinuxMccmdDriver::new);
 * });
 * }</pre>
 *
 * <h2>Key format</h2>
 * <ul>
 *   <li>{@code osFamily} — matched case-insensitively. Standard values
 *       are {@code "dos"}, {@code "unix"}, {@code "linux"}, but you can
 *       invent new families (e.g. {@code "bsd"}) provided you also write
 *       a kernel that loads from them.</li>
 *   <li>{@code driverName} — uppercased before being used as a key.
 *       This matches how {@code CONFIG.SYS} parses {@code DEVICE=} lines
 *       and how Linux kernel modules conventionally name themselves.</li>
 * </ul>
 *
 * <h2>Duplicate keys</h2>
 * Registering the same {@code (family, name)} pair twice silently
 * overwrites the previous factory. If two mods both claim the same
 * slot, the later one wins — there is no warning. Namespace your
 * driver names if you expect collisions.
 *
 * @see Driver
 * @see DriverInitResult
 */
public final class DriverRegistry {
    /**
     * A factory that produces fresh {@link Driver} instances. The kernel
     * calls this once per boot so drivers don't share state between
     * machines.
     */
    @FunctionalInterface
    public interface DriverFactory {
        /**
         * @return a new driver instance; must not return {@code null}
         */
        Driver create();
    }

    /**
     * Composite registry key.
     *
     * @param osFamily   the OS family, already lowercased
     * @param driverName the driver name, already uppercased
     */
    public record Key(String osFamily, String driverName) {}

    private static final Map<Key, DriverFactory> FACTORIES = new HashMap<>();

    private DriverRegistry() {}

    /**
     * Register a driver factory.
     *
     * @param osFamily   the OS family key (matched case-insensitively)
     * @param driverName the driver name (uppercased automatically)
     * @param f          the factory that creates the driver
     */
    public static void register(String osFamily, String driverName, DriverFactory f) {
        FACTORIES.put(new Key(osFamily.toLowerCase(Locale.ROOT),
                              driverName.toUpperCase(Locale.ROOT)), f);
    }

    /**
     * Instantiate a driver. Called by the kernel at boot.
     *
     * @param osFamily   the OS family key
     * @param driverName the driver name
     * @return a fresh driver instance, or {@code null} if no factory is
     *         registered for that pair
     */
    public static Driver load(String osFamily, String driverName) {
        DriverFactory f = FACTORIES.get(new Key(
                osFamily.toLowerCase(Locale.ROOT),
                driverName.toUpperCase(Locale.ROOT)));
        return f == null ? null : f.create();
    }

    /**
     * Check whether a driver is registered without instantiating it.
     *
     * @param osFamily   the OS family key
     * @param driverName the driver name
     * @return {@code true} if a factory exists for that pair
     */
    public static boolean exists(String osFamily, String driverName) {
        return FACTORIES.containsKey(new Key(
                osFamily.toLowerCase(Locale.ROOT),
                driverName.toUpperCase(Locale.ROOT)));
    }
}