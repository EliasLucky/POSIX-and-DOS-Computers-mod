package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server to client after a {@link ServerboundFileWritePacket}
 * completes. Carries whether the write succeeded and, if not, the
 * error message rendered for the machine's OS family.
 *
 * @since 1.5
 */
public class ClientboundFileWriteResultPacket {
	public static final int MAX_PATH_LEN	= 512;
	public static final int MAX_MESSAGE_LEN = 512;

	private final BlockPos pos;
	private final String path;
	private final boolean success;
	private final String message;

	/**
	 * @param pos	  the computer block that handled the write
	 * @param path	  the path that was written
	 * @param success whether the write succeeded
	 * @param message empty on success, an OS-formatted error otherwise
	 */
	public ClientboundFileWriteResultPacket(BlockPos pos, String path, boolean success, String message) {
		this.pos = pos;
		this.path = path;
		this.success = success;
		this.message = message == null ? "" : message;
	}

	public ClientboundFileWriteResultPacket(FriendlyByteBuf buffer) {
		this.pos	 = buffer.readBlockPos();
		this.path	 = buffer.readUtf(MAX_PATH_LEN);
		this.success = buffer.readBoolean();
		this.message = buffer.readUtf(MAX_MESSAGE_LEN);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
		buffer.writeUtf(this.path, MAX_PATH_LEN);
		buffer.writeBoolean(this.success);
		buffer.writeUtf(this.message, MAX_MESSAGE_LEN);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				if (Minecraft.getInstance().screen instanceof ComputerTerminalScreen screen) {
					screen.onFileWriteResult(this.path, this.success, this.message);
				}
			});
		});
		ctx.setPacketHandled(true);
	}
}
