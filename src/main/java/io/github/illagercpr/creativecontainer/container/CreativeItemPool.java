package io.github.illagercpr.creativecontainer.container;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * The virtual item pool behind a creative container.
 *
 * <p>Semantics (agreed with the modpack requirements):
 * <ul>
 *     <li>The pool has a fixed number of <em>slots</em>. A slot either holds nothing or holds one item type.</li>
 *     <li>A filled slot never runs out: it reports a configurable amount and can hand that amount out again and again.</li>
 *     <li>Extraction always requires the caller to name the item it wants (see {@link #extract}).</li>
 * </ul>
 *
 * <p>This class is deliberately free of any mod-specific (AE2/ProjectE) type so it can be unit tested and exercised by
 * GameTests without those mods installed.
 */
public final class CreativeItemPool {

    /** Upper bound of the configurable reported amount, i.e. {@link Integer#MAX_VALUE}. */
    public static final int MAX_AMOUNT = Integer.MAX_VALUE;
    public static final int MIN_AMOUNT = 1;
    public static final int SLOT_COUNT = 54;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int reportedAmount = MAX_AMOUNT;

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
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack getSlot(int index) {
        return index >= 0 && index < SLOT_COUNT ? slots.get(index) : ItemStack.EMPTY;
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (ItemStack.isSameItemSameComponents(slots.get(i), template)) {
                return false;
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, template);
                return true;
            }
        }
        return false;
    }

    /** @return true when the pool changed */
    public boolean removeSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT || slots.get(index).isEmpty()) {
            return false;
        }
        slots.set(index, ItemStack.EMPTY);
        compact();
        return true;
    }

    /** Moves every filled slot to the front so the GUI never shows holes. */
    public void compact() {
        List<ItemStack> filled = new ArrayList<>(SLOT_COUNT);
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                filled.add(stack);
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.set(i, i < filled.size() ? filled.get(i) : ItemStack.EMPTY);
        }
    }

    /** Every item the pool can hand out, each as a single-item template stack. */
    public List<ItemStack> availableItems() {
        List<ItemStack> result = new ArrayList<>(SLOT_COUNT);
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                result.add(stack.copyWithCount(1));
            }
        }
        return result;
    }

    public List<ItemStack> availableItemsMatching(Predicate<ItemStack> filter) {
        List<ItemStack> result = new ArrayList<>(SLOT_COUNT);
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && filter.test(stack)) {
                result.add(stack.copyWithCount(1));
            }
        }
        return result;
    }

    /** Whether the pool can supply the given item right now. */
    public boolean contains(ItemStack requested) {
        if (requested.isEmpty()) {
            return false;
        }
        for (ItemStack stack : slots) {
            if (ItemStack.isSameItemSameComponents(stack, requested)) {
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
}
