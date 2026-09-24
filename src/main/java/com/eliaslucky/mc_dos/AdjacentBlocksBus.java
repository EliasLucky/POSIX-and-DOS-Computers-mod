public class AdjacentBlocksBus implements PeripheralBus {
    private final Level level;
    private final BlockPos computerPos;

    public AdjacentBlocksBus(Level level, BlockPos pos) {
        this.level = level;
        this.computerPos = pos;
    }

    @Override
    public List<PeripheralAddress> scan() {
        Map<String, Integer> slotCounters = new HashMap<>();
        List<PeripheralAddress> found = new ArrayList<>();

        for (Direction d : Direction.values()) {
            BlockPos adj = computerPos.relative(d);
            BlockEntity be = level.getBlockEntity(adj);
            if (be instanceof Peripheral p) {
                int slot = slotCounters.merge(p.deviceClass(), 1, Integer::sum) - 1;
                found.add(new PeripheralAddress(
                        p.deviceClass(), p.vendorId(), p.productId(),
                        slot, adj));
            }
        }
        return found;
    }

    @Override
    public Peripheral get(PeripheralAddress addr) {
        BlockEntity be = level.getBlockEntity(addr.worldPos());
        return (be instanceof Peripheral p) ? p : null;
    }
}
