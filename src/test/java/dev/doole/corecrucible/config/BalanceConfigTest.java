package dev.doole.corecrucible.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Tests for the config file: upgrading old files, merging player values with defaults, and writing the JSON back.
// These run during ./gradlew build (or ./gradlew test) without starting Minecraft.
class BalanceConfigTest {

    private static Map<String, Double> sample() {
        Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("tiers.iron.durability", 600.0);
        defaults.put("tiers.iron.mining_speed", 6.0);
        defaults.put("veins.coal_upper.rarity", -0.05);
        defaults.put("template_loot.simple_dungeon", 0.5);
        return defaults;
    }

    @Test
    void oldFilesPickUpResetSections() {
        JsonObject user = JsonParser.parseString(
                "{\"tiers\": {\"iron\": {\"durability\": 999}}, \"veins\": {\"diamond\": {\"rarity\": 0.1}}}").getAsJsonObject();
        List<String> warnings = new ArrayList<>();
        BalanceConfig.upgrade(user, warnings);
        // No config_version means version 1, so version 2's vein reset applies; the player's tier value stays.
        assertFalse(user.has("veins"));
        assertTrue(user.has("tiers"));
        assertEquals(1, warnings.size());

        // A version 3 file loses only the sections reset since: version 4's veins.lapis (a nested path), then
        // version 5's whole veins section. Its tier value stays.
        JsonObject v3 = JsonParser.parseString("{\"config_version\": 3, \"tiers\": {\"iron\": {\"durability\": 999}}, "
                + "\"veins\": {\"lapis\": {\"size\": 0.6}, \"coal_upper\": {\"size\": 1.4}}}").getAsJsonObject();
        warnings.clear();
        BalanceConfig.upgrade(v3, warnings);
        assertFalse(v3.has("veins"));
        assertTrue(v3.has("tiers"));
        assertEquals(2, warnings.size());

        JsonObject current = JsonParser.parseString(
                "{\"config_version\": " + BalanceConfig.VERSION + ", \"veins\": {\"diamond\": {\"rarity\": 0.1}}}").getAsJsonObject();
        warnings.clear();
        BalanceConfig.upgrade(current, warnings);
        assertTrue(current.has("veins"));
        assertFalse(current.has("config_version"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void playerValuesReplaceDefaults() {
        JsonObject user = JsonParser.parseString("{'tiers': {'iron': {'durability': 900}}}").getAsJsonObject();
        List<String> warnings = new ArrayList<>();
        Map<String, Double> merged = BalanceConfig.merge(sample(), user, warnings);
        assertEquals(900.0, merged.get("tiers.iron.durability"));
        assertEquals(6.0, merged.get("tiers.iron.mining_speed")); // missing: the default, written back to the file
        assertEquals(sample().keySet(), merged.keySet());
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void badValuesFallBackWithAWarning() {
        JsonObject user = JsonParser.parseString("{'tiers': {'iron': {'durability': 'lots', 'mining_speed': -2}},"
                + " 'veins': {'coal_upper': {'rarity': -0.2}}, 'typo': 1}").getAsJsonObject();
        List<String> warnings = new ArrayList<>();
        Map<String, Double> merged = BalanceConfig.merge(sample(), user, warnings);
        assertEquals(600.0, merged.get("tiers.iron.durability"));
        assertEquals(6.0, merged.get("tiers.iron.mining_speed"));
        assertEquals(-0.2, merged.get("veins.coal_upper.rarity")); // the one kind of option that may be negative
        assertFalse(merged.containsKey("typo"));
        assertEquals(3, warnings.size(), warnings.toString());
    }

    @Test
    void writesNestedObjectsAndWholeNumbers() {
        JsonObject json = BalanceConfig.toJson(sample());
        assertEquals("600", json.getAsJsonObject("tiers").getAsJsonObject("iron").get("durability").toString());
        assertEquals("-0.05", json.getAsJsonObject("veins").getAsJsonObject("coal_upper").get("rarity").toString());
        List<String> warnings = new ArrayList<>();
        assertEquals(sample(), BalanceConfig.merge(sample(), json, warnings));
        assertTrue(warnings.isEmpty());
        // A float default (e.g. 0.1F widened to double) is written the way the Java source spells it.
        Map<String, Double> floats = Map.of("armor.obsidian.knockback_resistance", (double) 0.1F);
        assertEquals("0.1", BalanceConfig.toJson(floats).getAsJsonObject("armor").getAsJsonObject("obsidian")
                .get("knockback_resistance").toString());
    }

    @Test
    void defaultsCoverEverySection() {
        Map<String, Double> defaults = BalanceConfig.defaults();
        assertEquals(600.0, defaults.get("tiers.iron.durability"));
        assertEquals(600.0, defaults.get("shields.iron.durability"));
        assertEquals(4500.0, defaults.get("launchers.reinforced.bow_durability"));
        assertEquals(0.5, defaults.get("template_loot.simple_dungeon"));
        assertTrue(defaults.containsKey("veins.diamond.size"));
        assertTrue(defaults.containsKey("body_armor.reinforced.horse_defense"));
        assertTrue(defaults.containsKey("armor.reinforced.chestplate"));
        // With no file loaded, every getter returns its default.
        assertEquals(600, ModTiers.IRON.durability());
    }
}
