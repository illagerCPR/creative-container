package io.github.illagercpr.creativecontainer.interop;

import net.neoforged.fml.ModList;

/**
 * Presence checks for the optional integrations.
 *
 * <p>The mod must load, place blocks and work with none of AE2 / ProjectE / Project Expansion installed, so neither the
 * core classes nor this class may reference a foreign type: the AE2 capability holder and the ProjectE bridge are the
 * only places that do, and both are loaded exclusively behind the checks below.
 */
public final class InteropHooks {

    public static final String AE2_MOD_ID = "ae2";
    public static final String PROJECT_E_MOD_ID = "projecte";
    public static final String PROJECT_EXPANSION_MOD_ID = "projectexpansion";

    private static Boolean ae2;
    private static Boolean projectE;
    private static Boolean projectExpansion;

    private InteropHooks() {
    }

    private static boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }

    /** Whether the ME storage integration is usable (AE2 present). */
    public static boolean ae2Available() {
        if (ae2 == null) {
            ae2 = isLoaded(AE2_MOD_ID);
        }
        return ae2;
    }

    public static boolean projectEAvailable() {
        if (projectE == null) {
            projectE = isLoaded(PROJECT_E_MOD_ID);
        }
        return projectE;
    }

    public static boolean projectExpansionAvailable() {
        if (projectExpansion == null) {
            projectExpansion = isLoaded(PROJECT_EXPANSION_MOD_ID);
        }
        return projectExpansion;
    }
}
