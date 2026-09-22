package com.eliaslucky.mc_dos.blocks.computer;

import com.eliaslucky.mc_dos.AllCreativeModeTabs;
import com.eliaslucky.mc_dos.blocks.DirectionalHorizontalBlock;
import com.eliaslucky.mc_dos.blocks.ICustomCreativeTab;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class IBMComputerBlock extends DirectionalHorizontalBlock implements EntityBlock, ICustomCreativeTab {
	private final ComputerType computerType;

	public IBMComputerBlock(Properties properties, ComputerType computerType) {
		super(properties);
		this.computerType = computerType;
	}
	
	@Override
	public ResourceKey<CreativeModeTab> getCreativeTab() {
		return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		ComputerBlockEntity be = new ComputerBlockEntity(pos, state);
		be.setComputerType(this.computerType);
		return be;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return (lvl, pos, st, be) -> {
			if (be instanceof ComputerBlockEntity computer) {
				ComputerBlockEntity.tick(lvl, pos, st, computer);
			}
		};
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof ComputerBlockEntity computerBE) {
			computerBE.setComputerType(this.computerType);

			if (!computerBE.tryOccupy(player)) {
				if (!level.isClientSide()) {
					player.displayClientMessage(Component.literal("Computer is currently in use!"), true);
				}
				return InteractionResult.FAIL;
			}

			if (level.isClientSide()) {
				net.minecraft.client.Minecraft.getInstance().setScreen(new ComputerTerminalScreen(pos, this.computerType));
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}	
}
