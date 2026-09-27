package io.github.illagercpr.creativecontainer.block;

import io.github.illagercpr.creativecontainer.CCConfig;
import io.github.illagercpr.creativecontainer.container.CreativeItemPool;
import io.github.illagercpr.creativecontainer.container.CreativePoolItemHandler;
import io.github.illagercpr.creativecontainer.network.PoolDeltaPayload;
import io.github.illagercpr.creativecontainer.registry.CCBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity of the creative container.
 *
 * <p>It owns a {@link CreativeItemPool} (unlimited items, one type per slot) and the reported amount that storage
 * buses see. This class intentionally references no AE2 type: the ME adapter lives in
 * {@code interop.ae2.CreativeContainerMeStorage} and is only instantiated when AE2 is loaded.
 */
public class CreativeContainerBlockEntity extends BlockEntity {

    public static final String TAG_POOL = "Pool";
    public static final String TAG_AMOUNT = "ReportedAmount";

    private final CreativeItemPool pool = new CreativeItemPool(defaultPoolSlots());
    private final CreativePoolItemHandler itemHandler = new CreativePoolItemHandler(pool);

    /**
     * Cached AE2 ME view of this container, or {@code null}. Typed as {@link Object} on purpose: the field must exist
     * on installations without AE2, so no AE2 type may appear in this class.
     */
    @Nullable
    private Object ae2Storage;

    public CreativeContainerBlockEntity(BlockPos pos, BlockState state) {
        super(CCBlockEntities.CREATIVE_CONTAINER.get(), pos, state);
    }

    public CreativeItemPool pool() {
        return pool;
    }

    public CreativePoolItemHandler itemHandler() {
        return itemHandler;
    }

    public int reportedAmount() {
        return pool.reportedAmount();
    }

    public void setReportedAmount(int amount) {
        if (pool.setReportedAmount(amount)) {
            pushDelta(PoolDeltaPayload.amount(worldPosition, pool.reportedAmount()));
        }
    }

    /** Adds one template stack to the pool. @return whether the pool changed */
    public boolean addItem(ItemStack stack) {
        if (pool.addItem(stack)) {
            // A new item always lands at the end of the contiguous filled prefix.
            pushDelta(PoolDeltaPayload.slot(worldPosition, pool.reportedAmount(), pool.filledSlots() - 1,
                    pool.getSlot(pool.filledSlots() - 1)));
            return true;
        }
        return false;
    }

    /** @return whether the pool changed */
    public boolean removeSlot(int index) {
        if (pool.removeSlot(index)) {
            // Compaction reorders the tail, so the mirror gets a full ordered resync. Removals are a manual GUI
            // action, which keeps this rare.
            pushDelta(PoolDeltaPayload.fullResync(worldPosition, pool.reportedAmount(), pool.availableItems()));
            return true;
        }
        return false;
    }

    public List<ItemStack> availableItems() {
        return pool.availableItems();
    }

    @Nullable
    public Object ae2Storage() {
        return ae2Storage;
    }

    public void setAe2Storage(@Nullable Object storage) {
        this.ae2Storage = storage;
    }

    /**
     * Marks the block entity dirty and pushes an incremental pool delta to every client tracking this chunk.
     *
     * <p>The vanilla full-pool update packet still exists ({@link #getUpdateTag()} / {@link #getUpdatePacket()}), but
     * it only travels on chunk loads; in-game changes ship as {@link PoolDeltaPayload}s so the largest configured
     * pools stay cheap to edit. With no player tracking the chunk (headless GameTests) this is a no-op.
     */
    private void pushDelta(PoolDeltaPayload payload) {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(worldPosition), payload);
        }
    }

    /** Client side: applies an incremental pool delta pushed by the server (see {@link PoolDeltaPayload}). */
    public void applyClientDelta(PoolDeltaPayload payload) {
        if (!payload.pos().equals(worldPosition)) {
            return;
        }
        pool.setReportedAmount(payload.reportedAmount());
        if (payload.clearFirst()) {
            pool.clear();
        }
        for (PoolDeltaPayload.SlotChange change : payload.changes()) {
            pool.setSlot(change.index(), change.stack());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_AMOUNT, pool.reportedAmount());
        ContainerHelper.saveAllItems(tag, pool.slots(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // A container saved with a larger configuration keeps its size; the config can only grow new containers.
        int savedSlots = tag.contains(ContainerHelper.TAG_ITEMS, Tag.TAG_LIST)
                ? tag.getList(ContainerHelper.TAG_ITEMS, Tag.TAG_COMPOUND).size()
                : 0;
        pool.resize(Math.max(defaultPoolSlots(), savedSlots));
        NonNullList<ItemStack> slots = pool.slots();
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, slots, registries);
        pool.setReportedAmount(tag.contains(TAG_AMOUNT) ? tag.getInt(TAG_AMOUNT) : defaultReportedAmount());
        pool.compact();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static int defaultReportedAmount() {
        return CCConfig.DEFAULT_REPORTED_AMOUNT.get();
    }

    public static int defaultPoolSlots() {
        return CCConfig.POOL_SLOTS.get();
    }
}
