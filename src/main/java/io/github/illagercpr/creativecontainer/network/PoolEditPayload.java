package io.github.illagercpr.creativecontainer.network;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: remove a template from the pool (middle-click in the pool grid), or set the reported amount
 * ({@code amount >= 1}, {@code slot < 0}).
 */
public record PoolEditPayload(int slot, int amount) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PoolEditPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "pool_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PoolEditPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PoolEditPayload::slot,
                    ByteBufCodecs.VAR_INT, PoolEditPayload::amount,
                    PoolEditPayload::new
            );

    public static PoolEditPayload remove(int slot) {
        return new PoolEditPayload(slot, -1);
    }

    public static PoolEditPayload setAmount(int amount) {
        return new PoolEditPayload(-1, amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
