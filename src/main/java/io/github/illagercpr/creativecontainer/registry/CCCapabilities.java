package io.github.illagercpr.creativecontainer.registry;

import io.github.illagercpr.creativecontainer.block.CreativeContainerBlockEntity;
import io.github.illagercpr.creativecontainer.interop.InteropHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers block capabilities.
 *
 * <p>The AE2 capability is only registered when AE2 is actually loaded: the {@code appeng.api.AECapabilities} class is
 * therefore never touched on an installation without AE2, and no AE2 type leaks into the core classes.
 */
public final class CCCapabilities {

    private CCCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CCCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                CCBlockEntities.CREATIVE_CONTAINER.get(),
                (CreativeContainerBlockEntity be, net.minecraft.core.Direction side) -> be.itemHandler()
        );

        if (InteropHooks.ae2Available()) {
            Ae2CapabilityRegistration.register(event);
        }
    }
}
