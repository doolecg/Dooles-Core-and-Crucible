package dev.doole.corecrucible.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.gameplay.TemplateLoot;
import dev.doole.corecrucible.registry.BodyArmorStats;
import dev.doole.corecrucible.registry.LauncherStats;
import dev.doole.corecrucible.registry.ModArmorMaterials;
import dev.doole.corecrucible.registry.ModTiers;
import dev.doole.corecrucible.registry.RodStats;
import dev.doole.corecrucible.registry.ShearsStats;
import dev.doole.corecrucible.registry.ShieldStats;
import dev.doole.corecrucible.worldgen.VeinKind;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The balance config, {@code config/dooles_core_crucible.json}. Every number in it has a default in one of the
 * stats enums; the enums' getters ask {@link #get} for the configured value. The file is read once at startup, before
 * any item is registered, so it can change the values that get baked into items (durability, tool speed, armor).
 * Changes need a restart. Unit tests never load a file, so they always see the defaults.
 *
 * <p>The file is written with every option on first launch, in the game's {@code config/} folder. Sections: {@code tiers}
 * (tool durability, speed, damage), {@code armor}, {@code body_armor} (horse and wolf armor), {@code launchers} (bows and
 * crossbows), {@code rods}, {@code shears}, {@code shields}, {@code veins} (ore generation) and {@code template_loot}.
 * Delete a line to go back to its default, or delete the file to reset everything.
 */
public final class BalanceConfig {
    public static final String FILE_NAME = "dooles_core_crucible.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Written to the file as {@code config_version}. Bump it when a release changes a section's defaults enough that
     * old files should pick up the new ones, and list that section in {@link #RESETS}.
     */
    static final int VERSION = 5;
    /**
     * Sections (dotted paths) reset to the new defaults when a file older than the version is loaded (the player's
     * other values stay). 2: ore veins made much rarer, with pockets between them; 3: smaller diamond veins; 4: the
     * gems only in pockets of up to 8; 5: half the Coal, shorter and thinner veins for the other ores.
     */
    static final Map<Integer, List<String>> RESETS = Map.of(2, List.of("veins"), 3, List.of("veins.diamond"),
            4, List.of("veins.lapis", "veins.diamond", "veins.emerald"), 5, List.of("veins"));

    /** The loaded values by dotted key, e.g. {@code tiers.iron.durability}. Empty until {@link #load} runs. */
    private static final Map<String, Double> VALUES = new HashMap<>();

    private BalanceConfig() {
    }

    // Look up a configured value by its dotted key, or return the fallback (the Java default) if it isn't set.
    public static double get(String key, double fallback) {
        Double value = VALUES.get(key);
        return value != null ? value : fallback;
    }

    public static float get(String key, float fallback) {
        Double value = VALUES.get(key);
        return value != null ? value.floatValue() : fallback;
    }

    public static int get(String key, int fallback) {
        Double value = VALUES.get(key);
        return value != null ? (int) Math.round(value) : fallback;
    }

    /** Every option with its default, in the order the file lists them. */
    public static Map<String, Double> defaults() {
        Map<String, Double> out = new LinkedHashMap<>();
        // Each stats class lists its own options. To add a new option: put it in that class's writeDefaults, then read it
        // with BalanceConfig.get(key, default) where it's used. It appears in players' files on their next launch.
        ModTiers.writeDefaults(out);
        ModArmorMaterials.writeDefaults(out);
        BodyArmorStats.writeDefaults(out);
        LauncherStats.writeDefaults(out);
        RodStats.writeDefaults(out);
        ShearsStats.writeDefaults(out);
        ShieldStats.writeDefaults(out);
        VeinKind.writeDefaults(out);
        TemplateLoot.writeDefaults(out);
        return out;
    }

    /**
     * Reads the file from {@code configDir}, keeping the default for anything missing or invalid, then writes the file
     * back so it lists every option (the player's own values stay as they were).
     */
    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        JsonObject user = null;
        if (Files.exists(file)) {
            try {
                JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    user = parsed.getAsJsonObject();
                } else {
                    CoreCrucible.LOGGER.warn("{} is not a JSON object; using the defaults", file);
                }
            } catch (IOException | RuntimeException e) {
                CoreCrucible.LOGGER.warn("Couldn't read {}; using the defaults: {}", file, e.getMessage());
            }
        }
        List<String> warnings = new ArrayList<>();
        // Old file from an earlier release? Reset the sections whose defaults changed (see RESETS).
        if (user != null) upgrade(user, warnings);
        Map<String, Double> values = merge(defaults(), user, warnings);
        for (String warning : warnings) CoreCrucible.LOGGER.warn("{}: {}", FILE_NAME, warning);
        VALUES.clear();
        VALUES.putAll(values);
        // A file we couldn't parse is left alone, so a typo doesn't wipe the player's settings.
        if (user == null && Files.exists(file)) return;
        try {
            JsonObject out = new JsonObject();
            out.addProperty("config_version", VERSION);
            toJson(values).entrySet().forEach(e -> out.add(e.getKey(), e.getValue()));
            String text = GSON.toJson(out) + "\n";
            if (!Files.exists(file) || !Files.readString(file, StandardCharsets.UTF_8).equals(text)) {
                Files.createDirectories(configDir);
                Files.writeString(file, text, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            CoreCrucible.LOGGER.warn("Couldn't write {}: {}", file, e.getMessage());
        }
    }

    /**
     * Brings a file written by an older release up to date: drops its {@code config_version} and every section whose
     * defaults changed since, so those use the new defaults. A file without a version predates versioning (1).
     */
    static void upgrade(JsonObject user, List<String> warnings) {
        JsonElement version = user.remove("config_version");
        int from = version != null && version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber()
                ? version.getAsInt() : 1;
        for (int v = from + 1; v <= VERSION; v++) {
            for (String section : RESETS.getOrDefault(v, List.of())) {
                if (remove(user, section)) {
                    warnings.add("the " + section + " section was reset to this version's new defaults");
                }
            }
        }
    }

    /** Removes the member at a dotted path, e.g. {@code veins.diamond}; true if there was one. */
    private static boolean remove(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject node = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = node.get(parts[i]);
            if (next == null || !next.isJsonObject()) return false;
            node = next.getAsJsonObject();
        }
        return node.remove(parts[parts.length - 1]) != null;
    }

    /**
     * The value of every default key: the player's number where it's valid, else the default. Unknown keys and bad
     * values add a line to {@code warnings}. Only a vein's rarity (and favored_rarity) may be negative.
     */
    static Map<String, Double> merge(Map<String, Double> defaults, JsonObject user, List<String> warnings) {
        Map<String, JsonElement> given = new LinkedHashMap<>();
        if (user != null) flatten("", user, given);
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement value = given.remove(key);
            double number = entry.getValue();
            if (value != null) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    double read = value.getAsDouble();
                    if (!Double.isFinite(read) || (read < 0 && !key.endsWith("rarity"))) {
                        warnings.add(key + " = " + value + " is out of range; using " + format(number));
                    } else {
                        number = read;
                    }
                } else {
                    warnings.add(key + " = " + value + " is not a number; using " + format(number));
                }
            }
            out.put(key, number);
        }
        for (String unknown : given.keySet()) warnings.add("unknown option " + unknown + " (ignored)");
        return out;
    }

    /** Nests dotted keys back into objects, writing whole numbers without a decimal point. */
    static JsonObject toJson(Map<String, Double> values) {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            JsonObject node = root;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!node.has(parts[i])) node.add(parts[i], new JsonObject());
                node = node.getAsJsonObject(parts[i]);
            }
            node.add(parts[parts.length - 1], number(entry.getValue()));
        }
        return root;
    }

    /** Whole numbers without a decimal point; float defaults as written in Java (0.1, not 0.10000000149011612). */
    private static JsonPrimitive number(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return new JsonPrimitive((long) v);
        float f = (float) v;
        return new JsonPrimitive(f == v ? Double.parseDouble(Float.toString(f)) : v);
    }

    // Turns nested JSON ({"tiers": {"iron": {"durability": 250}}}) into dotted keys (tiers.iron.durability).
    private static void flatten(String prefix, JsonObject object, Map<String, JsonElement> out) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue().isJsonObject()) {
                flatten(key, entry.getValue().getAsJsonObject(), out);
            } else {
                out.put(key, entry.getValue());
            }
        }
    }

    private static String format(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }
}
