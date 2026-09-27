package io.github.illagercpr.creativecontainer.container;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges a {@link CreativeItemPool} to the generic NeoForge item handler capability so that hoppers, pipes and other
 * mods see the pool.
 *
 * <p>Extraction rules mirror the ME side:
 * <ul>
 *     <li>{@link #extractItem(int, int, boolean)} cannot name an item by definition, so it is refused with
 *     {@link ItemStack#EMPTY} — this mod never lets a caller pull "something" out of an infinite source.</li>
 *     <li>{@link #extractItemMatching} is the explicit, item-aware entry point used by our own code and tests.</li>
 * </ul>
 */
public final class CreativePoolItemHandler implements IItemHandler {

    private final CreativeItemPool pool;

    public CreativePoolItemHandler(CreativeItemPool pool) {
        this.pool = pool;
    }

    public CreativeItemPool pool() {
        return pool;
    }

    @Override
    public int getSlots() {
        return pool.slotCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return pool.getSlot(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return pool.reportedAmount();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // Insertion is only possible when explicitly enabled in the config; even then the pool stores templates,
        // not real stacks, so a caller must use addItem() through the GUI.
        return false;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        // No item specified -> refuse.
        return ItemStack.EMPTY;
    }

    /**
     * Item-aware extraction. Returns {@link ItemStack#EMPTY} when the pool does not hold the requested item.
     */
    public ItemStack extractItemMatching(ItemStack requested, int amount) {
        long extracted = pool.extract(requested, amount);
        return extracted <= 0 ? ItemStack.EMPTY : requested.copyWithCount((int) extracted);
    }

    /** @return the item in the given slot, or {@code null} when the slot is empty */
    @Nullable
    public ItemStack templateInSlot(int slot) {
        ItemStack stack = pool.getSlot(slot);
        return stack.isEmpty() ? null : stack;
    }
}
