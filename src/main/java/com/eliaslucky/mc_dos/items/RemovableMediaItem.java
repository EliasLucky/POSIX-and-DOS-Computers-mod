package com.eliaslucky.mc_dos.items;

import com.eliaslucky.mc_dos.api.vfs.DiskMedia;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A removable storage item — floppy diskette, CD, DVD, Blu-ray disc.
 *
 * <p>The item carries a full {@link VirtualFileSystem.Node} subtree in
 * its NBT under the {@code "vfs"} key. Inserting the item copies that
 * tree into the machine's mount table; ejecting serializes it back.
 *
 * <p>Read-only media (CD-ROM, DVD-ROM) still carry a tree; they just
 * cannot be written to after insertion. Attempts to create or modify
 * files on a read-only mount fail with a permission error, matching
 * real optical media.
 *
 * <p>Every media type shares this one item class. The distinction is
 * the {@link #media()} field, set at construction and used by
 * compatibility checks.
 *
 * @since 1.5
 */
public class RemovableMediaItem extends Item {
    /** NBT key holding the serialized content tree. */
    public static final String TAG_VFS   = "vfs";
    /** NBT key holding the user-assigned label. */
    public static final String TAG_LABEL = "Label";

    private final DiskMedia media;

    /**
     * @param media the physical media type
     * @param props the item properties (max stack size 1, etc.)
     */
    public RemovableMediaItem(DiskMedia media, Properties props) {
        super(props);
        this.media = media;
    }

    /** @return the physical media type this item represents. */
    public DiskMedia media() { return media; }

    // Content tree access

    /**
     * Read the content tree from the stack's NBT. If the stack has never
     * been formatted, returns a fresh empty tree — that represents a
     * blank disk. Read-only media are not pre-formatted with content
     * because that is a write operation.
     *
     * @param stack the item stack
     * @return the root node; never {@code null}
     */
    public VirtualFileSystem.Node readRoot(ItemStack stack) {
        CompoundTag itemTag = stack.getTag();
        if (itemTag != null && itemTag.contains(TAG_VFS)) {
            return VirtualFileSystem.Node.load(itemTag.getCompound(TAG_VFS), null);
        }
        // Blank media: create an empty root.
        VirtualFileSystem.Node blank = new VirtualFileSystem.Node("/", true);
        return blank;
    }

    /**
     * Write the content tree to the stack's NBT.
     *
     * @param stack the item stack
     * @param root  the tree to serialize
     */
    public void writeRoot(ItemStack stack, VirtualFileSystem.Node root) {
        if (!media.writable()) return;    // read-only media refuse writes
        CompoundTag itemTag = stack.getOrCreateTag();
        itemTag.put(TAG_VFS, root.save());
    }

    /** @return the disk's label, or a default derived from the media type. */
    public String label(ItemStack stack) {
        CompoundTag itemTag = stack.getTag();
        if (itemTag != null && itemTag.contains(TAG_LABEL)) {
            return itemTag.getString(TAG_LABEL);
        }
        return media.displayName();
    }

    /** Set the disk's label. Ignored on read-only media. */
    public void setLabel(ItemStack stack, String label) {
        if (!media.writable()) return;
        CompoundTag itemTag = stack.getOrCreateTag();
        itemTag.putString(TAG_LABEL, label);
    }

    /** @return {@code true} if the disk accepts writes. */
    public boolean writable() { return media.writable(); }

    // Presentation

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(label(stack)));
        tooltip.add(Component.literal(media.displayName()));
        if (!media.writable()) {
            tooltip.add(Component.literal("Read-only"));
        }
        VirtualFileSystem.Node root = readRoot(stack);
        tooltip.add(Component.literal("Files: " + countFiles(root)));
    }

    private static int countFiles(VirtualFileSystem.Node node) {
        int count = 0;
        for (VirtualFileSystem.Node child : node.children.values()) {
            if (child.isDirectory) count += countFiles(child);
            else count++;
        }
        return count;
    }
}
