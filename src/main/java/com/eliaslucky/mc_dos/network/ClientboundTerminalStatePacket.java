package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from server to client when a terminal screen opens or the boot
 * state changes. Tells the client whether the machine is in POST, in
 * SETUP, or running, and carries the POST lines if applicable.
 */
public class ClientboundTerminalStatePacket {
	private final BlockPos pos;
	private final boolean postPhase;
	private final List<String> postLines;
	private final int countdownSeconds;
	private final String currentPath;

	public ClientboundTerminalStatePacket(BlockPos pos, boolean postPhase, List<String> postLines, int countdownSeconds, String currentPath) {
		this.pos = pos;
		this.postPhase = postPhase;
		this.postLines = postLines == null ? List.of() : List.copyOf(postLines);
		this.countdownSeconds = countdownSeconds;
		this.currentPath = currentPath == null ? "" : currentPath;
	}

	public ClientboundTerminalStatePacket(FriendlyByteBuf buffer) {
		this.pos = buffer.readBlockPos();
		this.postPhase = buffer.readBoolean();
		int n = buffer.readVarInt();
		List<String> lines = new ArrayList<>(n);
		for (int i = 0; i < n; i++) lines.add(buffer.readUtf());
		this.postLines = lines;
		this.countdownSeconds = buffer.readVarInt();
		this.currentPath = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeBoolean(postPhase);
		buffer.writeVarInt(postLines.size());
		for (String line : postLines) buffer.writeUtf(line);
		buffer.writeVarInt(countdownSeconds);
		buffer.writeUtf(currentPath);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Minecraft.getInstance().screen instanceof ComputerTerminalScreen screen) {
				screen.onTerminalState(postPhase, postLines, countdownSeconds);
			}
		}));
		ctx.setPacketHandled(true);
	}
}
