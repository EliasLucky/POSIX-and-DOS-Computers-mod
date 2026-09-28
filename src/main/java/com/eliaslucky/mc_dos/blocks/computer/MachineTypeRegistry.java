package com.eliaslucky.mc_dos.blocks.computer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registry of {@link MachineType} values.
 *
 * <p>Seeded at class load with every {@link ComputerType} value. Addons
 * register their own at mod setup:
 * <pre>{@code
 * MachineTypeRegistry.register(new MyMainframeType());
 * }</pre>
 *
 * <p>The ID is what gets persisted to NBT, so changing it after
 * release breaks existing saves. Treat IDs as stable!!!.
 *
 * @since 1.5
 */
public final class MachineTypeRegistry {
	private static final Map<String, MachineType> REGISTRY = new LinkedHashMap<>();

	private MachineTypeRegistry() {}

	/** Register a machine type. */
	public static void register(MachineType type) {
		REGISTRY.put(type.id(), type);
	}

	/** @return the type for the given ID, or {@code null}. */
	public static MachineType get(String id) {
		return REGISTRY.get(id);
	}

	/** @return whether a type is registered under the given ID. */
	public static boolean exists(String id) {
		return REGISTRY.containsKey(id);
	}

	/** @return an unmodifiable view of all registered types. */
	public static Map<String, MachineType> all() {
		return Map.copyOf(REGISTRY);
	}

	static {
		for (ComputerType type : ComputerType.values()) {
			register(type);
		}
	}
}
