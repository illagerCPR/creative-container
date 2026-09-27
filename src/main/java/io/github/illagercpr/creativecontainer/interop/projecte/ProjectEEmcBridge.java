package io.github.illagercpr.creativecontainer.interop.projecte;

import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.interop.InteropHooks;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * Gives the creative container its absurd ProjectE EMC value.
 *
 * <p>Everything here goes through reflection on purpose: ProjectE is not a dependency of this mod, its artefacts are
 * not on Maven Central, and a compile-time binding would make the jar fail to load without it.
 *
 * <p>The write path is the very same one ProjectE's own {@code /projecte setemc} command uses:
 * {@code CustomEMCParser.init(registries)} (read the current {@code config/ProjectE/custom_emc.json}) followed by
 * {@code addToFile(nssItem, emc)} and {@code flush(registries)} (merge our entry in and save). ProjectE recomputes its
 * EMC values on every data pack sync, so writing before that computation makes the value take effect without any
 * extra reload — and because the entry is persisted, it survives {@code /reload} and server restarts.
 *
 * <p><b>Value limit:</b> ProjectE stores item EMC as a {@code long} (max {@code 9223372036854775807}). The value this
 * mod ships by default ({@value #BASE_EMC}) fits comfortably. The requirement for Project Expansion asks for
 * {@code 9999999999999999999999} (see {@link #PROJECT_EXPANSION_REQUESTED_EMC_TEXT}), ~1084x beyond a long and
 * therefore cannot be stored: Project Expansion's BigInteger support covers EMC <em>storage</em> (links, relays,
 * collectors) and player knowledge, while item values stay {@code long} there too — its {@code setemc} mixin only
 * rewrites the reload notice, not the argument type. We therefore write {@link Long#MAX_VALUE} in that case and say so
 * in the tooltip/docs instead of silently overflowing into a negative value.
 */
public final class ProjectEEmcBridge {

    /** EMC value with plain ProjectE: 999,999,999,999,999,999. */
    public static final long BASE_EMC = 999_999_999_999_999_999L;

    /**
     * The value the requirement asks for when Project Expansion is installed. Kept as text plus a big integer on
     * purpose: it is ~1084x larger than {@link Long#MAX_VALUE} and therefore cannot be stored by ProjectE's item EMC
     * map (see the class javadoc).
     */
    public static final String PROJECT_EXPANSION_REQUESTED_EMC_TEXT = "9999999999999999999999";
    public static final BigInteger PROJECT_EXPANSION_REQUESTED_EMC =
            new BigInteger(PROJECT_EXPANSION_REQUESTED_EMC_TEXT);

    private static final String CUSTOM_EMC_PARSER = "moze_intel.projecte.config.CustomEMCParser";
    private static final String NSS_ITEM = "moze_intel.projecte.api.nss.NSSItem";
    private static final String EMC_PROXY = "moze_intel.projecte.api.proxy.IEMCProxy";

    /** Mod id of the optional Project Expansion add-on. */
    private static final String PROJECT_EXPANSION_MOD_ID = "projectexpansion";

    private static boolean initialised;
    private static boolean broken;
    private static Method init;
    private static Method addToFile;
    private static Method flush;
    private static Method createItem;
    private static Object emcProxy;
    private static Method proxyGetValue;

    private ProjectEEmcBridge() {
    }

    /** The EMC value as requested by the requirements for the current environment (may exceed a long). */
    public static BigInteger requestedValue() {
        return InteropHooks.projectExpansionAvailable()
                ? PROJECT_EXPANSION_REQUESTED_EMC
                : BigInteger.valueOf(BASE_EMC);
    }

    /** The EMC value that can actually be written, clamped to what ProjectE's {@code long} map can hold. */
    public static long targetValue() {
        BigInteger requested = requestedValue();
        return requested.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0
                ? Long.MAX_VALUE
                : requested.longValueExact();
    }

    /**
     * Merges our EMC entry into ProjectE's custom EMC file.
     *
     * @return true when the entry is in place (or was already)
     */
    public static boolean apply(MinecraftServer server, ItemLike item) {
        if (broken || !resolve()) {
            return false;
        }
        HolderLookup.Provider registries = server.registryAccess();
        long value = targetValue();
        try {
            Object nssItem = createItem.invoke(null, item);
            // Mirrors ProjectE's own command: read current file, edit in memory, write back.
            init.invoke(null, registries);
            addToFile.invoke(null, nssItem, value);
            flush.invoke(null, registries);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            CreativeContainer.LOGGER.error("Could not register the ProjectE EMC value; the creative container will have "
                    + "no EMC value on this installation", e);
            return false;
        }
    }

    /**
     * Reads back what ProjectE currently reports for the item.
     *
     * @return the EMC value, or {@code -1} when it cannot be queried
     */
    public static long queryValue(ItemStack stack) {
        if (broken || !resolve() || emcProxy == null) {
            return -1L;
        }
        try {
            Object result = proxyGetValue.invoke(emcProxy, stack);
            return result instanceof Long value ? value : -1L;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1L;
        }
    }

    @Nullable
    public static String describeEnvironment() {
        if (!InteropHooks.projectEAvailable()) {
            return null;
        }
        boolean expansion = InteropHooks.projectExpansionAvailable();
        return "projecte" + (expansion ? " + " + PROJECT_EXPANSION_MOD_ID : "");
    }

    private static boolean resolve() {
        if (initialised) {
            return true;
        }
        initialised = true;
        try {
            Class<?> parser = Class.forName(CUSTOM_EMC_PARSER);
            Class<?> nssItem = Class.forName(NSS_ITEM);
            Class<?> proxy = Class.forName(EMC_PROXY);

            Class<?> registriesClass = HolderLookup.Provider.class;
            init = parser.getMethod("init", registriesClass);
            addToFile = parser.getMethod("addToFile", nssItem, long.class);
            flush = parser.getMethod("flush", registriesClass);
            createItem = nssItem.getMethod("createItem", ItemLike.class);

            Field instance = proxy.getField("INSTANCE");
            emcProxy = instance.get(null);
            proxyGetValue = proxy.getMethod("getValue", ItemStack.class);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            CreativeContainer.LOGGER.error("ProjectE is present but its EMC API could not be reached; skipping the EMC "
                    + "integration", e);
            return false;
        }
    }
}
