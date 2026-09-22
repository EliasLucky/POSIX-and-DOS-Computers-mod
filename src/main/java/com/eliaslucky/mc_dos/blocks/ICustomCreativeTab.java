package com.eliaslucky.mc_dos.blocks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public interface ICustomCreativeTab
{
	ResourceKey<CreativeModeTab> getCreativeTab();
}
