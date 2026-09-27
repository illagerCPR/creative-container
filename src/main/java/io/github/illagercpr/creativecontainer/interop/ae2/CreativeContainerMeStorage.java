package io.github.illagercpr.creativecontainer.interop.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Exposes a {@link CreativeContainerBlockEntity} as an ME inventory, which is what an ME storage bus reads when it is
 * placed against the block.
 *
 * <p>Rules (from the modpack requirements):
 * <ul>
 *     <li>{@link #getAvailableStacks} reports every item the pool carries, {@code reportedAmount} each.</li>
 *     <li>{@link #extract} requires the caller to name an item; it hands out at most {@code reportedAmount} and never
 *     depletes the pool.</li>
 *     <li>{@link #insert} reports that nothing was accepted, so an infinite source can never swallow items.</li>
 * </ul>
 */
public final class CreativeContainerMeStorage implements MEStorage {

    private final CreativeContainerBlockEntity container;

    private CreativeContainerMeStorage(CreativeContainerBlockEntity container) {
        this.container = container;
    }

    /** Returns the (cached) ME view of the given container. */
    public static CreativeContainerMeStorage of(CreativeContainerBlockEntity container) {
        if (container.ae2Storage() instanceof CreativeContainerMeStorage existing) {
            return existing;
        }
        CreativeContainerMeStorage created = new CreativeContainerMeStorage(container);
        container.setAe2Storage(created);
        return created;
    }

    public CreativeContainerBlockEntity container() {
        return container;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("block.creativecontainer.creative_container");
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        long amount = container.reportedAmount();
        for (ItemStack stack : container.availableItems()) {
            AEItemKey key = AEItemKey.of(stack);
            if (key != null) {
                out.add(key, amount);
            }
        }
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        // Nothing is ever stored: an infinite source must not be able to swallow items.
        return 0L;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (amount <= 0 || !(what instanceof AEItemKey itemKey)) {
            // No item named -> refuse, which is the "never guess what the caller wants" rule of this mod.
            return 0L;
        }
        return container.pool().extract(itemKey.getReadOnlyStack(), amount);
    }
}
