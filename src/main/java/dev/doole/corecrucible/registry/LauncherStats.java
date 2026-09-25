package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Bow and crossbow stats per tier. The damage multiplier and armor pierce are the old tiered-arrow
 * table moved onto the launcher. Wood is the vanilla bow and crossbow, so its row changes nothing.
 */
public enum LauncherStats {
    // Default values. Change them here for every install, or in config/dooles_core_crucible.json under "launchers" for
    // one install. Damage × multiplies the arrow's damage; pierce is the share of it that ignores armor (0.4 = 40%);
    // draw ticks is how long a full draw takes (20 = 1 second, vanilla).
    // tier, damage ×, pierce, bow draw ticks, bow durability, crossbow durability
    WOOD(ModTiers.WOOD, 1.0, 0.0, 20, 384, 465),
    BONE(ModTiers.BONE, 1.1, 0.0, 19, 400, 480),
    FLINT(ModTiers.FLINT, 1.1, 0.0, 19, 400, 480),
    COPPER(ModTiers.COPPER, 1.2, 0.05, 18, 450, 500),
    IRON(ModTiers.IRON, 1.3, 0.10, 17, 600, 600),
    EMERALD(ModTiers.EMERALD, 1.4, 0.15, 16, 1050, 1050),
    DIAMOND(ModTiers.DIAMOND, 1.5, 0.20, 15, 1600, 1600),
    OBSIDIAN(ModTiers.OBSIDIAN, 1.6, 0.25, 15, 2400, 2400),
    NETHERITE(ModTiers.NETHERITE, 1.75, 0.30, 14, 3300, 3300),
    REINFORCED(ModTiers.REINFORCED, 2.0, 0.40, 13, 4500, 4500);

    /** Vanilla bow full-draw time and the crossbow charge time it's scaled against. */
    public static final int VANILLA_DRAW_TICKS = 20;

    private final ModTiers tier;
    private final double damageMultiplier;
    private final double armorPierce;
    private final int drawTicks;
    private final int bowDurability;
    private final int crossbowDurability;

    LauncherStats(ModTiers tier, double damageMultiplier, double armorPierce, int drawTicks, int bowDurability, int crossbowDurability) {
        this.tier = tier;
        this.damageMultiplier = damageMultiplier;
        this.armorPierce = armorPierce;
        this.drawTicks = drawTicks;
        this.bowDurability = bowDurability;
        this.crossbowDurability = crossbowDurability;
    }

    // The rows are in the same order as ModTiers, so a tier's position finds its row.
    public static LauncherStats of(ModTiers tier) {
        return values()[tier.ordinal()];
    }

    /** The stats of a chain bow or crossbow, or null for anything else. */
    public static LauncherStats of(ItemStack stack) {
        EquipType type = ModItems.typeOf(stack.getItem());
        return type != null && type.isLauncher() ? of(ModItems.tierOf(stack.getItem())) : null;
    }

    public ModTiers tier() {
        return tier;
    }

    public double damageMultiplier() {
        return BalanceConfig.get(key("arrow_damage"), damageMultiplier);
    }

    /** Share of arrow damage that ignores armor, 0 to 1. */
    public double armorPierce() {
        return BalanceConfig.get(key("armor_pierce"), armorPierce);
    }

    /** Ticks to a full bow draw; the crossbow charge scales by the same ratio. */
    public int drawTicks() {
        return Math.max(1, BalanceConfig.get(key("draw_ticks"), drawTicks));
    }

    public int durability(EquipType type) {
        return type == EquipType.CROSSBOW ? BalanceConfig.get(key("crossbow_durability"), crossbowDurability)
                : BalanceConfig.get(key("bow_durability"), bowDurability);
    }

    private String key(String field) {
        return "launchers." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code launchers} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (LauncherStats s : values()) {
            out.put(s.key("arrow_damage"), s.damageMultiplier);
            out.put(s.key("armor_pierce"), s.armorPierce);
            out.put(s.key("draw_ticks"), (double) s.drawTicks);
            out.put(s.key("bow_durability"), (double) s.bowDurability);
            out.put(s.key("crossbow_durability"), (double) s.crossbowDurability);
        }
    }

    /** Draw speed relative to vanilla, from the tier's draw time; 1 for non-tiered launchers. */
    public static double drawSpeed(LauncherStats stats) {
        return stats == null ? 1.0 : (double) VANILLA_DRAW_TICKS / stats.drawTicks();
    }

    public static double drawSpeed(ItemStack stack) {
        return drawSpeed(of(stack));
    }

    /** Ticks held, as the vanilla bow's power curve should see them. */
    public static int scaledHeldTicks(int held, double speed) {
        return (int) Math.floor(held * speed + 1e-6);
    }

    /** A vanilla crossbow charge time (Quick Charge included) at this speed; at least 1 tick. */
    public static int scaledChargeTicks(int vanilla, double speed) {
        return Math.max(1, (int) Math.round(vanilla / speed));
    }
}
