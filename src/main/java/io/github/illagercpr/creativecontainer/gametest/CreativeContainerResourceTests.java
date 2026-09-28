package io.github.illagercpr.creativecontainer.gametest;

import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.INTEROP;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.check;
import static io.github.illagercpr.creativecontainer.gametest.CreativeContainerTestSupport.checkEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.illagercpr.creativecontainer.CreativeContainer;
import io.github.illagercpr.creativecontainer.menu.CreativeContainerLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the shipped resources.
 *
 * <p>Assets are read from the classpath on purpose: the server-side resource manager only scans {@code data/}, so
 * {@code assets/} lookups through it would fail even though the files exist (a trap that costs an afternoon).
 */
@GameTestHolder(CreativeContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeContainerResourceTests {

    private static final String NS = "creativecontainer";

    private CreativeContainerResourceTests() {
    }

    @GameTest(template = INTEROP)
    public static void languageFilesCoverEveryKey(GameTestHelper helper) {
        String[] required = {
                "block.creativecontainer.creative_container",
                "itemGroup.creativecontainer",
                "tooltip.creativecontainer.creative_container.1",
                "tooltip.creativecontainer.creative_container.2",
                "gui.creativecontainer.search",
                "gui.creativecontainer.search.hint",
                "gui.creativecontainer.pool",
                "gui.creativecontainer.amount",
                "gui.creativecontainer.amount.label",
                "gui.creativecontainer.amount.tooltip",
                "gui.creativecontainer.designate.hint",
                "key.categories.creativecontainer",
                "key.creativecontainer.select_pool_item",
        };
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            JsonObject json = json("/assets/" + NS + "/lang/" + locale + ".json");
            for (String key : required) {
                check(json.has(key), locale + " is missing the translation key " + key);
            }
        }
        helper.succeed();
    }

    @GameTest(template = INTEROP)
    public static void modelsAndBlockStateReferenceTheRealTexture(GameTestHelper helper) {
        JsonObject blockState = json("/assets/" + NS + "/blockstates/creative_container.json");
        String model = blockState.getAsJsonObject("variants").getAsJsonObject("")
                .get("model").getAsString();
        checkEquals(NS + ":block/creative_container", model, "block state model");

        JsonObject blockModel = json("/assets/" + NS + "/models/block/creative_container.json");
        String texture = blockModel.getAsJsonObject("textures").get("all").getAsString();
        checkEquals(NS + ":block/creative_container", texture, "block model texture");
        check(bytes("/assets/" + NS + "/textures/block/creative_container.png").length > 0,
                "the block texture must exist");

        JsonObject itemModel = json("/assets/" + NS + "/models/item/creative_container.json");
        checkEquals(NS + ":block/creative_container", itemModel.get("parent").getAsString(), "item model parent");
        helper.succeed();
    }

    /** The screen blits fixed regions, so the shipped textures must match the layout constants exactly. */
    @GameTest(template = INTEROP)
    public static void guiTexturesMatchTheLayout(GameTestHelper helper) {
        byte[] background = bytes("/assets/" + NS + "/textures/gui/creative_container.png");
        checkEquals(CreativeContainerLayout.IMAGE_WIDTH, pngWidth(background), "GUI background width");
        checkEquals(CreativeContainerLayout.IMAGE_HEIGHT, pngHeight(background), "GUI background height");

        byte[] slot = bytes("/assets/" + NS + "/textures/gui/slot.png");
        checkEquals(CreativeContainerLayout.CELL, pngWidth(slot), "slot cell width");
        checkEquals(CreativeContainerLayout.CELL, pngHeight(slot), "slot cell height");
        helper.succeed();
    }

    @GameTest(template = INTEROP)
    public static void dataFilesAreValid(GameTestHelper helper) {
        JsonObject loot = json("/data/" + NS + "/loot_table/blocks/creative_container.json");
        checkEquals("minecraft:block", loot.get("type").getAsString(), "loot table type");
        String dropped = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                .getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString();
        checkEquals(NS + ":creative_container", dropped, "the block drops itself");

        // v0.1.1: no recipe is shipped any more — modpack authors provide their own; the creative tab stays.
        check(CreativeContainerResourceTests.class.getResourceAsStream("/data/" + NS + "/recipe/creative_container.json")
                == null, "no default recipe may be shipped; modpacks configure their own");
        helper.succeed();
    }

    // ---- classpath helpers -----------------------------------------------------------------------

    private static byte[] bytes(String path) {
        try (InputStream in = CreativeContainerResourceTests.class.getResourceAsStream(path)) {
            check(in != null, "missing resource: " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("could not read " + path + ": " + e);
        }
    }

    private static JsonObject json(String path) {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static int pngWidth(byte[] png) {
        return readInt(png, 16);
    }

    private static int pngHeight(byte[] png) {
        return readInt(png, 20);
    }

    private static int readInt(byte[] data, int offset) {
        check(data.length > offset + 4, "truncated PNG");
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }
}
