package io.github.illagercpr.creativecontainer.gametest;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Small assertion helpers shared by the GameTests.
 *
 * <p>{@code assert} statements are deliberately not used: they are disabled unless the JVM runs with {@code -ea}, which
 * would make the tests silently pass.
 */
final class CreativeContainerTestSupport {

    static final String SMOKE = "smoke";
    static final String INTEROP = "interop";

    private CreativeContainerTestSupport() {
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    static void checkEquals(long expected, long actual, String what) {
        if (expected != actual) {
            throw new GameTestAssertException(what + ": expected " + expected + " but was " + actual);
        }
    }

    /**
     * Compares item stacks the way the mod compares them in production code.
     *
     * <p>{@link net.minecraft.world.item.ItemStack#equals} is representation sensitive: a stack made by
     * {@code copyWithCount} can carry an empty patched component map where a freshly built stack carries {@code null},
     * so two stacks that print identically may not be {@code equals}. {@code isSameItemSameComponents} is the
     * comparison that actually matches gameplay semantics.
     */
    static void checkSameStack(net.minecraft.world.item.ItemStack expected, net.minecraft.world.item.ItemStack actual,
                               String what) {
        if (expected.getCount() != actual.getCount()
                || !net.minecraft.world.item.ItemStack.isSameItemSameComponents(expected, actual)) {
            throw new GameTestAssertException(what + ": expected " + expected + " but was " + actual);
        }
    }

    static void checkEquals(Object expected, Object actual, String what) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new GameTestAssertException(what + ": expected " + expected + " but was " + actual);
        }
    }

    /** Places a creative container and returns its block entity. */
    static io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity placeContainer(
            GameTestHelper helper, int x, int y, int z) {
        helper.setBlock(x, y, z, io.github.illagercpr.creativecontainer.registry.CCBlocks.CREATIVE_CONTAINER.get());
        return helper.getBlockEntity(new BlockPos(x, y, z));
    }

    static void log(String message) {
        CreativeContainer.LOGGER.info("[gametest] {}", message);
    }
}
