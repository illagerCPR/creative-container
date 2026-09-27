package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.placeContainer;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.storagebus.StorageBusPart;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * A real, running ME network — energy cell, cable, storage bus attached to the creative container, export bus draining
 * into a chest — instead of the capability-level views {@link Ae2TestSupport} verifies.
 *
 * <p>This class references {@code appeng} types, so it is only ever loaded when AE2 is present; the GameTest holder
 * calls into it after checking {@code InteropHooks.ae2Available()}. Parts are placed through the public
 * {@link PartHelper#setPart} API, the same entry point AE2's own test plots use.
 */
public final class Ae2NetworkTestSupport {

    /** Local coordinates: the creative container under test. */
    private static final BlockPos CONTAINER_POS = new BlockPos(1, 1, 1);
    /** Local coordinates: the cable bus (glass cable + storage bus + export bus). */
    private static final BlockPos CABLE_POS = new BlockPos(2, 1, 1);
    /** Local coordinates: the chest the export bus drains into. */
    private static final BlockPos CHEST_POS = new BlockPos(3, 1, 1);
    /** Local coordinates: the creative energy cell powering the ad-hoc network. */
    private static final BlockPos ENERGY_POS = new BlockPos(2, 2, 1);

    private static final int REPORTED_AMOUNT = 1234;

    private Ae2NetworkTestSupport() {
    }

    /**
     * Builds the minimal real network around the creative container and returns the storage bus part facing it.
     *
     * <p>Layout on the base layer: {@code [container][cable bus][chest]}, with the creative energy cell above the cable
     * bus. The storage bus on the cable's west face reads the container; the export bus on the east face drains the
     * network into the chest.
     */
    static StorageBusPart buildNetwork(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, CONTAINER_POS.getX(), CONTAINER_POS.getY(),
                CONTAINER_POS.getZ());
        container.addItem(new ItemStack(Items.DIAMOND));
        container.setReportedAmount(REPORTED_AMOUNT);

        // Energy first, so the cable bus that comes next forms into a powered network immediately.
        helper.setBlock(ENERGY_POS, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(CHEST_POS, Blocks.CHEST);

        ServerLevel level = helper.getLevel();
        // A fake player instead of a connected mock player: ProjectE (co-installed in dev runs) syncs its world
        // transmutations on real logins only, and part placement needs no connection — this mirrors how AE2's own
        // test plots place parts (Platform.getFakePlayer).
        Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos cablePos = helper.absolutePos(CABLE_POS);
        PartHelper.setPart(level, cablePos, Direction.UP, player, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        StorageBusPart storageBus = PartHelper.setPart(level, cablePos, Direction.WEST, player,
                AEParts.STORAGE_BUS.asItem());
        ExportBusPart exportBus = PartHelper.setPart(level, cablePos, Direction.EAST, player,
                AEParts.EXPORT_BUS.asItem());
        check(storageBus != null, "the storage bus must be placed on the cable bus");
        check(exportBus != null, "the export bus must be placed on the cable bus");
        exportBus.getConfig().addFilter(Items.DIAMOND);
        return storageBus;
    }

    /**
     * The whole storage-bus read path with a live network: the grid inventory must report the pooled item at the
     * configured amount, must yield it on extraction, and must refuse anything the pool does not hold.
     */
    static void networkSeesThePoolAndYieldsItems(GameTestHelper helper) {
        StorageBusPart storageBus = buildNetwork(helper);
        AEItemKey diamond = AEItemKey.of(new ItemStack(Items.DIAMOND));

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    IGridNode node = storageBus.getGridNode();
                    check(node != null, "the storage bus must host a grid node");
                    check(node.isActive(), "the ad-hoc network must be online (powered, within the channel budget)");

                    IStorageService storageService = node.getGrid().getStorageService();
                    MEStorage network = storageService.getInventory();
                    KeyCounter available = new KeyCounter();
                    network.getAvailableStacks(available);
                    checkEquals(1, available.size(), "the network sees exactly the pooled item");
                    checkEquals(REPORTED_AMOUNT, available.get(diamond),
                            "each pooled item reports N through the real storage bus");

                    checkEquals(16L, network.extract(diamond, 16, Actionable.MODULATE, IActionSource.empty()),
                            "the network can extract the pooled item");
                    checkEquals(0L,
                            network.extract(AEItemKey.of(new ItemStack(Items.EMERALD)), 16, Actionable.MODULATE,
                                    IActionSource.empty()),
                            "items outside the pool are refused");
                })
                .thenSucceed();
    }

    /**
     * The full closed loop a player builds in game: export bus pulls the pooled item out of the network — through the
     * real storage bus attached to the container — and drops it into a chest, forever (the pool never depletes).
     */
    static void exportBusDrainsThePoolIntoAChest(GameTestHelper helper) {
        buildNetwork(helper);
        helper.succeedWhen(() -> {
            helper.assertContainerContains(CHEST_POS, Items.DIAMOND);
        });
    }
}
