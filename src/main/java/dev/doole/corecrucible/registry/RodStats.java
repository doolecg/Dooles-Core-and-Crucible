package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.config.BalanceConfig;
import java.util.Locale;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/**
 * Fishing rod stats per tier. Wood is the vanilla fishing rod, so its row changes nothing:
 * no extra lure or luck, and reel/cast multipliers of 1.0.
 */
public enum RodStats {
    // Default values. Change them here for every install, or in config/dooles_core_crucible.json under "rods" for one
    // install. Lure and luck work like extra levels of the Lure and Luck of the Sea enchantments.
    // tier, durability, lure +, luck +, reel ×, cast ×
    WOOD(ModTiers.WOOD, 64, 0, 0, 1.0, 1.0),
    BONE(ModTiers.BONE, 96, 0, 0, 1.05, 1.05),
    FLINT(ModTiers.FLINT, 96, 0, 0, 1.05, 1.05),
    COPPER(ModTiers.COPPER, 128, 0, 0, 1.1, 1.1),
    IRON(ModTiers.IRON, 192, 1, 0, 1.15, 1.15),
    EMERALD(ModTiers.EMERALD, 256, 1, 1, 1.2, 1.2),
    DIAMOND(ModTiers.DIAMOND, 320, 1, 1, 1.3, 1.25),
    OBSIDIAN(ModTiers.OBSIDIAN, 384, 2, 1, 1.4, 1.3),
    NETHERITE(ModTiers.NETHERITE, 448, 2, 2, 1.5, 1.35),
    REINFORCED(ModTiers.REINFORCED, 512, 3, 2, 1.6, 1.4);

    private final ModTiers tier;
    private final int durability;
    private final int lure;
    private final int luck;
    private final double reel;
    private final double cast;

    RodStats(ModTiers tier, int durability, int lure, int luck, double reel, double cast) {
        this.tier = tier;
        this.durability = durability;
        this.lure = lure;
        this.luck = luck;
        this.reel = reel;
        this.cast = cast;
    }

    // The rows are in the same order as ModTiers, so a tier's position finds its row.
    public static RodStats of(ModTiers tier) {
        return values()[tier.ordinal()];
    }

    /** The stats of a chain fishing rod, or null for anything else. */
    public static RodStats of(ItemStack stack) {
        EquipType type = ModItems.typeOf(stack.getItem());
        return type != null && type.isFishingRod() ? of(ModItems.tierOf(stack.getItem())) : null;
    }

    public ModTiers tier() {
        return tier;
    }

    public int durability() {
        return BalanceConfig.get(key("durability"), durability);
    }

    /** Extra Lure levels' worth of bite-wait reduction; each is worth 100 ticks (see FishingHookMixin). */
    public int lure() {
        return BalanceConfig.get(key("lure"), lure);
    }

    /** Flat bonus added to the fishing hook's luck (better loot table rolls). */
    public int luck() {
        return BalanceConfig.get(key("luck"), luck);
    }

    /** Multiplies the pull strength when reeling in a hooked entity. */
    public double reel() {
        return BalanceConfig.get(key("reel"), reel);
    }

    /** Multiplies the hook's initial flight distance when cast. */
    public double cast() {
        return BalanceConfig.get(key("cast"), cast);
    }

    private String key(String field) {
        return "rods." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code rods} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (RodStats s : values()) {
            out.put(s.key("durability"), (double) s.durability);
            out.put(s.key("lure"), (double) s.lure);
            out.put(s.key("luck"), (double) s.luck);
            out.put(s.key("reel"), s.reel);
            out.put(s.key("cast"), s.cast);
        }
    }
}
