package com.eliaslucky.mc_dos.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class MultipleBlock extends Block
{
	public static final BooleanProperty BACK = BooleanProperty.create("back");
	public static final BooleanProperty FORWARD = BooleanProperty.create("forward");
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	
	public MultipleBlock(Properties properties) {
		super(properties);

		this.registerDefaultState(this.stateDefinition.any()
			.setValue(BACK, false)
			.setValue(FORWARD, false)
			.setValue(LEFT, false)
			.setValue(RIGHT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BACK, FORWARD, LEFT, RIGHT);
	}
}
