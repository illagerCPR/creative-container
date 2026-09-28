package io.github.illagercpr.creativecontainer.network;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: designate (or re-designate) the pool slot that generic logistics pipes pull from, or clear the
 * designation with {@code -1}. Sent when the player presses the "select pipe outlet" key ({@code R} by default) while
 * hovering a pool slot in the container screen.
 */
public record SelectPoolSlotPayload(int slot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectPoolSlotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "select_pool_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPoolSlotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SelectPoolSlotPayload::slot,
                    SelectPoolSlotPayload::new
            );

    public static SelectPoolSlotPayload select(int slot) {
        return new SelectPoolSlotPayload(slot);
    }

    public static SelectPoolSlotPayload clear() {
        return new SelectPoolSlotPayload(-1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
