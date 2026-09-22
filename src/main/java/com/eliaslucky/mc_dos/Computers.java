package com.eliaslucky.furniture;

import com.eliaslucky.furniture.blocks.ICustomCreativeTab;
import com.eliaslucky.furniture.network.ModMessages;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Computers.MODID)
public class Computers
{
	public static final String MODID = "mc_dos";
	public static final String NAME = "MC-DOS Compputers";

	public static final Logger LOGGER = LogUtils.getLogger();

	public Computers(FMLJavaModLoadingContext context)
	{
		IEventBus modEventBus = context.getModEventBus();

		//REGISTRATE.registerEventListeners(modEventBus);

        
		AllCreativeModeTabs.register(modEventBus);
		AllBlocks.BLOCKS.register(modEventBus);
		AllBlockEntities.BLOCK_ENTITIES.register(modEventBus);
		AllItems.ITEMS.register(modEventBus);
		// AllRecipeTypes.register(modEventBus)
       
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::buildContents);
        
		MinecraftForge.EVENT_BUS.register(this);     
	}

	public static void init(final FMLCommonSetupEvent event) {
	
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(ModMessages::register);
	}

	public void buildContents(BuildCreativeModeTabContentsEvent event) {
		AllBlocks.BLOCKS.getEntries().forEach(registryObject -> {
			Block block = registryObject.get();

			if (block instanceof ICustomCreativeTab customTabBlock) {
				if (event.getTabKey() == customTabBlock.getCreativeTab()) {
					Item blockItem = block.asItem();

					if (blockItem != Items.AIR) {
						event.accept(blockItem);	
					}
				}
			}
		});
	}
}
