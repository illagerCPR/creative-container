package io.github.illagercpr.creativecontainer.container;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * The list of items the creative container offers.
 *
 * <p>It is built from the creative mode tabs — exactly the definition of "everything obtainable from the creative
 * inventory" — so datapack and modded tab contents are respected. Entries are pre-computed search keys (registry id,
 * description id and the tab-translated display name) so that the search field matches the same way the vanilla
 * creative search does, plus the mod-id.
 *
 * <p>The list is server-safe (it only reads the creative tab registry), so the same code backs the client grid and the
 * headless GameTests.
 */
public final class CreativeItemIndex {

    private static volatile List<Entry> all = List.of();

    private CreativeItemIndex() {
    }

    public record Entry(ItemStack stack, String displayName, String id, String namespace) {

        public boolean matches(String query) {
            if (query.isEmpty()) {
                return true;
            }
            return id.contains(query) || namespace.contains(query) || displayName.contains(query);
        }
    }

    public static List<Entry> all() {
        return all;
    }

    public static boolean isEmpty() {
        return all.isEmpty();
    }

    /** Rebuilds the index. Safe to call repeatedly; the result replaces the previous list atomically. */
    public static void rebuild(HolderLookup.Provider registries, FeatureFlagSet enabledFeatures) {
        List<ItemStack> stacks = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            try {
                tab.buildContents(new CreativeModeTab.ItemDisplayParameters(enabledFeatures, true, registries));
                stacks.addAll(tab.getDisplayItems());
            } catch (Throwable t) {
                // A broken third-party tab must never take the GUI down with it.
                io.github.illagercpr.creativecontainer.CreativeContainer.LOGGER.warn(
                        "Skipping creative tab {} while building the creative container item index",
                        BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab), t);
            }
        }

        List<Entry> entries = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            entries.add(toEntry(stack));
        }

        if (entries.isEmpty()) {
            // Fallback: the tab registry gave us nothing (data packs without tabs, tests, ...). Use the item registry.
            BuiltInRegistries.ITEM.stream()
                    .filter(item -> item != net.minecraft.world.item.Items.AIR)
                    .map(ItemLike::asItem)
                    .map(ItemStack::new)
                    .map(CreativeItemIndex::toEntry)
                    .forEach(entries::add);
        }

        entries.sort(Comparator.comparing(Entry::id));
        all = List.copyOf(entries);
    }

    private static Entry toEntry(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = key.toString();
        String namespace = key.getNamespace();
        StringBuilder names = new StringBuilder(stack.getHoverName().getString());
        try {
            names.append('\u0000').append(Component.translatable(stack.getDescriptionId()).getString());
        } catch (Throwable ignored) {
            // A modded item with a broken translation must not break indexing.
        }
        return new Entry(stack.copyWithCount(1), names.toString().toLowerCase(Locale.ROOT), id.toLowerCase(Locale.ROOT),
                namespace.toLowerCase(Locale.ROOT));
    }

    /** Case-insensitive search over id / namespace / names. The query is lower-cased by the caller-facing method. */
    public static List<Entry> search(String rawQuery, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        List<Entry> result = new ArrayList<>(Math.min(limit, 256));
        for (Entry entry : all) {
            if (entry.matches(query)) {
                result.add(entry);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    /** Number of items the index holds; used by guides and tests. */
    public static int size() {
        return all.size();
    }
}
