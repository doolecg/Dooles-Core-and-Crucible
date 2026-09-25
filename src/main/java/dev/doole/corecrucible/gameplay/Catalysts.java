package dev.doole.corecrucible.gameplay;

import dev.doole.corecrucible.recipe.AlloySmithingRecipe;
import dev.doole.corecrucible.recipe.ProgressionSmithingRecipe;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/**
 * The smithing-table catalyst slot: Obsidian, Netherite and Reinforced gear upgrades and the Reinforced
 * alloy need one catalyst, which the craft uses up. The slot shows while the base item is heading
 * for one of those.
 */
public final class Catalysts {

    private Catalysts() {
    }

    /** Catalyst for smithing gear up to {@code to}, or null when that upgrade needs none. */
    // To change which item a tier needs as a catalyst, edit this switch and isCatalyst below. The smithing table, tooltip,
    // JEI and recipe book all read it from here.
    public static Item forUpgrade(ModTiers to) {
        return switch (to) {
            case OBSIDIAN -> Items.GHAST_TEAR;
            case NETHERITE -> Items.WITHER_SKELETON_SKULL;
            case REINFORCED -> Items.NETHER_STAR;
            default -> null;
        };
    }

    /** Catalyst for alloying a {@code tier} alloy ingot, or null. */
    public static Item forAlloy(ModTiers tier) {
        return tier == ModTiers.REINFORCED ? Items.NETHER_STAR : null;
    }

    /** Catalyst a smithing recipe needs, or null. */
    public static Item required(Recipe<?> recipe) {
        if (recipe instanceof ProgressionSmithingRecipe progression) return forUpgrade(progression.tier());
        if (recipe instanceof AlloySmithingRecipe alloy) return forAlloy(alloy.tier());
        return null;
    }

    /** Whether the catalyst slot should show for this base item. */
    public static boolean wanted(ItemStack base) {
        if (base.isEmpty()) return false;
        // A piece of gear whose next tier needs a catalyst...
        ModTiers tier = ModItems.tierOf(base.getItem());
        if (tier != null && tier.ordinal() + 1 < ModTiers.values().length && forUpgrade(ModTiers.values()[tier.ordinal() + 1]) != null) {
            return true;
        }
        // ...or the ingot one tier below an alloy that needs a catalyst (e.g. Netherite alloy -> Reinforced alloy).
        for (ModTiers alloy : ModItems.ALLOY_TIERS) {
            if (forAlloy(alloy) != null && alloy.ordinal() > 0 && base.is(ModItems.alloyIngot(ModTiers.values()[alloy.ordinal() - 1]))) return true;
        }
        return false;
    }

    // Items allowed in the catalyst slot at all. Keep in step with forUpgrade/forAlloy.
    public static boolean isCatalyst(ItemStack stack) {
        return stack.is(Items.GHAST_TEAR) || stack.is(Items.WITHER_SKELETON_SKULL) || stack.is(Items.NETHER_STAR);
    }
}
