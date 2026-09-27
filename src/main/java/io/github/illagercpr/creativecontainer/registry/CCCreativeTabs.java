package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's own creative tab.
 *
 * <p>Note the single-stack rule: NeoForge rejects any stack whose count is not exactly 1 when a tab is built, so only
 * {@code new ItemStack(item)} entries are accepted here.
 */
public final class CCCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreativeContainer.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.creativecontainer"))
                    .icon(() -> new ItemStack(CCItems.CREATIVE_CONTAINER.get()))
                    .displayItems((parameters, output) -> output.accept(CCItems.CREATIVE_CONTAINER.get()))
                    .build());

    private CCCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
