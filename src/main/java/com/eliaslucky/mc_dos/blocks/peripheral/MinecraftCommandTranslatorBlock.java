public class MinecraftCommandTranslatorBlock extends Block
        implements EntityBlock, Peripheral {

    @Override public String deviceClass() { return "mccmd"; }
    @Override public String vendorId()    { return "mc_dos"; }
    @Override public String productId()   { return "mccmd_v1"; }

    @Override public void write(byte[] data) {
        // Queue the bytes as a command for the server tick to run.
    }
    // ...
}
