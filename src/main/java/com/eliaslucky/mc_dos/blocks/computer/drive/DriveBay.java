package com.eliaslucky.mc_dos.blocks.computer.drive;

import com.eliaslucky.mc_dos.api.vfs.DiskMedia;
import com.eliaslucky.mc_dos.api.vfs.DriveType;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.items.RemovableMediaItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A physical drive bay on a computer block. Holds one media item, or
 * nothing. When media is inserted, its content tree is available to
 * the VFS under a mount identifier chosen by the operating system.
 *
 * <p>Persisted to NBT: the bay's {@code DriveType} is fixed by the
 * computer type and recreated on load; the inserted item stack is
 * saved whole, which preserves the disk's own NBT content for free.
 *
 * @since 1.5
 */
public class DriveBay {
    private final int index;
    private final DriveType type;

    /** The DOS drive letter this bay maps to, e.g. "A", "B", "D". */
    private final String dosLetter;

    /** The POSIX device path, e.g. "/dev/fd0", "/dev/sr0". */
    private final String posixDevice;

    /** The POSIX default mount point, e.g. "/mnt/floppy", "/mnt/cdrom". */
    private final String posixMountPoint;

    /** The inserted media item, or {@link ItemStack#EMPTY}. */
    private ItemStack inserted = ItemStack.EMPTY;

    /** The root node of the inserted disk, or null when empty. */
    private VirtualFileSystem.Node mountedRoot = null;

    /** POSIX only: whether the user has run mount on this bay. */
    private boolean posixMounted = false;

    public DriveBay(int index, DriveType type,
                    String dosLetter, String posixDevice, String posixMountPoint) {
        this.index = index;
        this.type = type;
        this.dosLetter = dosLetter;
        this.posixDevice = posixDevice;
        this.posixMountPoint = posixMountPoint;
    }

    // Accessors
    /** @return the bay's index on the machine, 0-based. */
    public int index() { return index; }

    /** @return the drive's type. */
    public DriveType type() { return type; }

    /** @return the DOS drive letter, or {@code null} for bays with no letter. */
    public String dosLetter() { return dosLetter; }

    /** @return the POSIX device path, or {@code null} for bays without one. */
    public String posixDevice() { return posixDevice; }

    /** @return the POSIX default mount point, or {@code null}. */
    public String posixMountPoint() { return posixMountPoint; }

    /** @return {@code true} if media is currently inserted. */
    public boolean hasMedia() { return !inserted.isEmpty() && mountedRoot != null; }

    /** @return the inserted stack, or {@link ItemStack#EMPTY}. */
    public ItemStack insertedStack() { return inserted; }

    /** @return the media type of the inserted disk, or {@code null}. */
    public DiskMedia mediaType() {
        if (inserted.isEmpty()) return null;
        if (inserted.getItem() instanceof RemovableMediaItem rmi) {
            return rmi.media();
        }
        return null;
    }

    /** @return the root node of the mounted disk, or {@code null} when empty. */
    public VirtualFileSystem.Node mountedRoot() { return mountedRoot; }

    /** @return whether the drive has been mounted by the OS. */
    public boolean isPosixMounted() { return posixMounted; }

    /** Set the POSIX-mounted flag. Called by the mount/umount commands. */
    public void setPosixMounted(boolean mounted) { this.posixMounted = mounted; }

    // Insert / eject

    /**
     * Try to insert media into this bay.
     *
     * @param stack  the item stack being inserted
     * @param root   the disk's content tree, deserialized from the stack
     * @return {@code true} if insertion succeeded
     */
    public boolean insert(ItemStack stack, VirtualFileSystem.Node root) {
        if (hasMedia()) return false;
        if (!type.canRead(mediaFromStack(stack))) return false;
        this.inserted = stack.copy();
        this.inserted.setCount(1);
        this.mountedRoot = root;
        return true;
    }

    /**
     * Eject the current media.
     *
     * @return the ejected stack, or {@link ItemStack#EMPTY} if nothing was inserted
     */
    public ItemStack eject() {
        if (inserted.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = inserted.copy();
        inserted = ItemStack.EMPTY;
        mountedRoot = null;
        posixMounted = false;
        return out;
    }

    
    public void save(CompoundTag tag) {
        if (!inserted.isEmpty()) {
            CompoundTag stackTag = new CompoundTag();
            inserted.save(stackTag);
            tag.put("Inserted", stackTag);
        }
        tag.putBoolean("PosixMounted", posixMounted);
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Inserted")) {
            inserted = ItemStack.of(tag.getCompound("Inserted"));
            if (!inserted.isEmpty() &&
                    inserted.getItem() instanceof RemovableMediaItem rmi) {
                mountedRoot = rmi.readRoot(inserted);
            }
        }
        posixMounted = tag.getBoolean("PosixMounted");
    }

    private static DiskMedia mediaFromStack(ItemStack stack) {
        if (stack.getItem() instanceof RemovableMediaItem rmi) {
            return rmi.media();
        }
        return null;
    }
}
