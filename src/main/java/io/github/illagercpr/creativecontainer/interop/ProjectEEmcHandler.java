package io.github.illagercpr.creativecontainer.interop;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.interop.projecte.ProjectEEmcBridge;
import io.github.illagercpr.creativecontainer.registry.CCItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Drives the ProjectE EMC integration.
 *
 * <p>ProjectE computes its EMC values on data pack sync ({@link OnDatapackSyncEvent}, which also fires once at server
 * start). Writing our entry before that computation — during {@link ServerAboutToStartEvent} — means the value is in
 * place for the very first computation, with no extra reload. The write is idempotent, so it is repeated on every sync
 * as a cheap safety net.
 */
public final class ProjectEEmcHandler {

    private static boolean applied;

    private ProjectEEmcHandler() {
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!InteropHooks.projectEAvailable()) {
            return;
        }
        MinecraftServer server = event.getServer();
        boolean ok = ProjectEEmcBridge.apply(server, CCItems.CREATIVE_CONTAINER.get());
        if (ok) {
            applied = true;
            CreativeContainer.LOGGER.info("ProjectE integration active ({}): the creative container carries {} EMC.",
                    ProjectEEmcBridge.describeEnvironment(), ProjectEEmcBridge.targetValue());
        }
    }

    /** Idempotent re-assertion: covers a deleted config file or a late-loaded ProjectE. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (!InteropHooks.projectEAvailable()) {
            return;
        }
        ProjectEEmcBridge.apply(event.getPlayerList().getServer(), CCItems.CREATIVE_CONTAINER.get());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!InteropHooks.projectEAvailable()) {
            return;
        }
        long reported = ProjectEEmcBridge.queryValue(new ItemStack(CCItems.CREATIVE_CONTAINER.get()));
        long expected = ProjectEEmcBridge.targetValue();
        if (reported == expected) {
            CreativeContainer.LOGGER.info("ProjectE reports {} EMC for the creative container.", reported);
        } else if (applied) {
            CreativeContainer.LOGGER.warn("ProjectE currently reports {} EMC for the creative container, expected {}. "
                    + "Run /reload once to recompute EMC values.", reported, expected);
        }
    }
}
