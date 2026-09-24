public final class LinuxMccmdDriver implements Driver {

    private final List<Peripheral> bound = new ArrayList<>();

    @Override public String name() { return "mccmd.ko"; }

    @Override
    public DriverInitResult init(DriverContext ctx) {
        // Linux-style: bind to ALL matching peripherals (auto-enumerated).
        List<PeripheralAddress> matches = ctx.bus().scan().stream()
                .filter(a -> a.deviceClass().equals("mccmd"))
                .toList();

        if (matches.isEmpty()) return DriverInitResult.FAILED;

        for (PeripheralAddress addr : matches) {
            Peripheral p = ctx.bus().get(addr);
            String devName = ctx.registerDeviceName("mccmd", new MccmdDeviceHandler(p));
            bound.add(p);
            ctx.log("mccmd: registered /dev/" + devName);
        }
        return DriverInitResult.OK;
    }

    @Override public void shutdown() { bound.clear(); }
}
