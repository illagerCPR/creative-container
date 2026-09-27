package io.github.illagercpr.creativecontainer.network;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Client -> server: add one item type to the container's virtual pool. */
public record AddToPoolPayload(ItemStack stack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AddToPoolPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CreativeContainer.MOD_ID, "add_to_pool"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddToPoolPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, AddToPoolPayload::stack,
                    AddToPoolPayload::new
            );

    public AddToPoolPayload {
        stack = stack.copyWithCount(1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
