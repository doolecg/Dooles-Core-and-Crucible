package dev.doole.corecrucible.client;

import dev.doole.corecrucible.item.TieredBowItem;
import dev.doole.corecrucible.item.TieredCrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.21.2 {
/*import dev.doole.corecrucible.mixin.client.ItemPropertiesInvoker;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.LauncherStats;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.component.ChargedProjectiles;
*///?}

/** Client support for the tiered bows and crossbows. */
public final class LauncherClient {

    private LauncherClient() {
    }

    /**
     * Widens vanilla's {@code is(Items.BOW)} / {@code is(Items.CROSSBOW)} render checks (first-person hands, the
     * charged-crossbow arm pose, the bow zoom) to the tiered launchers.
     */
    public static boolean isLauncher(ItemStack stack, Object vanilla, boolean original) {
        if (original) return true;
        if (vanilla == Items.BOW) return stack.getItem() instanceof TieredBowItem;
        if (vanilla == Items.CROSSBOW) return stack.getItem() instanceof TieredCrossbowItem;
        return false;
    }

    /** 1.21.1 item-model properties. 26.x reads use_duration, crossbow/pull and charge_type from the item definitions. */
    public static void registerItemProperties() {
        //? if <1.21.2 {
        /*for (ModTiers tier : ModTiers.values()) {
            Item bow = ModItems.isVanilla(tier, EquipType.BOW) ? null : ModItems.equipment(tier, EquipType.BOW);
            if (bow != null) {
                // Pull runs 0 to 1 over the tier's draw time, as vanilla's does over 20 ticks.
                register(bow, "pull", (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F
                        : (float) ((stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) * LauncherStats.drawSpeed(stack) / 20.0));
                register(bow, "pulling", (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
            Item crossbow = ModItems.isVanilla(tier, EquipType.CROSSBOW) ? null : ModItems.equipment(tier, EquipType.CROSSBOW);
            if (crossbow != null) {
                register(crossbow, "pull", (stack, level, entity, seed) -> entity == null || CrossbowItem.isCharged(stack) ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / CrossbowItem.getChargeDuration(stack, entity));
                register(crossbow, "pulling", (stack, level, entity, seed) -> entity != null && entity.isUsingItem()
                        && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
                register(crossbow, "charged", (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
                register(crossbow, "firework", (stack, level, entity, seed) -> {
                    ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
                    return charged != null && charged.contains(Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
                });
            }
        }
        *///?}
    }

    //? if <1.21.2 {
    /*private static void register(Item item, String name, ClampedItemPropertyFunction function) {
        ItemPropertiesInvoker.dooles_core_crucible$register(item, Identifier.withDefaultNamespace(name), function);
    }
    *///?}
}
