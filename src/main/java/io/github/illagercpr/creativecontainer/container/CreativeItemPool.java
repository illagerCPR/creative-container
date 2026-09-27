package io.github.illagercpr.creativecontainer.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The virtual item pool behind a creative container.
 *
 * <p>Semantics (agreed with the modpack requirements):
 * <ul>
 *     <li>The pool has a configurable number of <em>slots</em>. A slot either holds nothing or holds one item type.</li>
 *     <li>A filled slot never runs out: it reports a configurable amount and can hand that amount out again and again.</li>
 *     <li>Extraction always requires the caller to name the item it wants (see {@link #extract}).</li>
 * </ul>
 *
 * <p>Implementation notes: filled slots are always kept contiguous ({@code [0, filledSlots)}), and an
 * {@code item -> slot indices} hash index accelerates {@link #contains} and {@link #addItem} to (almost) constant time
 * for the large configured pools. The index only narrows by item — the final equality check stays
 * {@link ItemStack#isSameItemSameComponents}, so component-variant stacks (e.g. two differently enchanted swords)
 * keep working as distinct pool entries no matter how the vanilla stack comparison treats their component maps.
 *
 * <p>This class is deliberately free of any mod-specific (AE2/ProjectE) type so it can be unit tested and exercised by
 * GameTests without those mods installed.
 */
public final class CreativeItemPool {

    /** Upper bound of the configurable reported amount, i.e. {@link Integer#MAX_VALUE}. */
    public static final int MAX_AMOUNT = Integer.MAX_VALUE;
    public static final int MIN_AMOUNT = 1;

    /** Configured pool size bounds: matches {@code CCConfig#POOL_SLOTS}. */
    public static final int MIN_SLOT_COUNT = 9;
    public static final int MAX_SLOT_COUNT = 4320;
    public static final int DEFAULT_SLOT_COUNT = 108;

    private NonNullList<ItemStack> slots;
    private int slotCount;

    /** Number of filled slots; filled slots are always the contiguous prefix {@code [0, filled)}. */
    private int filled;

    /** item -> slot indices currently holding that item; keeps {@link #contains} off the linear scan. */
    private final Map<net.minecraft.world.item.Item, IntList> byItem = new HashMap<>();

    private int reportedAmount = MAX_AMOUNT;

    public CreativeItemPool() {
        this(DEFAULT_SLOT_COUNT);
    }

    public CreativeItemPool(int requestedSlotCount) {
        slotCount = clampSlotCount(requestedSlotCount);
        slots = NonNullList.withSize(slotCount, ItemStack.EMPTY);
    }

    private static int clampSlotCount(int requested) {
        return Math.max(MIN_SLOT_COUNT, Math.min(MAX_SLOT_COUNT, requested));
    }

    public int slotCount() {
        return slotCount;
    }

    /** Number of filled slots; filled slots are the contiguous prefix {@code [0, filledSlots)}. */
    public int filledSlots() {
        return filled;
    }

    /**
     * Re-sizes the pool while keeping its contents. Used when loading a container that was saved with a different
     * (usually larger) configuration: the effective size never shrinks below the stored contents.
     */
    public void resize(int newSize) {
        int target = Math.max(clampSlotCount(newSize), filled);
        if (target == slotCount) {
            return;
        }
        NonNullList<ItemStack> resized = NonNullList.withSize(target, ItemStack.EMPTY);
        for (int i = 0; i < filled; i++) {
            resized.set(i, slots.get(i));
        }
        slots = resized;
        slotCount = target;
    }

    public NonNullList<ItemStack> slots() {
        return slots;
    }

    public int reportedAmount() {
        return reportedAmount;
    }

    /** Clamps to {@code [1, Integer.MAX_VALUE]}; returns whether the value actually changed. */
    public boolean setReportedAmount(int amount) {
        int clamped = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, amount));
        if (clamped == reportedAmount) {
            return false;
        }
        reportedAmount = clamped;
        return true;
    }

    public boolean isEmpty() {
        return filled == 0;
    }

    public ItemStack getSlot(int index) {
        return index >= 0 && index < slotCount ? slots.get(index) : ItemStack.EMPTY;
    }

    /**
     * Puts an item into the pool, reusing the slot the item already occupies when possible.
     *
     * @return true when the pool changed
     */
    public boolean addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack template = stack.copyWithCount(1);
        IntList candidates = byItem.get(template.getItem());
        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (ItemStack.isSameItemSameComponents(slots.get(candidates.getInt(i)), template)) {
                    return false;
                }
            }
        }
        if (filled >= slotCount) {
            return false;
        }
        slots.set(filled, template);
        byItem.computeIfAbsent(template.getItem(), item -> new IntArrayList()).add(filled);
        filled++;
        return true;
    }

    /** @return true when the pool changed */
    public boolean removeSlot(int index) {
        if (index < 0 || index >= slotCount || slots.get(index).isEmpty()) {
            return false;
        }
        slots.set(index, ItemStack.EMPTY);
        compact();
        return true;
    }

    /** Moves every filled slot to the front so the GUI never shows holes. */
    public void compact() {
        List<ItemStack> items = new ArrayList<>(filled);
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        for (int i = 0; i < slotCount; i++) {
            slots.set(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        filled = items.size();
        rebuildIndex();
    }

    /** Empties the pool (used by the client mirror when a full resync arrives). */
    public void clear() {
        for (int i = 0; i < slotCount; i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        filled = 0;
        byItem.clear();
    }

    /**
     * Direct slot write used by the client mirror's incremental sync. The payload protocol only writes either a
     * replacement for an already-filled slot or the next slot of the contiguous prefix (after {@link #clear()}), so
     * the "filled slots form a prefix" invariant is never broken through this path.
     */
    public void setSlot(int index, ItemStack stack) {
        if (index < 0 || index >= slotCount) {
            return;
        }
        ItemStack replacement = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ItemStack previous = slots.get(index);
        if (!previous.isEmpty()) {
            removeFromIndex(previous.getItem(), index);
            filled--;
        }
        slots.set(index, replacement);
        if (!replacement.isEmpty()) {
            byItem.computeIfAbsent(replacement.getItem(), item -> new IntArrayList()).add(index);
            filled = Math.max(filled, index + 1);
        }
    }

    /** Every item the pool can hand out, each as a single-item template stack. */
    public List<ItemStack> availableItems() {
        List<ItemStack> result = new ArrayList<>(filled);
        for (int i = 0; i < filled; i++) {
            result.add(slots.get(i).copyWithCount(1));
        }
        return result;
    }

    public List<ItemStack> availableItemsMatching(Predicate<ItemStack> filter) {
        List<ItemStack> result = new ArrayList<>(filled);
        for (int i = 0; i < filled; i++) {
            if (filter.test(slots.get(i))) {
                result.add(slots.get(i).copyWithCount(1));
            }
        }
        return result;
    }

    /** Whether the pool can supply the given item right now. */
    public boolean contains(ItemStack requested) {
        if (requested.isEmpty()) {
            return false;
        }
        IntList candidates = byItem.get(requested.getItem());
        if (candidates == null) {
            return false;
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (ItemStack.isSameItemSameComponents(slots.get(candidates.getInt(i)), requested)) {
                return true;
            }
        }
        return false;
    }

    /** How many of {@code requested} the pool can report. Zero when it does not have the item. */
    public long reportableAmount(ItemStack requested) {
        return contains(requested) ? reportedAmount : 0L;
    }

    /**
     * Extracts up to {@code amount} of the given item.
     *
     * <p>An empty request is refused with a zero result: the pool never guesses what the caller wants, which is the
     * "refuse to extract when no item is specified" rule of this mod.
     */
    public long extract(ItemStack requested, long amount) {
        if (amount <= 0 || !contains(requested)) {
            return 0L;
        }
        return Math.min(amount, reportedAmount);
    }

    private void rebuildIndex() {
        byItem.clear();
        for (int i = 0; i < filled; i++) {
            byItem.computeIfAbsent(slots.get(i).getItem(), item -> new IntArrayList()).add(i);
        }
    }

    private void removeFromIndex(net.minecraft.world.item.Item item, int index) {
        IntList candidates = byItem.get(item);
        if (candidates == null) {
            return;
        }
        candidates.rem(index); // remove by value: the index stores slot positions
        if (candidates.isEmpty()) {
            byItem.remove(item);
        }
    }
}
