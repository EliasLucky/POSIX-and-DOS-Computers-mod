public final class DosMccmdDriver implements Driver {

    private Peripheral peripheral;
    private DosDeviceTable deviceTable;

    @Override
    public String name() { return "MCCMD.SYS"; }

    @Override
    public DriverInitResult init(DriverContext ctx) {
        // DOS-style: find peripheral by command-line-specified slot.
        int slot = Integer.parseInt(ctx.loadParams().getOrDefault("SLOT", "0"));

        List<PeripheralAddress> scan = ctx.bus().scan();
        PeripheralAddress chosen = scan.stream()
                .filter(a -> a.deviceClass().equals("mccmd") && a.slot() == slot)
                .findFirst().orElse(null);

        if (chosen == null) {
            ctx.log("MCCMD.SYS: no matching device at slot " + slot);
            return DriverInitResult.FAILED;
        }

        this.peripheral = ctx.bus().get(chosen);
        ctx.registerDevice("MCCMD", new MccmdDeviceHandler(peripheral));
        ctx.log("MCCMD.SYS installed at slot " + slot);
        return DriverInitResult.OK;
    }

    @Override public void shutdown() { peripheral = null; }
}
