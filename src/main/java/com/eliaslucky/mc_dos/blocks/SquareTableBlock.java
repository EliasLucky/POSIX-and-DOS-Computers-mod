package com.eliaslucky.mc_dos.blocks;

import com.eliaslucky.mc_dos.AllCreativeModeTabs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class SquareTableBlock extends MultipleBlock implements ICustomCreativeTab {
	public SquareTableBlock(Properties properties) {
		super(properties);
	}
	
	@Override
	public ResourceKey<CreativeModeTab> getCreativeTab() {
		return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
		boolean back = level.getBlockState(currentPos.south()).is(this);
		boolean forward = level.getBlockState(currentPos.north()).is(this);
		boolean left = level.getBlockState(currentPos.west()).is(this);
		boolean right = level.getBlockState(currentPos.east()).is(this);

		return state.setValue(BACK, back)
			    .setValue(FORWARD, forward)
			    .setValue(LEFT, left)
			    .setValue(RIGHT, right);
	}
}
