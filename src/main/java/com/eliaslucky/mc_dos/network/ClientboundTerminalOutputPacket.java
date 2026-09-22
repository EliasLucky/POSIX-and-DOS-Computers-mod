package com.eliaslucky.furniture.network;

import com.eliaslucky.furniture.client.ComputerTerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundTerminalOutputPacket {
	private final String output;
	private final String currentPath;

	public ClientboundTerminalOutputPacket(String output, String currentPath) {
		this.output = output;
		this.currentPath = currentPath;
	}

	public ClientboundTerminalOutputPacket(FriendlyByteBuf buffer) {
		this.output = buffer.readUtf();
		this.currentPath = buffer.readUtf();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUtf(this.output);
		buffer.writeUtf(this.currentPath);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				if (Minecraft.getInstance().screen instanceof ComputerTerminalScreen screen) {
					screen.appendOutput(this.output, this.currentPath);
				}
			});
		});
		ctx.setPacketHandled(true);
	}
}
