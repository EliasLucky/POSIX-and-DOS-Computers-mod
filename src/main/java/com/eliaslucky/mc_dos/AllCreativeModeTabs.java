package com.eliaslucky.furniture;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

//@EventBusSubscriber(bus = Bus.MOD)
public class AllCreativeModeTabs {
	private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Furniture.MODID);

	public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB = REGISTER.register("base", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.abstract_furniture.base"))
		.icon(() -> new ItemStack(AllBlocks.CHAIR.get()))
		.build()
	);

	public static final RegistryObject<CreativeModeTab> ROAD_CREATIVE_TAB = REGISTER.register("road", () -> CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.abstract_furniture.road"))
		.icon(() -> new ItemStack(AllBlocks.ORANGE_BARREL_ROAD_BARRIER.get()))
		.build()
	);

	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
