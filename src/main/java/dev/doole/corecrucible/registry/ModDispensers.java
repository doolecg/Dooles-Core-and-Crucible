package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.item.TieredShearsItem;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;
//? if <1.21.2 {
/*import dev.doole.corecrucible.item.TieredHorseArmorItem;
import net.minecraft.world.item.Items;
*///?}

/**
 * Dispenser behaviour for chain items that vanilla registers per item. Tiered shears shear like vanilla ones (sheep,
 * beehives...): vanilla registers its own {@code ShearsDispenseItemBehavior} for Items.SHEARS, and we register the
 * same for ours. On 1.21.1 only vanilla's four horse armors put themselves on a horse, so tiered horse armor reuses
 * that behaviour too (26.x equips any horse armor through its equippable component).
 */
public final class ModDispensers {

    private ModDispensers() {
    }

    public static void register() {
        for (ModTiers tier : ModTiers.values()) {
            var item = ModItems.equipment(tier, EquipType.SHEARS);
            if (item instanceof TieredShearsItem) DispenserBlock.registerBehavior(item, new ShearsDispenseItemBehavior());
            //? if <1.21.2 {
            /*var horseArmor = ModItems.equipment(tier, EquipType.HORSE_ARMOR);
            if (horseArmor instanceof TieredHorseArmorItem) {
                DispenserBlock.registerBehavior(horseArmor, DispenserBlock.DISPENSER_REGISTRY.get(Items.IRON_HORSE_ARMOR));
            }
            *///?}
        }
    }
}
