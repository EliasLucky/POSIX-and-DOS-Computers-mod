package com.eliaslucky.mc_dos.network;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public class ServerboundFileWritePacket {
    /** Upper bound on both path and content. Keeps a malicious client from OOM-ing the server. */
    public static final int MAX_PATH_LEN    = 512;
    public static final int MAX_CONTENT_LEN = 1 << 20; // 1 MiB

    private final BlockPos pos;
    private final String path;
    private final String content;

    public ServerboundFileWritePacket(BlockPos pos, String path, String content) {
        this.pos     = pos;
        this.path    = path;
        this.content = content;
    }

    public ServerboundFileWritePacket(FriendlyByteBuf buffer) {
        this.pos     = buffer.readBlockPos();
        this.path    = buffer.readUtf(MAX_PATH_LEN);
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

            VirtualFileSystem vfs = computer.getFileSystem();
            VirtualFileSystem.Node node = vfs.resolvePath(this.path);

            if (node == null) {
                // Create the file in the current directory (mirrors how QBASIC "New" works).
                String cleanPath = this.path.replace('/', '\\');
                int lastSlash = cleanPath.lastIndexOf('\\');
                String name = (lastSlash == -1) ? cleanPath : cleanPath.substring(lastSlash + 1);
                if (name.isEmpty()) return;

                VirtualFileSystem.Node parent = vfs.getCurrentDir();
                if (lastSlash != -1) {
                    String parentPath = cleanPath.substring(0, lastSlash);
                    if (!parentPath.isEmpty()) {
                        parent = vfs.resolvePath(parentPath);
                    }
                }
                if (parent == null || !parent.isDirectory) return;

                node = new VirtualFileSystem.Node(name.toUpperCase(Locale.ROOT), false);
                parent.addChild(node);
            }

            if (node.isDirectory) return;

            node.content = this.content;
            computer.setChanged();
        });
        ctx.setPacketHandled(true);
    }
}
