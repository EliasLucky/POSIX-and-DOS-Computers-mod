package com.eliaslucky.mc_dos;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.eliaslucky.mc_dos.blocks.SquareTableBlock;
import com.eliaslucky.mc_dos.blocks.computer.ComputerType;
import com.eliaslucky.mc_dos.blocks.computer.IBMComputerBlock;
import com.eliaslucky.mc_dos.blocks.ChairBlock;
import com.eliaslucky.mc_dos.blocks.DeskCabinetBlock;

public class AllBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Computers.MODID);

	public static final RegistryObject<Block> TABLE = BLOCKS.register("table",
		() -> new SquareTableBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.0F,2.0F)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.ignitedByLava()
		)
	);

	public static final RegistryObject<Block> CHAIR = BLOCKS.register("chair",
		() -> new ChairBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.0F,2.0F)
			.sound(SoundType.WOOD)
			.noOcclusion() // isopaquecube(false) and isfullcube(false)
			.ignitedByLava()
		)
	);
	
	public static final RegistryObject<Block> DESK_CABINET = BLOCKS.register("desk_cabinet",
			() -> new DeskCabinetBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOD)
				.strength(2.0F,2.0F)
				.sound(SoundType.WOOD)
				.noOcclusion() // isopaquecube(false) and isfullcube(false)
				.ignitedByLava()
			)
		);

	/* PC STUFF */
	
	public static final RegistryObject<Block> WHITE_IBM_PC_AT_COMPUTER = BLOCKS.register("white_ibm_pcat_computer",
			() -> new IBMComputerBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(2.0F,2.0F)
				.sound(SoundType.STONE)
				.noOcclusion(), ComputerType.IBM_PC_AT
			)
		);
}
