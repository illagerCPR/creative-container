package io.github.illagercpr.creativecontainer.client;

import io.github.illagercpr.creativecontainer.registry.CCMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-only registrations.
 *
 * <p>This class is referenced exclusively from the mod constructor behind a {@code Dist.CLIENT} check, so a dedicated
 * server never loads it (and therefore never resolves client-only event types).
 */
public final class CCClient {

    private CCClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CCClient::onRegisterMenuScreens);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CCMenus.CREATIVE_CONTAINER.get(), CreativeContainerScreen::new);
    }
}
