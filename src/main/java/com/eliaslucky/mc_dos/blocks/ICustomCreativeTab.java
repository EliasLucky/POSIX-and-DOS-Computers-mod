package com.eliaslucky.furniture.blocks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public interface ICustomCreativeTab
{
	ResourceKey<CreativeModeTab> getCreativeTab();
}
