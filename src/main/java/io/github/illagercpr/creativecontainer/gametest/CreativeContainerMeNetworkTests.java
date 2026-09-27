package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.INTEROP;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.log;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.interop.InteropHooks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end tests over a real ME network (energy cell + cable + storage bus + export bus), not just the capability
 * views.
 *
 * <p>This class itself is free of AE2 types so that it loads on an installation without AE2; each test logs and passes
 * when AE2 is missing, and otherwise delegates to {@link Ae2NetworkTestSupport}, which is the only AE2-typed class here
 * and is therefore loaded strictly on demand.
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeContainerMeNetworkTests {

    private CreativeContainerMeNetworkTests() {
    }

    @GameTest(template = INTEROP)
    public static void networkSeesThePoolAndYieldsItems(GameTestHelper helper) {
        if (skipWithoutAe2(helper)) {
            return;
        }
        Ae2NetworkTestSupport.networkSeesThePoolAndYieldsItems(helper);
    }

    @GameTest(template = INTEROP)
    public static void exportBusDrainsThePoolIntoAChest(GameTestHelper helper) {
        if (skipWithoutAe2(helper)) {
            return;
        }
        Ae2NetworkTestSupport.exportBusDrainsThePoolIntoAChest(helper);
    }

    /** @return true when AE2 is absent, in which case the test passed vacuously */
    private static boolean skipWithoutAe2(GameTestHelper helper) {
        if (InteropHooks.ae2Available()) {
            return false;
        }
        log("AE2 is not installed; skipping the ME network end-to-end test");
        helper.succeed();
        return true;
    }
}
