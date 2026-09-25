package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.Locale;
import java.util.Map;

/**
 * Horse and wolf armor per tier. Horse armor has no durability, only armor points; wolf armor keeps
 * vanilla's 11 armor points (enough to soak every hit while it lasts) and gets more durability per tier. Copper wolf
 * armor is the vanilla one; Copper, Iron, Diamond and Netherite horse armor are vanilla on 26.x, so only the mod
 * items read these numbers.
 */
public enum BodyArmorStats {
    // Default values. Change them here for every install, or in config/dooles_core_crucible.json under "body_armor" for
    // one install. Only rows for items the mod adds show up in the config (vanilla's horse armor keeps vanilla stats).
    // tier, horse armor defense, wolf armor durability
    COPPER(ModTiers.COPPER, 4, 64),
    IRON(ModTiers.IRON, 5, 128),
    EMERALD(ModTiers.EMERALD, 8, 192),
    DIAMOND(ModTiers.DIAMOND, 11, 256),
    OBSIDIAN(ModTiers.OBSIDIAN, 14, 352),
    NETHERITE(ModTiers.NETHERITE, 19, 448),
    REINFORCED(ModTiers.REINFORCED, 23, 576);

    /** Vanilla wolf armor's armor points, kept by every tier. */
    public static final int WOLF_DEFENSE = 11;

    private final ModTiers tier;
    private final int horseDefense;
    private final int wolfDurability;

    BodyArmorStats(ModTiers tier, int horseDefense, int wolfDurability) {
        this.tier = tier;
        this.horseDefense = horseDefense;
        this.wolfDurability = wolfDurability;
    }

    /** The row for a body-armor tier, or null below Copper. */
    public static BodyArmorStats of(ModTiers tier) {
        for (BodyArmorStats stats : values()) {
            if (stats.tier == tier) return stats;
        }
        return null;
    }

    public ModTiers tier() {
        return tier;
    }

    public int horseDefense() {
        return BalanceConfig.get(key("horse_defense"), horseDefense);
    }

    public int wolfDurability() {
        return BalanceConfig.get(key("wolf_durability"), wolfDurability);
    }

    private String key(String field) {
        return "body_armor." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code body_armor} section of the balance config ({@code config/dooles_core_crucible.json}): only the rows a mod item reads. */
    public static void writeDefaults(Map<String, Double> out) {
        for (BodyArmorStats s : values()) {
            if (!ModItems.isVanilla(s.tier, EquipType.HORSE_ARMOR)) out.put(s.key("horse_defense"), (double) s.horseDefense);
            if (!ModItems.isVanilla(s.tier, EquipType.WOLF_ARMOR)) out.put(s.key("wolf_durability"), (double) s.wolfDurability);
        }
    }
}
