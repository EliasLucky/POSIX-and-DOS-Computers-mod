package com.eliaslucky.mc_dos.blocks.computer;

import com.eliaslucky.mc_dos.AllCreativeModeTabs;
import com.eliaslucky.mc_dos.blocks.DirectionalHorizontalBlock;
import com.eliaslucky.mc_dos.blocks.ICustomCreativeTab;
import com.eliaslucky.mc_dos.blocks.computer.drive.DriveBay;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.items.RemovableMediaItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class IBMComputerBlock extends DirectionalHorizontalBlock implements EntityBlock, ICustomCreativeTab {
	private final MachineType machineType;

	public IBMComputerBlock(Properties properties, MachineType MachineType) {
		super(properties);
		this.machineType = MachineType;
	}
	
	@Override
	public ResourceKey<CreativeModeTab> getCreativeTab() {
		return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		ComputerBlockEntity be = new ComputerBlockEntity(pos, state);
		be.setMachineType(this.machineType);
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
			ItemStack held = player.getItemInHand(hand);
			boolean sneaking = player.isShiftKeyDown();

			// Insert
			if (sneaking && !held.isEmpty() && held.getItem() instanceof RemovableMediaItem) {
				if (!level.isClientSide()) {
					if (computerBE.tryInsertMedia(held, player)) {
						player.displayClientMessage(
								Component.literal("Disk inserted."), true);
					} else {
						player.displayClientMessage(
								Component.literal("No compatible empty bay."), true);
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide());
			}

			// Eject
			if (sneaking && held.isEmpty() && computerBE.hasInsertedMedia()) {
				if (!level.isClientSide()) {
					// Eject from the first bay that has media.
					for (DriveBay bay : computerBE.driveBays()) {
						if (bay.hasMedia()) {
							computerBE.tryEjectMedia(bay.index(), player);
							player.displayClientMessage(
									Component.literal("Disk ejected."), true);
							break;
						}
					}
				}
				return InteractionResult.sidedSuccess(level.isClientSide());
			}
			// Terminal

			if (!computerBE.tryOccupy(player)) {
				if (!level.isClientSide()) {
					player.displayClientMessage(Component.literal("Computer is currently in use!"), true);
				}
				return InteractionResult.FAIL;
			}
			
			if (!level.isClientSide()) {
				
			}

			if (level.isClientSide()) {
				net.minecraft.client.Minecraft.getInstance().setScreen(new ComputerTerminalScreen(pos, this.machineType));
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}	
}
