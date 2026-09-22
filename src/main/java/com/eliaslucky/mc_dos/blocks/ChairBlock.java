package com.eliaslucky.furniture.blocks;

import com.eliaslucky.furniture.AllCreativeModeTabs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChairBlock extends DirectionalHorizontalBlock implements ICustomCreativeTab {
	protected static final VoxelShape SHAPE = Block.box(1.6D,0.0D,1.6D,14.4D,9.6D,14.4D);

	public ChairBlock(Properties properties) {
		super(properties);
	}
	
	@Override
	public ResourceKey<CreativeModeTab> getCreativeTab() {
		return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
