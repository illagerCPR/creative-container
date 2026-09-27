package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.INTEROP;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.container.CreativeItemIndex;
import io.github.illagercpr.creativecontainer.registry.CCBlocks;
import io.github.illagercpr.creativecontainer.registry.CCCreativeTabs;
import io.github.illagercpr.creativecontainer.registry.CCItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The item catalogue behind the GUI, plus the creative-tab invariants NeoForge enforces at runtime.
 *
 * <p>NeoForge rejects any creative tab entry whose stack count is not exactly 1, and a missing block item silently
 * becomes {@code AIR} with count 0 — both crash the game when a player opens the creative inventory. The tab is rebuilt
 * here through the same wrapper the client uses, so a regression fails the GameTests instead of the players.
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeItemIndexTests {

    private CreativeItemIndexTests() {
    }

    @GameTest(template = INTEROP)
    public static void indexContainsOurItem(GameTestHelper helper) {
        CreativeItemIndex.rebuild(helper.getLevel().registryAccess(), helper.getLevel().enabledFeatures());
        check(!CreativeItemIndex.isEmpty(), "the creative item index must not be empty");
        check(CreativeItemIndex.size() > 100, "the index must contain the vanilla catalogue, got " + CreativeItemIndex.size());

        boolean found = CreativeItemIndex.search("creative_container", Integer.MAX_VALUE).stream()
                .anyMatch(entry -> entry.stack().is(CCItems.CREATIVE_CONTAINER.get()));
        check(found, "the creative container must be reachable from the search field");
        helper.succeed();
    }

    @GameTest(template = INTEROP)
    public static void searchMatchesIdAndIsCaseInsensitive(GameTestHelper helper) {
        CreativeItemIndex.rebuild(helper.getLevel().registryAccess(), helper.getLevel().enabledFeatures());

        List<CreativeItemIndex.Entry> diamonds = CreativeItemIndex.search("DIAMOND", 64);
        check(!diamonds.isEmpty(), "the search must be case insensitive");
        check(diamonds.size() <= 64, "the search honours its limit");

        check(CreativeItemIndex.search("definitely-not-an-item-xyz", 8).isEmpty(),
                "an unknown query returns nothing");
        checkEquals(CreativeItemIndex.size(), CreativeItemIndex.search("", Integer.MAX_VALUE).size(),
                "an empty query returns everything");
        helper.succeed();
    }

    @GameTest(template = INTEROP)
    public static void creativeTabEntriesAreSingleNonAirStacks(GameTestHelper helper) {
        CreativeModeTab tab = CCCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));

        var items = tab.getDisplayItems();
        check(!items.isEmpty(), "our creative tab must not be empty");
        for (ItemStack stack : items) {
            check(!stack.isEmpty(), "a creative tab entry resolved to AIR (missing block item?)");
            checkEquals(1, stack.getCount(), "creative tab entries must be single stacks: " + stack);
        }
        boolean containsBlock = items.stream().anyMatch(stack -> stack.is(CCBlocks.CREATIVE_CONTAINER.get().asItem()));
        check(containsBlock, "the creative container must be in the mod's own tab");
        helper.succeed();
    }

    @GameTest(template = INTEROP)
    public static void blockAndItemShareOneRegistryName(GameTestHelper helper) {
        checkEquals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(CCBlocks.CREATIVE_CONTAINER.get())
                        .toString(),
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(CCItems.CREATIVE_CONTAINER.get()).toString(),
                "block and item ids must match");
        check(CCBlocks.CREATIVE_CONTAINER.get().asItem() == CCItems.CREATIVE_CONTAINER.get(),
                "the block must resolve to its own block item");
        helper.succeed();
    }
}
