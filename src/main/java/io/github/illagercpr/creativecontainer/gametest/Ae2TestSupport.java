package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.placeContainer;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.interop.ae2.CreativeContainerMeStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AE2 interoperability, verified through the very lookup AE2 performs.
 *
 * <p>This class references {@code appeng.api} types, so it is only ever loaded when AE2 is present: the GameTest class
 * calls into it after checking {@code InteropHooks.ae2Available()}.
 */
public final class Ae2TestSupport {

    private Ae2TestSupport() {
    }

    /**
     * A storage bus reads the adjacent block's {@code AECapabilities.ME_STORAGE} capability. Verified against AE2
     * 19.2.17 bytecode: {@code StorageBusPart} builds its adjacency lookup as
     * {@code new PartAdjacentApi<>(this, AECapabilities.ME_STORAGE)}, so a bus placed against the block sees this
     * inventory — with no grid node, no channel and no power required from us.
     */
    static void storageBusCapabilityIsExposed(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));

        for (Direction side : Direction.values()) {
            MEStorage storage = helper.getLevel().getCapability(AECapabilities.ME_STORAGE, absolute, side);
            check(storage != null, "every side must answer the storage bus lookup: " + side);
            check(storage instanceof CreativeContainerMeStorage, "and it must be our container view");
        }
        check(helper.getLevel().getCapability(AECapabilities.ME_STORAGE, absolute, Direction.UP)
                == CreativeContainerMeStorage.of(container), "the view is cached per block entity");
        helper.succeed();
    }

    /** What an ME network sees once a storage bus is attached: the pool, at the configured amount. */
    static void meViewReportsThePool(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 3, 1, 3);
        container.addItem(new ItemStack(Items.DIAMOND));
        container.setReportedAmount(1234);

        MEStorage storage = helper.getLevel().getCapability(
                AECapabilities.ME_STORAGE, helper.absolutePos(new BlockPos(3, 1, 3)), Direction.UP);
        check(storage != null, "the container must expose an ME view");

        KeyCounter counter = new KeyCounter();
        storage.getAvailableStacks(counter);
        checkEquals(1, counter.size(), "exactly the pooled item is reported");
        checkEquals(1234L, counter.get(AEItemKey.of(new ItemStack(Items.DIAMOND))), "reported amount");
        checkEquals(0L, counter.get(AEItemKey.of(new ItemStack(Items.EMERALD))), "items outside the pool are absent");
        helper.succeed();
    }

    /** Extraction must name an item, must honour the configured amount and must never deplete the pool. */
    static void extractionSemantics(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.DIAMOND));
        container.setReportedAmount(1234);
        MEStorage storage = CreativeContainerMeStorage.of(container);
        IActionSource source = IActionSource.empty();
        AEItemKey diamond = AEItemKey.of(new ItemStack(Items.DIAMOND));

        checkEquals(1234L, storage.extract(diamond, 9999, Actionable.SIMULATE, source), "simulated extraction");
        checkEquals(1234L, storage.extract(diamond, 9999, Actionable.MODULATE, source), "real extraction");
        checkEquals(1234L, storage.extract(diamond, 9999, Actionable.MODULATE, source), "extraction never depletes");
        checkEquals(0L, storage.extract(AEItemKey.of(new ItemStack(Items.EMERALD)), 64, Actionable.MODULATE, source),
                "an item outside the pool is refused");
        checkEquals(0L, storage.extract(diamond, 0, Actionable.MODULATE, source), "a zero-sized request is refused");
        checkEquals(0L, storage.insert(diamond, 64, Actionable.MODULATE, source), "insertion is refused");
        check(container.pool().contains(new ItemStack(Items.DIAMOND)), "the pool still holds the item");
        helper.succeed();
    }

    /** Lowering the reported amount immediately changes what the network sees. */
    static void reportedAmountIsLive(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.DIAMOND));
        MEStorage storage = CreativeContainerMeStorage.of(container);
        AEItemKey diamond = AEItemKey.of(new ItemStack(Items.DIAMOND));

        container.setReportedAmount(64);
        KeyCounter first = new KeyCounter();
        storage.getAvailableStacks(first);
        checkEquals(64L, first.get(diamond), "the network follows the configured amount");

        container.setReportedAmount(Integer.MAX_VALUE);
        KeyCounter second = new KeyCounter();
        storage.getAvailableStacks(second);
        checkEquals((long) Integer.MAX_VALUE, second.get(diamond), "and follows it back up");
        helper.succeed();
    }
}
