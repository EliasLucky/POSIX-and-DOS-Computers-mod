package com.eliaslucky.mc_dos.blocks.peripheral.mccmd;

import com.eliaslucky.mc_dos.AllCreativeModeTabs;
import com.eliaslucky.mc_dos.AllBlockEntities;
import com.eliaslucky.mc_dos.blocks.ICustomCreativeTab;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MinecraftCommandTranslatorBlock extends Block
        implements EntityBlock, ICustomCreativeTab {

    public MinecraftCommandTranslatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ResourceKey<CreativeModeTab> getCreativeTab() {
        return AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinecraftCommandTranslatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof MinecraftCommandTranslatorBlockEntity mccmd) {
                MinecraftCommandTranslatorBlockEntity.tick(lvl, pos, st, mccmd);
            }
        };
    }
}
