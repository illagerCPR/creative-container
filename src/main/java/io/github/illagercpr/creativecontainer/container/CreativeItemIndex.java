package io.github.illagercpr.creativecontainer.container;

import io.github.illagercpr.creativecontainer.pinyin.PinIn;
import io.github.illagercpr.creativecontainer.pinyin.searchers.Searcher;
import io.github.illagercpr.creativecontainer.pinyin.searchers.TreeSearcher;
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
 * <p>Search also matches <b>pinyin</b> (full syllables and first letters, e.g. {@code zsj} for 钻石剑) through a
 * vendored copy of <em>PinIn</em> — the library Just Enough Characters is built on. The match runs against the same
 * search keys, so it composes with the plain substring behaviour instead of replacing it; JEC itself is neither
 * required nor touched (it only hooks the JEI search box).
 *
 * <p>The list is server-safe (it only reads the creative tab registry, and PinIn is a plain string library), so the
 * same code backs the client grid and the headless GameTests.
 */
public final class CreativeItemIndex {

    private static volatile List<Entry> all = List.of();

    /** The pinyin matcher context; loading its dictionary costs tens of milliseconds, so it is built on first use. */
    private static volatile PinIn pinyinContext;

    /**
     * Pre-built pinyin index over every entry's search keys. Invalidated on rebuild and re-created lazily on the next
     * search: building it is a one-off cost, querying it is fast.
     */
    private static volatile TreeSearcher<Entry> pinyinSearcher;

    private static final Object SEARCHER_LOCK = new Object();

    private CreativeItemIndex() {
    }

    public record Entry(ItemStack stack, String displayName, String id, String namespace) {

        /**
         * Plain substring semantics over id / namespace / names. Kept as the definition of the ASCII behaviour;
         * {@link #search} additionally matches pinyin through the pre-built index.
         */
        public boolean matches(String query) {
            if (query.isEmpty()) {
                return true;
            }
            return id.contains(query) || namespace.contains(query) || displayName.contains(query);
        }

        /** Every string the search field matches against, joined into the single key the pinyin index stores. */
        public String searchKey() {
            return displayName + '\u0000' + id + '\u0000' + namespace;
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
        // The old index points at stale entries; the next search rebuilds it from the new list.
        pinyinSearcher = null;
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

    /**
     * Case-insensitive search over id / namespace / names, plus pinyin (full syllables and first letters). Results
     * keep the id-ordered list order; the query is lower-cased here.
     */
    public static List<Entry> search(String rawQuery, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        List<Entry> source = all;
        if (query.isEmpty()) {
            return new ArrayList<>(source.subList(0, Math.min(limit, source.size())));
        }
        List<Entry> candidates = pinyinSearcher().search(query);
        candidates.sort(Comparator.comparing(Entry::id));
        return candidates.size() > limit ? new ArrayList<>(candidates.subList(0, limit)) : candidates;
    }

    /** The lazily built pinyin index; created on first use and after every rebuild. */
    private static TreeSearcher<Entry> pinyinSearcher() {
        TreeSearcher<Entry> searcher = pinyinSearcher;
        if (searcher == null) {
            synchronized (SEARCHER_LOCK) {
                if (pinyinSearcher == null) {
                    TreeSearcher<Entry> built = new TreeSearcher<>(Searcher.Logic.CONTAIN, pinyinContext());
                    for (Entry entry : all) {
                        built.put(entry.searchKey(), entry);
                    }
                    pinyinSearcher = built;
                }
                searcher = pinyinSearcher;
            }
        }
        return searcher;
    }

    /** The shared PinIn context (one dictionary for the whole mod), with the JEC-style accelerator enabled. */
    private static PinIn pinyinContext() {
        PinIn context = pinyinContext;
        if (context == null) {
            synchronized (SEARCHER_LOCK) {
                if (pinyinContext == null) {
                    PinIn created = new PinIn();
                    created.config().accelerate(true);
                    pinyinContext = created;
                }
                context = pinyinContext;
            }
        }
        return context;
    }

    /** Number of items the index holds; used by guides and tests. */
    public static int size() {
        return all.size();
    }
}
