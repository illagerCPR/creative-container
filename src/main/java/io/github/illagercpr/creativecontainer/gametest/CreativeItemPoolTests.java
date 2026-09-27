package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.SMOKE;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkSameStack;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.container.CreativeItemPool;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The pool rules of the creative container, exercised without a world: unlimited items, a global reported amount, and
 * the "never extract when no item is named" guarantee.
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeItemPoolTests {

    private CreativeItemPoolTests() {
    }

    @GameTest(template = SMOKE)
    public static void startsEmptyAndReportsMaxAmount(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        check(pool.isEmpty(), "a fresh pool must be empty");
        checkEquals(CreativeItemPool.MAX_AMOUNT, pool.reportedAmount(), "default reported amount");
        checkEquals(0L, pool.extract(new ItemStack(Items.DIAMOND), 64), "extracting from an empty pool");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void holdsOneTypePerSlotAndNeverRunsOut(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        check(pool.addItem(new ItemStack(Items.DIAMOND, 64)), "first insert must change the pool");
        check(!pool.addItem(new ItemStack(Items.DIAMOND, 1)), "the same item must not take a second slot");
        check(pool.addItem(new ItemStack(Items.EMERALD)), "a different item takes the next slot");
        checkEquals(2, pool.availableItems().size(), "pool contents");

        // The pool reports the configured amount and hands it out again and again.
        checkEquals(Integer.MAX_VALUE, pool.reportableAmount(new ItemStack(Items.DIAMOND)), "reported diamonds");
        checkEquals(4096L, pool.extract(new ItemStack(Items.DIAMOND), 4096), "extract 4096 diamonds");
        checkEquals(4096L, pool.extract(new ItemStack(Items.DIAMOND), 4096), "extract again: never depletes");
        checkEquals(Integer.MAX_VALUE, pool.reportableAmount(new ItemStack(Items.DIAMOND)), "still reports max");

        // An item that is not in the pool cannot be handed out.
        checkEquals(0L, pool.extract(new ItemStack(Items.GOLD_INGOT), 64), "extracting an unknown item");
        // An empty request is refused outright: the pool never guesses.
        checkEquals(0L, pool.extract(ItemStack.EMPTY, 64), "extracting without naming an item");
        checkEquals(0L, pool.extract(new ItemStack(Items.DIAMOND), 0), "extracting a zero amount");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void respectsMatchingComponents(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        enchanted.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        check(pool.addItem(enchanted), "component-carrying item is accepted");
        check(pool.contains(enchanted), "same components must match");
        check(!pool.contains(new ItemStack(Items.DIAMOND_SWORD)), "a plain sword is a different item");
        checkEquals(0L, pool.extract(new ItemStack(Items.DIAMOND_SWORD), 1), "plain sword is not supplied");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void reportedAmountIsClamped(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        pool.setReportedAmount(0);
        checkEquals(CreativeItemPool.MIN_AMOUNT, pool.reportedAmount(), "zero clamps up to the minimum");
        pool.setReportedAmount(-12345);
        checkEquals(CreativeItemPool.MIN_AMOUNT, pool.reportedAmount(), "negative clamps up to the minimum");
        pool.setReportedAmount(Integer.MAX_VALUE);
        checkEquals(Integer.MAX_VALUE, pool.reportedAmount(), "the maximum is exactly Integer.MAX_VALUE");

        pool.addItem(new ItemStack(Items.DIAMOND));
        pool.setReportedAmount(7);
        checkEquals(7L, pool.extract(new ItemStack(Items.DIAMOND), 1000), "extraction is capped by the amount");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void removalCompactsThePool(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        pool.addItem(new ItemStack(Items.DIAMOND));
        pool.addItem(new ItemStack(Items.EMERALD));
        pool.addItem(new ItemStack(Items.GOLD_INGOT));
        check(pool.removeSlot(0), "removing a filled slot");
        // Slot 0 is refilled by compaction, so the "already empty" case has to name a slot past the contents.
        check(!pool.removeSlot(5), "removing an already empty slot");
        check(!pool.removeSlot(-1), "removing an out-of-range slot");
        checkSameStack(new ItemStack(Items.EMERALD), pool.getSlot(0), "the pool compacts towards slot zero");
        checkSameStack(new ItemStack(Items.GOLD_INGOT), pool.getSlot(1), "second item follows");
        check(pool.getSlot(2).isEmpty(), "the tail is empty");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void poolHasFiftyFourSlots(GameTestHelper helper) {
        CreativeItemPool pool = new CreativeItemPool();
        int inserted = 0;
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            if (pool.addItem(new ItemStack(item))) {
                inserted++;
            }
            if (inserted == CreativeItemPool.SLOT_COUNT) {
                break;
            }
        }
        checkEquals(CreativeItemPool.SLOT_COUNT, inserted, "a full pool holds exactly 54 distinct items");

        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item == Items.AIR || pool.contains(new ItemStack(item))) {
                continue;
            }
            check(!pool.addItem(new ItemStack(item)), "a full pool refuses further items");
            break;
        }
        helper.succeed();
    }
}
