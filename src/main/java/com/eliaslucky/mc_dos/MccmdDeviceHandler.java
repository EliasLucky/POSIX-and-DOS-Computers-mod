public class MccmdDeviceHandler implements DeviceHandler {
    private final Peripheral peripheral;

    public MccmdDeviceHandler(Peripheral peripheral) { this.peripheral = peripheral; }

    @Override public void onWrite(byte[] data)      { peripheral.write(data); }
    @Override public byte[] onRead(int n)           { return peripheral.read(n); }
    @Override public int onIoctl(int cmd, byte[] a) { return peripheral.ioctl(cmd, a); }
    @Override public boolean hasData()              { return peripheral.hasData(); }
    @Override public String description()           { return "Minecraft command translator"; }
}
