package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if fabric && >=1.21.2 {
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//?} else if fabric {
/*import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
*///?}

// Builds the mod's own tab in the creative-mode inventory, holding every item the mod adds.
public final class ModCreativeTab {

    private ModCreativeTab() {
    }

    /** Called by the loader-specific registration hook, after items are registered. */
    public static void register(BiConsumer<Identifier, CreativeModeTab> registrar) {
        //? if fabric && >=1.21.2 {
        CreativeModeTab.Builder builder = FabricCreativeModeTab.builder();
        //?} else if fabric {
        /*CreativeModeTab.Builder builder = FabricItemGroup.builder();
        *///?} else {
        /*CreativeModeTab.Builder builder = CreativeModeTab.builder();
        *///?}
        CreativeModeTab tab = builder
                .title(Component.translatable("itemGroup." + CoreCrucible.MOD_ID))
                // The tab's icon is the Reinforced pickaxe; it lists every mod item in registration order.
                .icon(() -> new ItemStack(ModItems.equipment(ModTiers.REINFORCED, EquipType.PICKAXE)))
                .displayItems((params, output) -> {
                    for (Item item : ModItems.all()) {
                        output.accept(item);
                    }
                })
                .build();
        registrar.accept(CoreCrucible.id("main"), tab);
    }
}
