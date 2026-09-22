package com.eliaslucky.furniture.blocks;

import com.eliaslucky.furniture.AllCreativeModeTabs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DeskCabinetBlock extends DirectionalHorizontalBlock implements ICustomCreativeTab {
	public static final EnumProperty<DeskPart> PART = EnumProperty.create("part", DeskPart.class);
	
	public DeskCabinetBlock(Properties properties) {
		super(properties);
		
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.SOUTH)
				.setValue(PART, DeskPart.SINGLE)
		);
	}
	
	@Override
	public ResourceKey<CreativeModeTab> getCreativeTab() {
		return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING,PART);
	}
	
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState pNeighborState, LevelAccessor level, BlockPos currentPos, BlockPos pNeighborPos) {
		Direction facing = state.getValue(FACING);
		
		Direction relativeRight = facing.getCounterClockWise();
		Direction relativeLeft = facing.getClockWise();
	
		BlockState leftState = level.getBlockState(currentPos.relative(relativeLeft));
		BlockState rightState = level.getBlockState(currentPos.relative(relativeRight));
		
		boolean hasValidLeft = leftState.is(this) && leftState.getValue(FACING) == facing;
		boolean hasValidRight = rightState.is(this) && rightState.getValue(FACING) == facing;
	
		DeskPart newPart = DeskPart.SINGLE;
		
		if (hasValidRight && !hasValidLeft) {
			newPart = DeskPart.LEFT;
		}
		else if (!hasValidRight && hasValidLeft) {
			newPart = DeskPart.RIGHT;
		}
		else if (hasValidRight && hasValidLeft) {
			newPart = DeskPart.MIDDLE;
		}
		
		return state.setValue(PART, newPart);
	}
	
	public enum DeskPart implements StringRepresentable {
		SINGLE("single"),
		LEFT("left"),
		MIDDLE("middle"),
		RIGHT("right");
		
		private final String name;
		
		DeskPart(String name) {
			this.name = name;
		}
		
		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}