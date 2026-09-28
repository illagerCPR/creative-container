package io.github.illagercpr.creativecontainer.network;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Server -> client: an incremental pool update for one creative container.
 *
 * <p>The block entity used to re-send its whole pool (a full block-entity update tag) on every change, which becomes
 * a megabyte-scale packet for the largest configured pools. Instead, the server pushes only what changed:
 * <ul>
 *     <li>{@code clearFirst=false, changes=[one slot]} — a regular addition through the GUI/interop;</li>
 *     <li>{@code clearFirst=false, changes=[]} — only the reported amount and/or the designated pipe outlet changed;</li>
 *     <li>{@code clearFirst=true} — a removal compacted the pool, so the mirror is wiped and rebuilt from the carried
 *     (ordered) contents. Removals are a manual GUI action, so the full payload stays rare.</li>
 * </ul>
 * Every shape also carries the current designated pipe outlet (see {@code CreativeItemPool#designatedSlot}). The full
 * snapshot on chunk load still travels through the vanilla block-entity update tag; this payload only covers the
 * deltas in between.
 */
public record PoolDeltaPayload(BlockPos pos, int reportedAmount, int designatedSlot, boolean clearFirst,
                               List<SlotChange> changes)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PoolDeltaPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "pool_delta"));

    public record SlotChange(int index, ItemStack stack) {

        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SlotChange> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, SlotChange::index,
                        ItemStack.STREAM_CODEC, SlotChange::stack,
                        SlotChange::new);
    }

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PoolDeltaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PoolDeltaPayload::pos,
                    ByteBufCodecs.VAR_INT, PoolDeltaPayload::reportedAmount,
                    ByteBufCodecs.VAR_INT, PoolDeltaPayload::designatedSlot,
                    ByteBufCodecs.BOOL, PoolDeltaPayload::clearFirst,
                    ByteBufCodecs.collection(ArrayList::new, SlotChange.STREAM_CODEC), PoolDeltaPayload::changes,
                    PoolDeltaPayload::new);

    public PoolDeltaPayload {
        changes = List.copyOf(changes);
    }

    /** Only the reported amount and/or the designated outlet changed (the change list is empty). */
    public static PoolDeltaPayload amount(BlockPos pos, int reportedAmount, int designatedSlot) {
        return new PoolDeltaPayload(pos, reportedAmount, designatedSlot, false, List.of());
    }

    /** A single slot was added or replaced at {@code index}. */
    public static PoolDeltaPayload slot(BlockPos pos, int reportedAmount, int designatedSlot, int index,
                                        ItemStack stack) {
        return new PoolDeltaPayload(pos, reportedAmount, designatedSlot, false, List.of(new SlotChange(index, stack)));
    }

    /** The pool was compacted: wipe the mirror and rebuild it from the given ordered contents. */
    public static PoolDeltaPayload fullResync(BlockPos pos, int reportedAmount, int designatedSlot,
                                              List<ItemStack> contents) {
        List<SlotChange> changes = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            changes.add(new SlotChange(i, contents.get(i)));
        }
        return new PoolDeltaPayload(pos, reportedAmount, designatedSlot, true, changes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
