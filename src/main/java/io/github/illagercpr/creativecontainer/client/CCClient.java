package io.github.illagercpr.creativecontainer.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.illagercpr.creativecontainer.registry.CCMenus;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only registrations.
 *
 * <p>This class is referenced exclusively from the mod constructor behind a {@code Dist.CLIENT} check, so a dedicated
 * server never loads it (and therefore never resolves client-only event types).
 */
public final class CCClient {

    /** Category of the key bindings this mod adds. */
    public static final String KEY_CATEGORY = "key.categories.creativecontainer";

    /**
     * Designates the hovered pool slot as the outlet generic logistics pipes pull from (press again to clear).
     * Only meaningful while the container screen is open, so it keeps the vanilla "in-screen" conflict context.
     */
    public static final KeyMapping SELECT_POOL_ITEM = new KeyMapping(
            "key.creativecontainer.select_pool_item",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY);

    private CCClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CCClient::onRegisterMenuScreens);
        modEventBus.addListener(CCClient::onRegisterKeyMappings);
    }

    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CCMenus.CREATIVE_CONTAINER.get(), CreativeContainerScreen::new);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SELECT_POOL_ITEM);
    }
}
