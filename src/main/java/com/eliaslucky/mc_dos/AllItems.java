package com.eliaslucky.mc_dos;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AllItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Computers.MODID);

	public static final RegistryObject<Item> WHITE_DESK_CABINET = ITEMS.register("white_desk_cabinet",
			() -> new BlockItem(AllBlocks.WHITE_DESK_CABINET.get(), new Item.Properties())
		);

	public static final RegistryObject<Item> BLACK_DESK_CABINET = ITEMS.register("black_desk_cabinet",
			() -> new BlockItem(AllBlocks.BLACK_DESK_CABINET.get(), new Item.Properties())
		);
		
	/* PC STUFF */
	public static final RegistryObject<Item> WHITE_IBM_PC_AT_COMPUTER = ITEMS.register("white_ibm_pcat_computer",
			() -> new BlockItem(AllBlocks.WHITE_IBM_PC_AT_COMPUTER.get(), new Item.Properties())
		);

	public static final RegistryObject<Item> HARDWARE_LPC_MCCMD_BLOCK = ITEMS.register("hardware_lpc_mccmd",
			() -> new BlockItem(AllBlocks.HARDWARE_LPC_MCCMD_BLOCK.get(), new Item.Properties())
		);
}
