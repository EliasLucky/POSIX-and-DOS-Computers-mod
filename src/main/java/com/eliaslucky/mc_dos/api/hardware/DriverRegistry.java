package com.eliaslucky.mc_dos.api.hardware;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Central registry for {@link Driver} implementations.
 *
 * <p>Addons register their drivers here during mod setup. Each kernel
 * decides how to install and load them.
 *
 * <h2>Initialization</h2>
 * <ul>
 * 	<li>DOS copies the file at {@link Entry#installPath()} into the VFS,
 * 	    and loads the driver when the line {@code DEVICE=} in @{code CONFIG.SYS} is parsed.</li>
 * 	<li></li>
 * 	<li></li>
 * </ul>
 *
 * <h2>Registration</h2>
 * Call from {@code FMLCommonSetupEvent}, inside {@code event.enqueueWork}:
 * <pre>{@code
 * // DOS: needs a file and a CONFIG.SYS line.
 * DriverRegistry.register("dos", "MCCMD", "C:\\DRIVERS\\MCCMD.SYS", "MZ\u0090...MCCMD\n", DosMccmdDriver::new);
 *
 * // Linux: needs a .ko under /lib/modules.
 * DriverRegistry.register("linux", "mccmd", "/lib/modules/2.4.20-8/kernel/drivers/char/mccmd.ko", "\u007fELD mccmd.ko\n", LinuxMccmdDriver::new);
 * 
 * // Unix v7: compiled in the kernel. no file.
 * DriverRegistry.register("unix", "MCCMD", UnixV7MccmdDriver::new);
 * }</pre>
 *
 * <h2>Key format</h2>
 * <ul>
 *	 <li>{@code osFamily} - matched case-insensitively. Standard values
 *		 are {@code "dos"}, {@code "unix"}, {@code "linux"}, but you can
 *		 invent new families (e.g. {@code "bsd"}) provided you also write
 *		 a kernel that loads from them.</li>
 *	 <li>{@code driverName} - uppercased before being used as a key.
 *		 This matches how {@code CONFIG.SYS} parses {@code DEVICE=} lines
 *		 and how Linux kernel modules conventionally name themselves.</li>
 * </ul>
 *
 * <h2>Duplicate keys</h2>
 * Registering the same {@code (family, name)} pair twice silently
 * overwrites the previous factory. If two mods both claim the same
 * slot, the later one wins - there is no warning. Namespace your
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
	 * @param osFamily	 the OS family, already lowercased
	 * @param driverName the driver name, already uppercased
	 */
	public record Key(String osFamily, String driverName) {}

	/**
	 * One registered driver, with everything a kernerl needs to isntall and load it.
	 * 
	 * @param osFamily        the OS family, already lowercased
	 * @param driverName      the driver name, already uppercased
	 * @param installPath     VFS path of the driver file, or {@code null}
	 * @param templateContent file body to seed, or {@code null} for empty
	 * @param factory         creates fresh {@link Driver} instances
	 *
	 */
	public record Entry(String osFamily, String key, String installPath, String templateContent, Supplier<Driver> factory) {
		/** @return {@code true} if this driver has an on-disk file. */ 
		public boolean hasFile() { return installPath != null; }
	}

	private static final Map<Key, Entry> FACTORIES = new HashMap<>();

	private DriverRegistry() {}

	/**
	 * Register a driver factory. Use when the OS has a driver file to seed.
	 *
	 * @param osFamily	  the OS family key (matched case-insensitively)
	 * @param driverName      the driver name (uppercased automatically)
	 * @param installPath     VFS path, or {@code null}
	 * @param templateContent file body, or {@code null} for empty
	 * @param f	          the factory that creates the driver
	 */
	public static void register(String osFamily, String driverName, String installPath, String templateContent, Supplier<Driver> f) {
		Key key = key(osFamily, driverName);
		FACTORIES.put(key, new Entry(key.osFamily(), key.driverName(), installPath, templateContent, f));
	}

	/**
	 * Register a driver that have no on-disk presence.
	 * For example, Unix v7 drivers which are compiled into the kernel.
	 *
	 * @param osFamily  the OS family key
	 * @param driveName the driver name
	 * @param f         the factory that creates the driver
	 */
	public static void register(String osFamily, String driverName, Supplier<Driver> f) {
		register(osFamily, driverName, null, null, f);
	}

	/**
	 * Instantiate a driver. Called by the kernel at boot.
	 *
	 * @param osFamily	 the OS family key
	 * @param driverName the driver name
	 * @return a fresh driver instance, or {@code null} if no factory is
	 *		   registered for that pair
	 */
	public static Driver load(String osFamily, String driverName) {
		Entry f = FACTORIES.get(key(osFamily,driverName));
		return f == null ? null : f.factory().get();
	}

	/**
	 * Look up and entry without isntantiating the driver.
	 *
	 * @param osFamily the OS family key
	 * @param driverName the driver name
	 * @return the entry, or {@code null}
	 */
	public static Entry get(String osFamily, String driverName) {
		return FACTORIES.get(key(osFamily, driverName));
	}

	/**
	 * Check whether a driver is registered without instantiating it.
	 *
	 * @param osFamily	 the OS family key
	 * @param driverName the driver name
	 * @return {@code true} if a factory exists for that pair
	 */
	public static boolean exists(String osFamily, String driverName) {
		return FACTORIES.containsKey(key(osFamily,driverName));
	}

	/**
	 * Every driver registered for an OS family.
	 * Kernels use this to seed files and probe the bus.
	 *
	 * @param osFamily the OS family key
	 * @return an immutable list, never {@code null}
	 */
	public static List<Entry> forFamily(String osFamily) {
		String family = osFamily.toLowerCase(Locale.ROOT);
		List<Entry> out = new ArrayList<>();
		for (Entry e : FACTORIES.values()) {
			if (e.osFamily().equals(family)) out.add(e);
		}
		return Collections.unmodifiableList(out);
	}

	private static Key key(String osFamily, String driverName) {
		return new Key(osFamily.toLowerCase(Locale.ROOT), driverName.toUpperCase(Locale.ROOT));
	}
}
