public interface DriverContext {

    /** The bus to scan for hardware. */
    PeripheralBus bus();

    /** Command-line / config parameters passed at load time.
     *  DOS: "DEVICE=... /PORT=0 /IRQ=5" gives {"PORT":"0","IRQ":"5"}
     *  Linux: "modprobe mccmd port=0" gives {"port":"0"}
     */
    Map<String, String> loadParams();

    /** Register a name in the OS's device namespace.
     *  DOS: name="MCCMD" → accessible as a device name
     *  Linux: name="mccmd0" → appears as /dev/mccmd0
     */
    void registerDevice(String name, DeviceHandler handler);

    /** Log a boot message. DOS: screen; Linux: dmesg. */
    void log(String message);
}
