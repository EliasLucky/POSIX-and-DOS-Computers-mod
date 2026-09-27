package com.eliaslucky.mc_dos.client;

import com.eliaslucky.mc_dos.Computers;
import com.eliaslucky.mc_dos.client.apps.TerminalApplicationRegistry;
import com.eliaslucky.mc_dos.client.apps.bios.BiosSetupRegistry;
import com.eliaslucky.mc_dos.client.apps.bios.AwardBiosSetupApplication;
import com.eliaslucky.mc_dos.client.apps.bios.IbmAtBiosSetupApplication;
import com.eliaslucky.mc_dos.client.apps.msd.MsdApplication;
import com.eliaslucky.mc_dos.client.apps.qbasic.QBasicApplication;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup.
 *
 * <p>Registers TUI applications and BIOS SETUP screens. Anything
 * registered here is available to every terminal in the session.
 * Addons ship their own class with the same
 * {@code @Mod.EventBusSubscriber} annotation and register their apps
 * the same way.
 *
 * @since 1.5
 */
@Mod.EventBusSubscriber(modid = Computers.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            // Terminal applications
            TerminalApplicationRegistry.register("QBASIC", QBasicApplication::new);
            //TerminalApplicationRegistry.register("EDIT",   EditApplication::new);
            TerminalApplicationRegistry.register("MSD",
                    (screen, args, content) -> new MsdApplication(screen, content));

            // BIOS setup screens
            // Keys must match Bios.setupScreenId(). The client looks up
            // the ID that arrived in ClientboundBiosConfigPacket.
            BiosSetupRegistry.register("IBM_AT_SETUP",
                    (screen, config, name) -> new IbmAtBiosSetupApplication(screen, config, name));
            BiosSetupRegistry.register("AWARD_SETUP",
                    (screen, config, name) -> new AwardBiosSetupApplication(screen, config, name));
        });
    }
}
