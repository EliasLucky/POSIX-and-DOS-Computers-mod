package com.eliaslucky.mc_dos;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class AllBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Computers.MODID);

	public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_PROGRAMMABLE_BLOCK = BLOCK_ENTITIES.register("computer_programmable_block",
		() -> BlockEntityType.Builder.of(ComputerBlockEntity::new, AllBlocks.WHITE_IBM_PC_AT_COMPUTER.get()).build(null)
	);
}
