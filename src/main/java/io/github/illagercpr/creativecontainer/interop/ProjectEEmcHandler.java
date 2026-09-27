package io.github.illagercpr.creativecontainer.interop;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.interop.projecte.ProjectEEmcBridge;
import io.github.illagercpr.creativecontainer.registry.CCItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Drives the ProjectE EMC integration.
 *
 * <p>The entry is written during {@link ServerAboutToStartEvent} through the very same code path ProjectE's own
 * {@code /projecte setemc} command uses. ProjectE computes its EMC mapping while loading data packs — which happens
 * <em>before</em> any server lifecycle event — so the very first computation of a server start cannot see the write.
 * In normal play this never matters (ProjectE recomputes on every data pack sync, including each player joining), but
 * the handler also self-heals: after the server started it reads the value back and, on mismatch, re-posts the same
 * data-pack-sync event {@code /reload} uses so the mapping contains the value immediately — even on a headless server
 * that no player ever joins. The write is idempotent and repeated on every sync as a safety net.
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
        MinecraftServer server = event.getServer();
        long expected = ProjectEEmcBridge.targetValue();
        long reported = ProjectEEmcBridge.queryValue(new ItemStack(CCItems.CREATIVE_CONTAINER.get()));
        if (reported == expected) {
            CreativeContainer.LOGGER.info("ProjectE reports {} EMC for the creative container.", reported);
            return;
        }
        if (!applied) {
            CreativeContainer.LOGGER.warn("ProjectE reports {} EMC for the creative container, expected {}; the "
                    + "custom EMC entry could not be written. Run /reload once to recompute EMC values.",
                    reported, expected);
            return;
        }
        // ProjectE computed its mapping while loading data packs, which happens before any server event — the first
        // computation of this start could not see our write yet. Re-post the data pack sync (/reload's broadcast of
        // it, i.e. with no player) so every listener recomputes with the entry in place.
        CreativeContainer.LOGGER.info("ProjectE computed EMC before the custom entry was written (it reported {}); "
                + "triggering a data pack sync to recompute.", reported);
        NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(server.getPlayerList(), null));
        long healed = ProjectEEmcBridge.queryValue(new ItemStack(CCItems.CREATIVE_CONTAINER.get()));
        if (healed == expected) {
            CreativeContainer.LOGGER.info("ProjectE now reports {} EMC for the creative container.", healed);
        } else {
            CreativeContainer.LOGGER.warn("ProjectE still reports {} EMC for the creative container, expected {}. "
                    + "Run /reload once to recompute EMC values.", healed, expected);
        }
    }
}
