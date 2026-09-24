public class DosDeviceTable {
    /** DOS device names are 1-8 chars, no extension, and can't collide
     *  with reserved names (CON, PRN, AUX, NUL, CLOCK$, LPT1-3, COM1-4). */
    private final Map<String, DeviceHandler> devices = new LinkedHashMap<>();

    private static final Set<String> RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL", "CLOCK$",
            "LPT1", "LPT2", "LPT3",
            "COM1", "COM2", "COM3", "COM4");

    public boolean register(String name, DeviceHandler h) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.length() < 1 || upper.length() > 8) return false;
        if (RESERVED.contains(upper)) return false;
        if (devices.containsKey(upper)) return false;
        devices.put(upper, h);
        return true;
    }

    public DeviceHandler lookup(String name) {
        return devices.get(name.toUpperCase(Locale.ROOT));
    }
    
    public boolean contains(String name) {
        return devices.containsKey(name.toUpperCase(Locale.ROOT));
    }

    public List<String> names() { return List.copyOf(devices.keySet()); }
    public void clear() { devices.clear(); }
}
