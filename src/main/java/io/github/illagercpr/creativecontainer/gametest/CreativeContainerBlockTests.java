package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.SMOKE;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkSameStack;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.makeMockServerPlayer;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.placeContainer;

import io.github.illagercpr.creativecontainer.CCConfig;
import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.container.CreativeItemPool;
import io.github.illagercpr.creativecontainer.container.CreativePoolItemHandler;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerMenu;
import io.github.illagercpr.creativecontainer.network.PoolDeltaPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * World-level behaviour of the creative container: the block entity, both capability paths, persistence and the menu.
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeContainerBlockTests {

    private CreativeContainerBlockTests() {
    }

    @GameTest(template = SMOKE)
    public static void placedBlockHasUsableBlockEntity(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        check(container != null, "the block must expose its block entity");
        checkEquals(CreativeItemPool.MAX_AMOUNT, container.reportedAmount(), "a new container reports the maximum");
        checkEquals((long) CCConfig.POOL_SLOTS.get().intValue(), (long) container.pool().slotCount(),
                "a new container honours the configured slot count");
        check(container.availableItems().isEmpty(), "a new container is empty");
        helper.succeed();
    }

    /**
     * BaseEntityBlock defaults to {@code RenderShape.INVISIBLE}, which leaves a placed container invisible (t1 bug
     * report): only the selection outline shows while the item form renders fine. The block must keep requesting its
     * chunk-baked model.
     */
    @GameTest(template = SMOKE)
    public static void placedBlockRendersItsModel(GameTestHelper helper) {
        placeContainer(helper, 1, 1, 1);
        checkEquals(net.minecraft.world.level.block.RenderShape.MODEL,
                helper.getBlockState(new BlockPos(1, 1, 1)).getRenderShape(),
                "a placed container must render its block model");
        helper.succeed();
    }

    /**
     * v0.1.1 semantics: slot-based extraction is served only from the designated pipe outlet. With no designation
     * every slot refuses; after designating, the outlet is an endless source and the other slots stay shut.
     */
    @GameTest(template = SMOKE)
    public static void itemHandlerServesOnlyTheDesignatedOutlet(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.DIAMOND));
        container.addItem(new ItemStack(Items.EMERALD));

        IItemHandler handler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(1, 1, 1)), null);
        check(handler != null, "the item handler capability must be exposed");

        // No designation -> every slot refuses.
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            check(handler.extractItem(slot, 64, false).isEmpty(),
                    "extraction must be refused before an outlet is designated: " + slot);
        }

        // Designate slot 0 (diamonds): the outlet serves the item endlessly, everything else stays refused.
        container.toggleDesignatedSlot(0);
        checkEquals(16, handler.extractItem(0, 16, false).getCount(), "the outlet hands out the requested amount");
        checkEquals(Items.DIAMOND, handler.extractItem(0, 64, false).getItem(), "the outlet keeps serving");
        check(handler.extractItem(1, 64, false).isEmpty(), "other slots must stay refused");
        check(container.pool().contains(new ItemStack(Items.DIAMOND)), "and never depletes the pool");

        // Pressing the key on the designated slot again clears it.
        container.toggleDesignatedSlot(0);
        checkEquals(-1, container.designatedSlot(), "the designation must clear");
        check(handler.extractItem(0, 16, false).isEmpty(), "after clearing, extraction is refused again");

        // Empty slots cannot become the outlet.
        container.toggleDesignatedSlot(5);
        checkEquals(-1, container.designatedSlot(), "an empty slot must not become the outlet");

        // The item-aware path is unaffected by the designation.
        CreativePoolItemHandler poolHandler = container.itemHandler();
        ItemStack extracted = poolHandler.extractItemMatching(new ItemStack(Items.EMERALD), 8);
        checkEquals(8, extracted.getCount(), "item-aware extraction hands out the requested amount");

        // Insertion is still refused: the container never stores anything.
        ItemStack leftover = handler.insertItem(0, new ItemStack(Items.DIAMOND, 32), false);
        checkEquals(32, leftover.getCount(), "insertion must be refused");
        helper.succeed();
    }

    /** Each container owns its pool and its designation; neighbours never leak into each other. */
    @GameTest(template = SMOKE)
    public static void outletsAreIndependentPerContainer(GameTestHelper helper) {
        CreativeContainerBlockEntity first = placeContainer(helper, 1, 1, 1);
        CreativeContainerBlockEntity second = placeContainer(helper, 1, 2, 1);
        first.addItem(new ItemStack(Items.DIAMOND));
        second.addItem(new ItemStack(Items.GOLD_INGOT));

        first.toggleDesignatedSlot(0);
        checkEquals(0, first.designatedSlot(), "the first container designates its diamond");
        checkEquals(-1, second.designatedSlot(), "the second container stays undesignated");

        IItemHandler secondHandler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(new BlockPos(1, 2, 1)), null);
        check(secondHandler.extractItem(0, 8, false).isEmpty(), "the undesignated container must refuse extraction");

        second.toggleDesignatedSlot(0);
        checkEquals(8, secondHandler.extractItem(0, 8, false).getCount(), "the second outlet serves gold");
        checkEquals(Items.GOLD_INGOT, secondHandler.extractItem(0, 8, false).getItem(), "its own item, not the neighbour's");
        helper.succeed();
    }

    /** The designation is persisted and survives a save/reload cycle. */
    @GameTest(template = SMOKE)
    public static void designatedOutletSurvivesSaveAndLoad(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.NETHER_STAR));
        container.setReportedAmount(4242);
        container.toggleDesignatedSlot(0);

        net.minecraft.nbt.CompoundTag tag = container.saveCustomOnly(helper.getLevel().registryAccess());
        CreativeContainerBlockEntity reloaded = new CreativeContainerBlockEntity(
                container.getBlockPos(), container.getBlockState());
        reloaded.loadCustomOnly(tag, helper.getLevel().registryAccess());

        checkEquals(4242, reloaded.reportedAmount(), "the reported amount must survive a reload");
        checkEquals(0, reloaded.designatedSlot(), "the designation must survive a reload");
        check(reloaded.pool().contains(new ItemStack(Items.NETHER_STAR)), "pool contents must survive a reload");
        helper.succeed();
    }

    /** Compaction moves the outlet with its item; removing the outlet item clears the designation. */
    @GameTest(template = SMOKE)
    public static void removalRepointsTheOutlet(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.DIAMOND));
        container.addItem(new ItemStack(Items.EMERALD));
        container.toggleDesignatedSlot(1); // the emerald

        container.removeSlot(0); // the diamond leaves; the emerald slides from slot 1 to slot 0
        checkEquals(0, container.designatedSlot(), "the designation must follow the item through compaction");
        checkSameStack(new ItemStack(Items.EMERALD), container.pool().getSlot(container.designatedSlot()),
                "the outlet still points at the emerald");

        container.removeSlot(0); // the emerald itself leaves
        checkEquals(-1, container.designatedSlot(), "removing the outlet item must clear the designation");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void containerSurvivesSaveAndLoad(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.NETHER_STAR));
        container.setReportedAmount(4242);

        net.minecraft.nbt.CompoundTag tag = container.saveCustomOnly(helper.getLevel().registryAccess());
        CreativeContainerBlockEntity reloaded = new CreativeContainerBlockEntity(
                container.getBlockPos(), container.getBlockState());
        reloaded.loadCustomOnly(tag, helper.getLevel().registryAccess());

        checkEquals(4242, reloaded.reportedAmount(), "the reported amount must survive a reload");
        check(reloaded.pool().contains(new ItemStack(Items.NETHER_STAR)), "pool contents must survive a reload");
        helper.succeed();
    }

    @GameTest(template = SMOKE)
    public static void updateTagCarriesPoolToClients(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        container.addItem(new ItemStack(Items.DIAMOND));
        container.setReportedAmount(99);
        container.toggleDesignatedSlot(0);

        net.minecraft.nbt.CompoundTag updateTag = container.getUpdateTag(helper.getLevel().registryAccess());
        check(updateTag.contains(CreativeContainerBlockEntity.TAG_AMOUNT), "the update tag carries the amount");
        check(updateTag.contains(CreativeContainerBlockEntity.TAG_DESIGNATED), "the update tag carries the outlet");
        CreativeContainerBlockEntity client = new CreativeContainerBlockEntity(
                container.getBlockPos(), container.getBlockState());
        client.loadWithComponents(updateTag, helper.getLevel().registryAccess());
        checkEquals(99, client.reportedAmount(), "the mirrored container sees the amount");
        checkEquals(0, client.designatedSlot(), "the mirrored container sees the outlet");
        check(client.pool().contains(new ItemStack(Items.DIAMOND)), "the mirrored container sees the pool");
        helper.succeed();
    }

    /**
     * In-game changes travel as incremental {@link PoolDeltaPayload}s instead of full block-entity snapshots. The
     * mirror must apply all four delta shapes: a single added slot, an amount-only change, a designation-only change
     * and a full ordered resync (after a compaction).
     */
    @GameTest(template = SMOKE)
    public static void clientMirrorAppliesDeltas(GameTestHelper helper) {
        CreativeContainerBlockEntity server = placeContainer(helper, 1, 1, 1);
        server.addItem(new ItemStack(Items.DIAMOND));
        server.setReportedAmount(77);

        CreativeContainerBlockEntity mirror = new CreativeContainerBlockEntity(
                server.getBlockPos(), server.getBlockState());
        mirror.loadWithComponents(server.getUpdateTag(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        check(mirror.pool().contains(new ItemStack(Items.DIAMOND)), "the mirror starts from the full snapshot");
        checkEquals(77, mirror.reportedAmount(), "the snapshot carries the amount");

        // amount-only delta
        mirror.applyClientDelta(PoolDeltaPayload.amount(server.getBlockPos(), 123, -1));
        checkEquals(123, mirror.reportedAmount(), "an amount-only delta updates the mirror");
        check(mirror.pool().contains(new ItemStack(Items.DIAMOND)), "an amount-only delta keeps the contents");

        // single-slot delta
        server.addItem(new ItemStack(Items.EMERALD));
        mirror.applyClientDelta(PoolDeltaPayload.slot(server.getBlockPos(), 123, -1,
                server.pool().filledSlots() - 1, new ItemStack(Items.EMERALD)));
        check(mirror.pool().contains(new ItemStack(Items.EMERALD)), "a slot delta lands in the mirror");
        check(mirror.pool().contains(new ItemStack(Items.DIAMOND)), "a slot delta keeps the other entries");

        // designation-only delta: the mirror's golden frame follows without touching the contents
        mirror.applyClientDelta(PoolDeltaPayload.amount(server.getBlockPos(), 123, 0));
        checkEquals(0, mirror.designatedSlot(), "a designation-only delta updates the outlet");
        checkSameStack(server.pool().getSlot(0), mirror.pool().getSlot(0), "and keeps the contents");

        // full resync after a removal compacted the server pool
        server.toggleDesignatedSlot(0); // the server designates the diamond too
        server.removeSlot(0);
        mirror.applyClientDelta(PoolDeltaPayload.fullResync(server.getBlockPos(), 123,
                server.designatedSlot(), server.pool().availableItems()));
        check(!mirror.pool().contains(new ItemStack(Items.DIAMOND)), "the resync wipes removed entries");
        checkSameStack(server.pool().availableItems().get(0), mirror.pool().availableItems().get(0),
                "the resync rebuilds the mirror in server order");
        checkEquals(server.pool().slotCount(), mirror.pool().slotCount(), "the resync keeps the size");
        checkEquals(server.designatedSlot(), mirror.designatedSlot(),
                "the resync carries the re-pointed outlet");
        helper.succeed();
    }

    private static int countInInventory(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(template = SMOKE)
    public static void menuEditsPoolAndRespectsDistance(GameTestHelper helper) {
        CreativeContainerBlockEntity container = placeContainer(helper, 1, 1, 1);
        ServerPlayer player = makeMockServerPlayer(helper);
        // The mock player spawns at the world spawn point, far away from the plot: stillValid() would fail until the
        // player is actually standing next to the block.
        BlockPos absolute = helper.absolutePos(new BlockPos(1, 1, 1));
        player.teleportTo(helper.getLevel(), absolute.getX() + 0.5, absolute.getY() + 1.0, absolute.getZ() + 0.5, 0.0F, 0.0F);

        CreativeContainerMenu menu = new CreativeContainerMenu(0, player.getInventory(), container);
        check(menu.stillValid(player), "the menu must be valid while the player is next to the block");

        menu.handleAddToPool(new ItemStack(Items.DIAMOND));
        check(container.pool().contains(new ItemStack(Items.DIAMOND)), "the GUI adds items to the pool");

        menu.handleSetAmount(777);
        checkEquals(777, container.reportedAmount(), "the GUI sets the reported amount");
        checkEquals(777, menu.reportedAmount(), "the menu reads the amount back");
        menu.handleSetAmount(0);
        checkEquals(1, container.reportedAmount(), "the GUI cannot set an amount below one");

        menu.handlePickItem(new ItemStack(Items.EMERALD, 8), -1, player);
        checkEquals(8, countInInventory(player, Items.EMERALD), "taken items land in the player inventory");

        menu.handleRemoveSlot(0);
        check(container.availableItems().isEmpty(), "the GUI can remove a pool entry");

        // The outlet designation goes through the menu as well, and respects the distance check like everything else.
        menu.handleAddToPool(new ItemStack(Items.GOLD_INGOT));
        menu.handleSelectPoolSlot(0, player);
        checkEquals(0, container.designatedSlot(), "the GUI can designate the pipe outlet");
        player.teleportTo(helper.getLevel(), absolute.getX() + 64.0, absolute.getY(), absolute.getZ(), 0.0F, 0.0F);
        check(!menu.stillValid(player), "the menu must close when the player walks away");
        menu.handleSelectPoolSlot(1, player);
        checkEquals(0, container.designatedSlot(), "a far-away player cannot change the outlet");
        helper.succeed();
    }
}
