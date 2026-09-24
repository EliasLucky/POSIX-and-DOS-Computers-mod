package com.eliaslucky.mc_dos.api.hardware;

import java.util.List;

public interface PeripheralBus {
    /** Enumerate peripherals visible to this computer, in a stable order. */
    List<PeripheralAddress> scan();

    Peripheral get(PeripheralAddress addr);
}
