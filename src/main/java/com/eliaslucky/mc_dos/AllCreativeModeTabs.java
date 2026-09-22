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
		.icon(() -> new ItemStack(AllBlocks.CHAIR.get()))
		.build()
	);

	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
