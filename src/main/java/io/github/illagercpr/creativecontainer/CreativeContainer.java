package io.github.illagercpr.creativecontainer;

import com.mojang.logging.LogUtils;
import io.github.illagercpr.creativecontainer.registry.CCBlockEntities;
import io.github.illagercpr.creativecontainer.registry.CCBlocks;
import io.github.illagercpr.creativecontainer.registry.CCCapabilities;
import io.github.illagercpr.creativecontainer.registry.CCCreativeTabs;
import io.github.illagercpr.creativecontainer.registry.CCItems;
import io.github.illagercpr.creativecontainer.registry.CCMenus;
import io.github.illagercpr.creativecontainer.registry.CCPayloads;
import io.github.illagercpr.creativecontainer.interop.ProjectEEmcHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Entry point of the Creative Container mod.
 *
 * <p>The mod is a pure add-on: it never requires Applied Energistics 2, ProjectE or Project Expansion to be present.
 * Every interop path is guarded by a runtime mod-presence check so that the shipped jar loads on a bare NeoForge
 * installation.
 */
@Mod(CreativeContainer.MOD_ID)
public final class CreativeContainer {

    public static final String MOD_ID = "creativecontainer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreativeContainer(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CCConfig.SPEC);

        CCBlocks.register(modEventBus);
        CCItems.register(modEventBus);
        CCBlockEntities.register(modEventBus);
        CCMenus.register(modEventBus);
        CCCreativeTabs.register(modEventBus);
        CCPayloads.register(modEventBus);
        CCCapabilities.register(modEventBus);

        // Game-bus hooks for the optional ProjectE integration. The listeners themselves are harmless no-ops when
        // ProjectE is absent; only the bridge they call knows ProjectE's class names (all through reflection).
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ProjectEEmcHandler::onServerAboutToStart);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ProjectEEmcHandler::onDatapackSync);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ProjectEEmcHandler::onServerStarted);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Loaded only on the client: the class references client-only event types.
            io.github.illagercpr.creativecontainer.client.CCClient.register(modEventBus);
        }
    }
}
