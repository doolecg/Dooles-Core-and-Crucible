package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Shield stats per tier. Wood is the vanilla shield, so its row matches vanilla: raised after 5 ticks,
 * disabled for 5 seconds by an axe, no knockback resistance and no side cover.
 */
public enum ShieldStats {
    // Default values. Change them here for every install, or in config/dooles_core_crucible.json under "shields" for one
    // install. Knockback resistance and side cover are shares from 0 to 1 (0.5 = half).
    // tier, durability, raise ticks, axe disable seconds, knockback resistance while blocking, side cover
    WOOD(ModTiers.WOOD, 336, 5, 5.0, 0.0, 0.0),
    BONE(ModTiers.BONE, 360, 5, 4.5, 0.0, 0.0),
    FLINT(ModTiers.FLINT, 360, 5, 4.5, 0.0, 0.0),
    COPPER(ModTiers.COPPER, 420, 5, 4.0, 0.0, 0.0),
    IRON(ModTiers.IRON, 600, 4, 3.5, 0.1, 0.0),
    EMERALD(ModTiers.EMERALD, 1050, 4, 3.0, 0.2, 0.0),
    DIAMOND(ModTiers.DIAMOND, 1600, 3, 2.5, 0.3, 0.25),
    OBSIDIAN(ModTiers.OBSIDIAN, 2400, 3, 2.0, 0.4, 0.35),
    NETHERITE(ModTiers.NETHERITE, 3300, 2, 1.5, 0.5, 0.5),
    REINFORCED(ModTiers.REINFORCED, 4500, 1, 1.0, 0.6, 0.6);

    /** Vanilla's raise delay (ticks) and axe disable time (seconds), which the tiers are scaled against. */
    public static final int VANILLA_RAISE_TICKS = 5;
    public static final double VANILLA_DISABLE_SECONDS = 5.0;

    private final ModTiers tier;
    private final int durability;
    private final int raiseTicks;
    private final double disableSeconds;
    private final double knockbackResistance;
    private final double sideCover;

    ShieldStats(ModTiers tier, int durability, int raiseTicks, double disableSeconds, double knockbackResistance, double sideCover) {
        this.tier = tier;
        this.durability = durability;
        this.raiseTicks = raiseTicks;
        this.disableSeconds = disableSeconds;
        this.knockbackResistance = knockbackResistance;
        this.sideCover = sideCover;
    }

    // The rows are in the same order as ModTiers, so a tier's position finds its row.
    public static ShieldStats of(ModTiers tier) {
        return values()[tier.ordinal()];
    }

    /** The stats of a chain shield (the vanilla one included), or null for anything else. */
    public static ShieldStats of(ItemStack stack) {
        EquipType type = ModItems.typeOf(stack.getItem());
        return type == EquipType.SHIELD ? of(ModItems.tierOf(stack.getItem())) : null;
    }

    public ModTiers tier() {
        return tier;
    }

    public int durability() {
        return BalanceConfig.get(key("durability"), durability);
    }

    /** Ticks from raising the shield until it blocks. */
    public int raiseTicks() {
        return Math.max(0, BalanceConfig.get(key("raise_ticks"), raiseTicks));
    }

    /** Seconds an axe hit puts the shield on cooldown for. */
    public double disableSeconds() {
        return BalanceConfig.get(key("axe_disable_seconds"), disableSeconds);
    }

    /** Knockback resistance added while blocking, 0 to 1. */
    public double knockbackResistance() {
        return Math.min(1.0, BalanceConfig.get(key("knockback_resistance"), knockbackResistance));
    }

    /** Share of the damage a raised shield stops from a hit on the side (between 90° and 135° off the front). */
    public double sideCover() {
        return Math.min(1.0, BalanceConfig.get(key("side_cover"), sideCover));
    }

    private String key(String field) {
        return "shields." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code shields} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (ShieldStats s : values()) {
            out.put(s.key("durability"), (double) s.durability);
            out.put(s.key("raise_ticks"), (double) s.raiseTicks);
            out.put(s.key("axe_disable_seconds"), s.disableSeconds);
            out.put(s.key("knockback_resistance"), s.knockbackResistance);
            out.put(s.key("side_cover"), s.sideCover);
        }
    }
}
