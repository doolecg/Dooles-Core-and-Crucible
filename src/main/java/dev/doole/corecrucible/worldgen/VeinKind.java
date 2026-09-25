package dev.doole.corecrucible.worldgen;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Every ore vein kind, named after the vanilla ore placement it replaces. Height ranges match vanilla's,
 * except Gold (-64..0) and Diamond (-64..-16), which sit lower. Ore is deliberately scarce: the rarity (region
 * threshold) and the vein spacing in {@link VeinShape} keep big veins under a tenth of the kind's vanilla ore, and
 * small pockets in every chunk bring each kind to about a quarter of vanilla's. The gems (Lapis, Diamond, Emerald) have
 * no big veins at all, only pockets of up to 8, and only Coal has long, thick ones. Big Coal veins are more common under forests and big Iron veins in
 * dripstone caves ({@link Favored}). {@code VeinShapeTest} checks the yields. Plain Java, so tests can read it;
 * {@link OreVeins} adds the blocks.
 */
public enum VeinKind {
    // These are the defaults. Players change them in config/dooles_core_crucible.json under "veins" (size, rarity,
    // ore_chance, filler_chance, raw_chance, patch, pockets, pocket_size, favored_rarity). Height ranges can only be
    // changed here. To make an ore more common: lower its rarity (more big veins) or raise its pockets per chunk.
    // If you change these, VeinShapeTest may need its expected yields updated.
    // VeinShape(minY, maxY, size, rarity, oreChance, fillerChance, rawChance[, patch]), pockets per chunk, max pocket size,
    // favored biome, rarity there (more big veins). Only Coal has big, long veins; the other ores' veins are
    // thinner (size 0.6) and cut short (patch 60), so a coal-sized vein of anything else is extremely rare.
    COAL_UPPER("ore_coal_upper", new VeinShape(136, 320, 1.4, 0.374, 0.50, 0.30, 0), 19.0, 5, Favored.FOREST, 0.126),
    COAL_LOWER("ore_coal_lower", new VeinShape(0, 192, 1.3, 0.417, 0.50, 0.30, 0), 12.5, 5, Favored.FOREST, 0.213),
    IRON_UPPER("ore_iron_upper", new VeinShape(80, 384, 0.6, 0.347, 0.45, 0.35, 0.03, 60), 35.0, 5, Favored.DRIPSTONE, 0.08),
    IRON_MIDDLE("ore_iron_middle", new VeinShape(-24, 56, 0.6, 0.404, 0.35, 0.40, 0.03, 60), 4.5, 5, Favored.DRIPSTONE, 0.17),
    IRON_SMALL("ore_iron_small", new VeinShape(-64, 72, 0.6, 0.528, 0.30, 0.30, 0.02, 60), 1.4, 5, Favored.DRIPSTONE, 0.39),
    COPPER("ore_copper", new VeinShape(-16, 112, 0.6, 0.383, 0.35, 0.40, 0.03, 60), 8.5, 5, Favored.NONE, 0),
    COPPER_LARGE("ore_copper_large", new VeinShape(-16, 112, 0.6, 0.266, 0.50, 0.30, 0.04, 60), 30.0, 5, Favored.NONE, 0),
    GOLD("ore_gold", new VeinShape(-64, 0, 0.6, 0.470, 0.30, 0.35, 0.02, 60), 2.15, 5, Favored.NONE, 0),
    GOLD_EXTRA("ore_gold_extra", new VeinShape(32, 256, 0.6, 0.329, 0.35, 0.35, 0.03, 60), 23.0, 5, Favored.NONE, 0),
    REDSTONE("ore_redstone", new VeinShape(-64, 15, 0.6, 0.415, 0.35, 0.35, 0, 60), 3.3, 5, Favored.NONE, 0),
    // The gems have no big veins (a rarity above 1 is never reached), only pockets of up to 8, so a find is never huge.
    LAPIS("ore_lapis", new VeinShape(-64, 64, 0.6, 1.5, 0.25, 0.25, 0), 0.56, 8, Favored.NONE, 0),
    DIAMOND("ore_diamond", new VeinShape(-64, -16, 0.33, 1.5, 0.25, 0.20, 0), 0.72, 8, Favored.NONE, 0),
    EMERALD("ore_emerald", new VeinShape(-16, 320, 0.45, 1.5, 0.25, 0.20, 0), 0.8, 8, Favored.NONE, 0);

    /** Vanilla ore placements switched off without a vein of their own: the veins above cover their ore. */
    public static final List<String> DISABLED_ONLY = List.of("ore_gold_lower", "ore_redstone_lower",
            "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried", "ore_lapis_buried");

    /** The vanilla placed feature (in the {@code minecraft} namespace) this vein replaces and whose biomes it follows. */
    public final String replaces;
    /** The default shape, before the balance config; {@link #shape()} is the one worldgen uses. */
    public final VeinShape shape;
    /** Where a kind's big veins are more common: under forests (Coal) or in dripstone caves (Iron). */
    public enum Favored { NONE, FOREST, DRIPSTONE }

    /** The biome where this kind's big veins are more common, and the default rarity there. */
    public final Favored favored;
    public final double favoredRarity;
    /** Default number of small ore pockets per chunk (the fraction is a chance of one more), before the config. */
    public final double pockets;
    /** Default largest pocket, in ore blocks; each pocket is 1 to this many. */
    public final int pocketSize;
    private VeinShape configured;

    VeinKind(String replaces, VeinShape shape, double pockets, int pocketSize, Favored favored, double favoredRarity) {
        this.replaces = replaces;
        this.shape = shape;
        this.pockets = pockets;
        this.pocketSize = pocketSize;
        this.favored = favored;
        this.favoredRarity = favoredRarity;
    }

    /** The rarity in the favored biome, from the balance config. */
    public double favoredRarity() {
        return BalanceConfig.get(key("favored_rarity"), favoredRarity);
    }

    /** Pockets per chunk from the balance config. */
    public double pockets() {
        return BalanceConfig.get(key("pockets"), pockets);
    }

    /** Largest pocket from the balance config, at least 1. */
    public int pocketSize() {
        return Math.max(1, BalanceConfig.get(key("pocket_size"), pocketSize));
    }

    /** The shape with the balance config's size, rarity and chances; heights stay vanilla's. */
    public VeinShape shape() {
        if (configured == null) {
            configured = new VeinShape(shape.minY(), shape.maxY(), BalanceConfig.get(key("size"), shape.size()),
                    BalanceConfig.get(key("rarity"), shape.rarity()), BalanceConfig.get(key("ore_chance"), shape.oreChance()),
                    BalanceConfig.get(key("filler_chance"), shape.fillerChance()), BalanceConfig.get(key("raw_chance"), shape.rawChance()),
                    Math.max(16, BalanceConfig.get(key("patch"), shape.patch())));
        }
        return configured;
    }

    /** This kind's noise for a world seed. Salted by kind, so different ores don't share a shape. */
    // The same world seed always gives the same veins, so a world regenerates identically.
    public VeinShape.Sampler sampler(long worldSeed) {
        return new VeinShape.Sampler(shape(), worldSeed ^ (replaces.hashCode() * 0x5DEECE66DL));
    }

    private String key(String field) {
        return "veins." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code veins} section of the balance config. */
    public static void writeDefaults(Map<String, Double> out) {
        for (VeinKind kind : values()) {
            out.put(kind.key("size"), kind.shape.size());
            out.put(kind.key("rarity"), kind.shape.rarity());
            out.put(kind.key("ore_chance"), kind.shape.oreChance());
            out.put(kind.key("filler_chance"), kind.shape.fillerChance());
            out.put(kind.key("raw_chance"), kind.shape.rawChance());
            out.put(kind.key("patch"), kind.shape.patch());
            out.put(kind.key("pockets"), kind.pockets);
            out.put(kind.key("pocket_size"), (double) kind.pocketSize);
            if (kind.favored != Favored.NONE) out.put(kind.key("favored_rarity"), kind.favoredRarity);
        }
    }
}
