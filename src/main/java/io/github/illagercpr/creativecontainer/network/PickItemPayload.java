package io.github.illagercpr.creativecontainer.network;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Client -> server: "put one of this item into inventory slot N". */
public record PickItemPayload(ItemStack stack, int slot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PickItemPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "pick_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PickItemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, PickItemPayload::stack,
                    ByteBufCodecs.VAR_INT, PickItemPayload::slot,
                    PickItemPayload::new
            );

    public PickItemPayload {
        // Never trust the client with an oversized stack: clamp to the item's own stack limit.
        stack = stack.copyWithCount(Math.max(1, Math.min(stack.getCount(), stack.getMaxStackSize())));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
