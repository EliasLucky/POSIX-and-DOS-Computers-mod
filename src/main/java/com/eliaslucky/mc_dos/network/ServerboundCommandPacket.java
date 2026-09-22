package com.eliaslucky.furniture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import com.eliaslucky.furniture.blocks.computer.ComputerBlockEntity;

public class ServerboundCommandPacket {
	private final BlockPos pos;
	private final String command;

	public ServerboundCommandPacket(BlockPos pos, String command) {
		this.pos = pos;
		this.command = command;
	}

	public ServerboundCommandPacket(FriendlyByteBuf buffer) {
		this.pos = buffer.readBlockPos();
		this.command = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
		buffer.writeUtf(this.command);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;

			ServerLevel level = player.serverLevel();
			BlockEntity be = level.getBlockEntity(this.pos);

			if (be instanceof ComputerBlockEntity computer) {
				String output = computer.processCommand(this.command);
				String currentPath = computer.getFileSystem().getCurrentPath();
				
				ModMessages.sendToPlayer(new ClientboundTerminalOutputPacket(output, currentPath), player);
			}
		});
		ctx.setPacketHandled(true);
	}
}
