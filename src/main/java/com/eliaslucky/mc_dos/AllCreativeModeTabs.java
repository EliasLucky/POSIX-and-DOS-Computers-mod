package com.eliaslucky.mc_dos;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

//@EventBusSubscriber(bus = Bus.MOD)
public class AllCreativeModeTabs {
	private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Computers.MODID);

	public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.mc_dos.base"))
		.icon(() -> new ItemStack(AllBlocks.WHITE_IBM_PC_AT_COMPUTER.get()))
		.build()
	);

	public static final RegistryObject<CreativeModeTab> HARDWARE_CREATIVE_TAB = REGISTER.register("peripherals", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.mc_dos.peripherals"))
		.icon(() -> new ItemStack(AllBlocks.HARDWARE_LPC_MCCMD_BLOCK.get()))
		.build()
	);

	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
