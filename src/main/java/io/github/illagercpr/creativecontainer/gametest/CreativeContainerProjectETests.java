package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.SMOKE;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.log;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.placeContainer;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.interop.InteropHooks;
import io.github.illagercpr.creativecontainer.interop.projecte.ProjectEEmcBridge;
import io.github.illagercpr.creativecontainer.registry.CCItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Live ProjectE verification: with real ProjectE on the class path (dev runtime only, pulled via cursemaven), the
 * custom EMC entry this mod writes at server start must actually show up in ProjectE's mapping.
 *
 * <p>The class references no ProjectE types — everything goes through {@link ProjectEEmcBridge}, which resolves
 * ProjectE reflectively. Without ProjectE the test logs and passes, so a dev environment that never pulled the jar
 * still runs green.
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeContainerProjectETests {

    private CreativeContainerProjectETests() {
    }

    /**
     * The agreed requirement value: the creative container carries 999,999,999,999,999,999 EMC (Project Expansion is
     * not on the dev class path, so {@link ProjectEEmcBridge#targetValue()} must be {@link ProjectEEmcBridge#BASE_EMC}
     * here).
     */
    @GameTest(template = SMOKE)
    public static void containerCarriesTheAgreedEmcValue(GameTestHelper helper) {
        if (skipWithoutProjectE(helper)) {
            return;
        }
        placeContainer(helper, 1, 1, 1);
        ItemStack stack = new ItemStack(CCItems.CREATIVE_CONTAINER.get());
        long expected = ProjectEEmcBridge.targetValue();
        log("Expecting ProjectE to report " + expected + " EMC for the creative container ("
                + ProjectEEmcBridge.describeEnvironment() + ")");
        checkEquals(ProjectEEmcBridge.BASE_EMC, expected,
                "without Project Expansion the target value must be the agreed base EMC");

        // ProjectE computes its EMC mapping on data pack sync, i.e. before the tests run; poll anyway so a slower
        // computation still converges instead of racing the first tick.
        helper.succeedWhen(() -> {
            long reported = ProjectEEmcBridge.queryValue(stack);
            checkEquals(expected, reported, "ProjectE EMC for the creative container");
        });
    }

    /** @return true when ProjectE is absent, in which case the test passed vacuously */
    private static boolean skipWithoutProjectE(GameTestHelper helper) {
        if (InteropHooks.projectEAvailable()) {
            return false;
        }
        log("ProjectE is not installed; skipping the live EMC verification");
        helper.succeed();
        return true;
    }
}
