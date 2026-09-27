package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity types. */
public final class CCBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreativeContainer.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeContainerBlockEntity>> CREATIVE_CONTAINER =
            BLOCK_ENTITIES.register("creative_container",
                    () -> BlockEntityType.Builder.of(CreativeContainerBlockEntity::new, CCBlocks.CREATIVE_CONTAINER.get())
                            .build(null));

    private CCBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
