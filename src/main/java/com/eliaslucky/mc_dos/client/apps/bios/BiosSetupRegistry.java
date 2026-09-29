package com.eliaslucky.mc_dos.client.apps.bios;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of BIOS SETUP screens, keyed by the ID that
 * {@code Bios.setupScreenId()} returns.
 *
 * <p>Addons that ship their own BIOS register their setup application
 * here at client setup:
 *
 * <pre>{@code
 * BiosSetupRegistry.register("MY_VENDOR_SETUP", (screen, config, name) ->
 *		   new MyVendorSetupApplication(screen, config, name));
 * }</pre>
 *
 * <p>The lookup path is:
 * <ol>
 *	 <li>Player presses DEL during POST.</li>
 *	 <li>Client sends {@code ServerboundRequestBiosConfigPacket}.</li>
 *	 <li>Server replies with {@code ClientboundBiosConfigPacket}
 *		 containing {@code bios.setupScreenId()}.</li>
 *	 <li>Client looks up the ID here and constructs the screen.</li>
 * </ol>
 *
 * <p>If no setup screen is registered for an ID, the client prints a
 * diagnostic line rather than crashing. That gives a
 * clear signal that forgot to register.
 *
 * @since 1.5
 */
public final class BiosSetupRegistry {
	/**
	 * Factory that constructs a BIOS SETUP screen.
	 *
	 * <p>The factory receives the machine's current configuration so
	 * the screen can display it, and the BIOS name for the title bar.
	 * The screen is expected to send its own save packet on exit; it
	 * does not need a callback because the network layer already has
	 * {@code ServerboundSaveBiosConfigPacket}.
	 */
	@FunctionalInterface
	public interface SetupFactory {
		/**
		 * @param screen   the hosting terminal screen
		 * @param config   the machine's current configuration
		 * @param biosName the BIOS name, for the title bar
		 * @return a fully constructed setup application
		 */
		TerminalApplication create(ComputerTerminalScreen screen,MachineConfig config,String biosName);
	}

	private static final Map<String, SetupFactory> FACTORIES = new HashMap<>();

	private BiosSetupRegistry() {}

	/**
	 * Register a setup screen under an ID. Called by mods during
	 * client setup. Registering twice under the same ID overwrites
	 * the earlier registration.
	 *
	 * @param setupScreenId the ID that a matching {@code Bios} returns from {@code setupScreenId()}
	 * @param factory		the constructor
	 */
	public static void register(String setupScreenId, SetupFactory factory) {
		FACTORIES.put(setupScreenId, factory);
	}

	/**
	 * Look up a setup factory by ID.
	 *
	 * @param setupScreenId the ID
	 * @return the factory, or {@code null} if none is registered
	 */
	public static SetupFactory get(String setupScreenId) {
		return FACTORIES.get(setupScreenId);
	}

	/**
	 * @param setupScreenId the ID
	 * @return {@code true} if a factory is registered
	 */
	public static boolean exists(String setupScreenId) {
		return FACTORIES.containsKey(setupScreenId);
	}
}
