package dev.doole.corecrucible.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Keeps each vein kind's ore yield at a set share of the vanilla placement it replaces. Yields are ore blocks per
 * block of the height range, assuming solid stone. The vanilla figures come from simulating vanilla's ore blob
 * algorithm with its counts and sizes (placements merged where a vein covers several), then corrected against a real
 * world: vanilla loses more deep ore to caves and to its discard-on-air rule than the simulation shows, so Copper,
 * Gold, Redstone, Lapis and Diamond were scaled to match a vanilla world's ore counts (seed 12345, 1024 chunks,
 * 26.3) against the same area generated with veins.
 */
class VeinShapeTest {

    private static final Map<VeinKind, Double> VANILLA = Map.ofEntries(
            Map.entry(VeinKind.COAL_UPPER, 0.0141),
            Map.entry(VeinKind.COAL_LOWER, 0.0090),
            Map.entry(VeinKind.IRON_UPPER, 0.0079),
            Map.entry(VeinKind.IRON_MIDDLE, 0.0033),
            Map.entry(VeinKind.IRON_SMALL, 0.0006),
            Map.entry(VeinKind.COPPER, 0.0038),
            Map.entry(VeinKind.COPPER_LARGE, 0.0160),
            // Gold and Diamond sit lower than vanilla's (-64..0, -64..-16): vanilla's ore per chunk over the new range.
            Map.entry(VeinKind.GOLD, 0.0013 * 97 / 65),
            Map.entry(VeinKind.GOLD_EXTRA, 0.0060),
            Map.entry(VeinKind.REDSTONE, 0.0024),
            Map.entry(VeinKind.LAPIS, 0.0010),
            Map.entry(VeinKind.DIAMOND, 0.0015 * 81 / 49),
            Map.entry(VeinKind.EMERALD, 0.0004));

    /** Ore share of the sampled blocks for this kind, over a wide area and a few seeds. */
    static double oreDensity(VeinKind kind) {
        Random random = new Random(42);
        int samples = 1_500_000;
        int ore = 0;
        for (long seed = 1; seed <= 3; seed++) {
            VeinShape.Sampler sampler = kind.sampler(seed);
            VeinShape s = kind.shape;
            for (int i = 0; i < samples; i++) {
                int x = random.nextInt(8192) - 4096;
                int z = random.nextInt(8192) - 4096;
                int y = s.minY() + random.nextInt(s.maxY() - s.minY() + 1);
                int c = sampler.classify(x, y, z);
                if (c == VeinShape.ORE || c == VeinShape.RAW) ore++;
            }
        }
        return ore / (3.0 * samples);
    }

    /** Ore share the small pockets add, over the same height range: pockets × average size per chunk column. */
    static double pocketDensity(VeinKind kind) {
        double averageSize = (1 + kind.pocketSize) / 2.0;
        return kind.pockets * averageSize / (256.0 * (kind.shape.maxY() - kind.shape.minY() + 1));
    }

    @Test
    void yieldsStayNearVanilla() {
        List<String> off = new ArrayList<>();
        StringBuilder report = new StringBuilder();
        for (VeinKind kind : VeinKind.values()) {
            double veins = oreDensity(kind) / VANILLA.get(kind);
            double total = veins + pocketDensity(kind) / VANILLA.get(kind);
            report.append(String.format("%s veins %.2f, total %.2f%n", kind, veins, total));
            // Ore is deliberately scarce: big veins hold under a tenth of vanilla's ore, and with the pockets each
            // kind yields about a quarter of vanilla's (the gems, pockets only, about a tenth).
            if (veins > 0.12) off.add(kind + String.format(" (veins %.2f× vanilla)", veins));
            if (total < 0.06 || total > 0.35) off.add(kind + String.format(" (total %.2f× vanilla)", total));
        }
        System.out.println(report);
        assertTrue(off.isEmpty(), "Vein yields off: " + off + "\n" + report);
    }

    @Test
    void cellCheckNeverSkipsAVein() {
        // mayContain must be true for every cell that holds a vein block, or chunks would lose ore.
        for (VeinKind kind : VeinKind.values()) {
            VeinShape.Sampler sampler = kind.sampler(7);
            Random random = new Random(3);
            for (int i = 0; i < 400_000; i++) {
                int x = random.nextInt(4096);
                int z = random.nextInt(4096);
                int y = kind.shape.minY() + random.nextInt(kind.shape.maxY() - kind.shape.minY() + 1);
                if (sampler.classify(x, y, z) == VeinShape.NONE) continue;
                int cx = (x & ~3) + 2, cy = Math.floorDiv(y, 4) * 4 + 2, cz = (z & ~3) + 2;
                assertTrue(sampler.mayContain(cx, cy, cz), kind + " vein block at " + x + "," + y + "," + z + " in a skipped cell");
            }
        }
    }

    @Test
    void replacedPlacementsAreSwitchedOff() throws IOException {
        // tools/gen_data.py writes a count-0 override for every vanilla ore placement a vein replaces or covers.
        List<String> names = new ArrayList<>(VeinKind.DISABLED_ONLY);
        for (VeinKind kind : VeinKind.values()) names.add(kind.replaces);
        List<String> wrong = new ArrayList<>();
        for (String name : names) {
            try (InputStream in = VeinShapeTest.class.getResourceAsStream("/data/minecraft/worldgen/placed_feature/" + name + ".json")) {
                if (in == null) {
                    wrong.add(name + ": missing");
                    continue;
                }
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject count = json.getAsJsonArray("placement").get(0).getAsJsonObject();
                if (count.get("count").getAsInt() != 0) wrong.add(name + ": still places ore");
            }
        }
        assertTrue(wrong.isEmpty(), "Run tools/gen_data.py. " + wrong);
    }
}
