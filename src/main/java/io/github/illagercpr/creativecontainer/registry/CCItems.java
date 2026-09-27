package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items: currently only the block item of the creative container. */
public final class CCItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreativeContainer.MOD_ID);

    public static final DeferredItem<BlockItem> CREATIVE_CONTAINER = ITEMS.registerSimpleBlockItem(
            "creative_container",
            CCBlocks.CREATIVE_CONTAINER,
            new Item.Properties().fireResistant()
    );

    private CCItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
