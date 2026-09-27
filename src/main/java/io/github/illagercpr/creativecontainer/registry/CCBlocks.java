package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.block.CreativeContainerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks. The matching block item lives in {@link CCItems}. */
public final class CCBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreativeContainer.MOD_ID);

    public static final DeferredBlock<CreativeContainerBlock> CREATIVE_CONTAINER = BLOCKS.register(
            "creative_container",
            () -> new CreativeContainerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    // hardness must stay >= 0: AE2's destruction plane refuses hardness == -1 outright.
                    .strength(3.5F, 1200.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
            )
    );

    private CCBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    /** Convenience for code that only needs the block instance. */
    public static Block block() {
        return CREATIVE_CONTAINER.get();
    }
}
