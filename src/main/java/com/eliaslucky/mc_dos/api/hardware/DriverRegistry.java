package com.eliaslucky.mc_dos.api.hardware;

public final class DriverRegistry {

    /** OS family = "dos" or "posix". Addons extend by adding new families. */
    public interface DriverFactory {
        Driver create();
    }

    public record Key(String osFamily, String driverName) {}

    private static final Map<Key, DriverFactory> FACTORIES = new HashMap<>();

    public static void register(String osFamily, String driverName, DriverFactory f) {
        FACTORIES.put(new Key(osFamily, driverName.toUpperCase(Locale.ROOT)), f);
    }

    public static Driver load(String osFamily, String driverName) {
        DriverFactory f = FACTORIES.get(new Key(osFamily, driverName.toUpperCase(Locale.ROOT)));
        return f == null ? null : f.create();
    }

    public static boolean exists(String osFamily, String driverName) {
        return FACTORIES.containsKey(new Key(osFamily, driverName.toUpperCase(Locale.ROOT)));
    }
}
