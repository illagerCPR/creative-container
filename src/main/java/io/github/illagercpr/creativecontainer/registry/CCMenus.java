package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu types. */
public final class CCMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CreativeContainer.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CreativeContainerMenu>> CREATIVE_CONTAINER =
            MENUS.register("creative_container",
                    () -> IMenuTypeExtension.create(CreativeContainerMenu::new));

    private CCMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
