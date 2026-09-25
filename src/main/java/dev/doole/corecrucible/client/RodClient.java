package dev.doole.corecrucible.client;

//? if <1.21.2 {
/*import dev.doole.corecrucible.mixin.client.ItemPropertiesInvoker;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
*///?}

/** Client support for the tiered fishing rods. */
public final class RodClient {

    private RodClient() {
    }

    /** 1.21.1 item-model property for the "cast" pose. 26.x reads minecraft:fishing_rod/cast from the item definitions. */
    public static void registerItemProperties() {
        //? if <1.21.2 {
        /*for (ModTiers tier : ModTiers.values()) {
            Item rod = ModItems.isVanilla(tier, EquipType.FISHING_ROD) ? null : ModItems.equipment(tier, EquipType.FISHING_ROD);
            if (rod == null) continue;
            // Copied from vanilla's own Items.FISHING_ROD "cast" property lambda, registered against our rod too.
            ItemPropertiesInvoker.dooles_core_crucible$register(rod, Identifier.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
                if (entity == null) return 0.0F;
                boolean mainHand = entity.getMainHandItem() == stack;
                boolean offHand = entity.getOffhandItem() == stack;
                if (entity.getMainHandItem().getItem() instanceof FishingRodItem) offHand = false;
                return (mainHand || offHand) && entity instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
            });
        }
        *///?}
    }
}
