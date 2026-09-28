package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileOpResult;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class ServerboundFileWritePacket {
	/** Upper bound on both path and content. Keeps a malicious client from OOM-ing the server. */
	public static final int MAX_PATH_LEN	= 512;
	public static final int MAX_CONTENT_LEN = 1 << 20; // 1 MiB

	private final BlockPos pos;
	private final String path;
	private final String content;

	public ServerboundFileWritePacket(BlockPos pos, String path, String content) {
		this.pos	 = pos;
		this.path	 = path;
		this.content = content;
	}

	public ServerboundFileWritePacket(FriendlyByteBuf buffer) {
		this.pos	 = buffer.readBlockPos();
		this.path	 = buffer.readUtf(MAX_PATH_LEN);
		this.content = buffer.readUtf(MAX_CONTENT_LEN);
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.pos);
		buffer.writeUtf(this.path, MAX_PATH_LEN);
		buffer.writeUtf(this.content, MAX_CONTENT_LEN);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context ctx = contextSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;

			if (!(player.serverLevel().getBlockEntity(this.pos) instanceof ComputerBlockEntity computer)) {
				return;
			}

			// Only the player currently occupying the terminal may write.
			if (!player.getUUID().equals(computer.getActiveUser())) {
				return;
			}

			FileOpResult result = computer.getFileSystem().writeFile(path, content);
			if (result.success()) computer.setChanged();

			String osFamily = computer.getMachineType().commandProcessor().osFamily();
			String message = result.success() ? "" : result.messageFor(osFamily);

			ModMessages.sendToPlayer(
					new ClientboundFileWriteResultPacket(pos, path, result.success(), message),
					player);
		});
		ctx.setPacketHandled(true);
	}
}
