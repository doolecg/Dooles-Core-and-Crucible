package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Tool;

/**
 * Bone/Flint and Copper shears stats. Iron is the vanilla {@code minecraft:shears}, so it keeps
 * vanilla's own durability and mining speed and has no row here.
 */
public enum ShearsStats {
    // Default values. Change them here for every install, or in config/dooles_core_crucible.json under "shears" for one
    // install. Mining speed × is compared with vanilla's shears (0.6 = 60% as fast).
    // tier, durability, mining speed ×
    BONE(ModTiers.BONE, 120, 0.6),
    FLINT(ModTiers.FLINT, 120, 0.6),
    COPPER(ModTiers.COPPER, 180, 0.8);

    private final ModTiers tier;
    private final int durability;
    private final double speedMultiplier;

    ShearsStats(ModTiers tier, int durability, double speedMultiplier) {
        this.tier = tier;
        this.durability = durability;
        this.speedMultiplier = speedMultiplier;
    }

    /** The stats for a chain-shears tier, or null for Iron (vanilla) and anything else. */
    public static ShearsStats of(ModTiers tier) {
        for (ShearsStats stats : values()) {
            if (stats.tier == tier) return stats;
        }
        return null;
    }

    public ModTiers tier() {
        return tier;
    }

    public int durability() {
        return BalanceConfig.get(key("durability"), durability);
    }

    public double speedMultiplier() {
        return BalanceConfig.get(key("speed"), speedMultiplier);
    }

    private String key(String field) {
        return "shears." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code shears} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (ShearsStats s : values()) {
            out.put(s.key("durability"), (double) s.durability);
            out.put(s.key("speed"), s.speedMultiplier);
        }
    }

    /**
     * Vanilla's shears {@code Tool} component, with every rule's mining speed (and the default speed) scaled by
     * {@code speedMultiplier}. Starting from vanilla's own {@code createToolProperties()} instead of rebuilding the
     * rule list means whatever vanilla lets shears cut (cobwebs, leaves, wool, vines...) is inherited for free, on
     * every version, without us having to copy each version's rule list by hand.
     */
    public Tool scaledTool() {
        Tool base = ShearsItem.createToolProperties();
        List<Tool.Rule> rules = new ArrayList<>();
        for (Tool.Rule rule : base.rules()) {
            rules.add(new Tool.Rule(rule.blocks(), rule.speed().map(speed -> (float) (speed * speedMultiplier())), rule.correctForDrops()));
        }
        //? if >=1.21.2 {
        return new Tool(rules, (float) (base.defaultMiningSpeed() * speedMultiplier()), base.damagePerBlock(), base.canDestroyBlocksInCreative());
        //?} else {
        /*return new Tool(rules, (float) (base.defaultMiningSpeed() * speedMultiplier()), base.damagePerBlock());
        *///?}
    }
}
