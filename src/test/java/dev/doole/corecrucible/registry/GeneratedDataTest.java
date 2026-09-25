package dev.doole.corecrucible.registry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Keeps tools/gen_data.py and tools/gen_textures.py in sync with the items registered in Java: every item
 * {@link ModItems#register} would register on this version needs its lang entry, model, item definition and texture,
 * and (checked on 1.21.1, which registers every item) the lang file names no item that Java doesn't register.
 */
class GeneratedDataTest {

    private static final String ASSETS = "/assets/dooles_core_crucible/";

    // The item ids ModItems.register registers on this version, in the same order. Items can't be built outside
    // registration, so this walks the same loops rather than calling register itself.
    private static List<String> registeredIds() {
        List<String> ids = new ArrayList<>();
        for (ModTiers tier : ModItems.ALLOY_TIERS) ids.add(tier.prefix() + "_alloy_ingot");
        for (ModTiers tier : ModItems.ALLOY_TIERS) {
            if (tier != ModTiers.NETHERITE) ids.add(tier.prefix() + "_upgrade_smithing_template");
        }
        for (ModTiers tier : ModTiers.values()) {
            for (EquipType type : EquipType.IN_ORDER) {
                if (type.existsFor(tier) && !ModItems.isVanilla(tier, type)) ids.add(tier.prefix() + "_" + type.suffix());
            }
        }
        if (!ModItems.VANILLA_COPPER_AND_SPEARS) {
            ids.add("golden_spear");
            ids.add("copper_nugget");
        }
        return ids;
    }

    @Test
    void everyItemHasItsData() throws IOException {
        JsonObject lang = readJson(ASSETS + "lang/en_us.json");
        List<String> missing = new ArrayList<>();
        for (String id : registeredIds()) {
            if (!lang.has("item.dooles_core_crucible." + id)) missing.add(id + ": lang entry");
            if (!exists(ASSETS + "models/item/" + id + ".json")) missing.add(id + ": model");
            if (!exists(ASSETS + "items/" + id + ".json")) missing.add(id + ": item definition");
            if (!exists(ASSETS + "textures/item/" + id + ".png")) missing.add(id + ": texture");
        }
        assertTrue(missing.isEmpty(), "Run tools/gen_data.py and tools/gen_textures.py. Missing: " + missing);
    }

    @Test
    void langNamesNoUnregisteredItems() throws IOException {
        // 26.x leaves the 1.21.1-only items unregistered but they stay in the shared lang file, so only 1.21.1
        // (where every item is registered) can tell a leftover entry from a version-specific one.
        if (ModItems.VANILLA_COPPER_AND_SPEARS) return;
        Set<String> ids = new LinkedHashSet<>(registeredIds());
        List<String> extra = new ArrayList<>();
        for (String key : readJson(ASSETS + "lang/en_us.json").keySet()) {
            if (!key.startsWith("item.dooles_core_crucible.")) continue;
            String id = key.substring("item.dooles_core_crucible.".length());
            // Deeper keys (smithing_template.<name>.applies_to, ...) are template text, not item names.
            if (!id.contains(".") && !ids.contains(id)) extra.add(id);
        }
        assertTrue(extra.isEmpty(), "gen_data.py names items ModItems doesn't register: " + extra);
    }

    private static boolean exists(String path) {
        return GeneratedDataTest.class.getResource(path) != null;
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream in = GeneratedDataTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, path + " is missing");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
