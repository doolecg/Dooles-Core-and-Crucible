package dev.doole.corecrucible.compat;

import dev.doole.corecrucible.gameplay.Catalysts;
import dev.doole.corecrucible.gameplay.ProgressionGraph;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Recipe views for recipe viewers, built from the same tables as the recipe data rather than from the
 * recipe manager, so they also work on 26.x clients (which don't receive the server's recipes).
 */
public final class ForgingDisplays {

    public enum Kind { GEAR, ALLOY }

    /** A smithing-table craft. {@code catalyst} is empty when none is needed. */
    public record Smithing(Kind kind, List<ItemStack> template, List<ItemStack> base, List<ItemStack> addition,
                           ItemStack catalyst, List<ItemStack> result) {
    }

    /** Alloys: tier → base ingot, raw addition. Mirrors {@code ALLOYS} in tools/gen_data.py. */
    // Each row: the alloy's tier, the ingot that goes in the base slot, and the raw material in the addition slot.
    // If you change an alloy recipe in gen_data.py, change it here too so JEI shows the same thing.
    private static final Object[][] ALLOYS = {
            {ModTiers.IRON, "minecraft:copper_ingot", "minecraft:iron_ingot"},
            {ModTiers.EMERALD, "dooles_core_crucible:iron_alloy_ingot", "minecraft:emerald"},
            {ModTiers.DIAMOND, "dooles_core_crucible:emerald_alloy_ingot", "minecraft:diamond"},
            {ModTiers.OBSIDIAN, "dooles_core_crucible:diamond_alloy_ingot", "minecraft:crying_obsidian"},
            {ModTiers.NETHERITE, "dooles_core_crucible:obsidian_alloy_ingot", "minecraft:netherite_ingot"},
            {ModTiers.REINFORCED, "dooles_core_crucible:netherite_alloy_ingot", "dooles_core_crucible:obsidian_alloy_ingot"},
    };

    private ForgingDisplays() {
    }

    // Every recipe JEI should list: all gear upgrades, then all alloys.
    public static List<Smithing> smithing() {
        List<Smithing> out = new ArrayList<>();
        // Gear upgrades, Copper → Iron and up.
        for (ModTiers from : ModTiers.values()) {
            for (EquipType type : EquipType.IN_ORDER) {
                for (ProgressionGraph.Step step : ProgressionGraph.next(from, type)) {
                    Item base = ModItems.equipment(from, type);
                    Item result = ModItems.equipment(step.to(), type);
                    if (base == null || result == null) continue;
                    out.add(new Smithing(Kind.GEAR, one(ModItems.upgradeTemplate(step.to())), one(base), one(ModItems.alloyIngot(step.to())),
                            stack(Catalysts.forUpgrade(step.to())), one(result)));
                }
            }
        }
        // Alloys.
        for (Object[] alloy : ALLOYS) {
            ModTiers tier = (ModTiers) alloy[0];
            out.add(new Smithing(Kind.ALLOY, one(ModItems.upgradeTemplate(tier)), one(item((String) alloy[1])), one(item((String) alloy[2])),
                    stack(Catalysts.forAlloy(tier)), one(ModItems.alloyIngot(tier))));
        }
        return out;
    }

    private static List<ItemStack> one(Item item) {
        return List.of(item == null ? ItemStack.EMPTY : new ItemStack(item));
    }

    private static ItemStack stack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item item(String id) {
        //? if >=1.21.2 {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        //?} else {
        /*return BuiltInRegistries.ITEM.get(Identifier.parse(id));
        *///?}
    }
}
