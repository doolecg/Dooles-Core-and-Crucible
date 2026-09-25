package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
//? if >=1.21.2 {
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//?} else {
/*import net.minecraft.world.item.EnchantedBookItem;
*///?}
//? if fabric && >=1.21.2 {
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//?} else if fabric {
/*import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
*///?}

/**
 * The mod's own creative tab (every mod item, then an enchanted book for each level of each mod enchantment), plus
 * where mod items slot into vanilla's Tools, Combat and Ingredients tabs. The loader entrypoints apply
 * {@link #vanillaPlacements} with their own tab-contents events.
 */
public final class ModCreativeTab {

    /** Put {@code item} right after {@code anchor} in a vanilla tab. */
    public record Placement(Item anchor, Item item) {
    }

    // Which vanilla tab each group of equipment goes in. Types in one group stay together per tier, like vanilla's
    // shovel/pickaxe/axe/hoe sets and helmet-to-boots armor sets.
    private static final EquipType[][] TOOL_GROUPS = {
            {EquipType.SHOVEL, EquipType.PICKAXE, EquipType.AXE, EquipType.HOE}, {EquipType.FISHING_ROD}, {EquipType.SHEARS}};
    private static final EquipType[][] COMBAT_GROUPS = {
            {EquipType.SWORD}, {EquipType.AXE}, {EquipType.SPEAR}, {EquipType.MACE}, {EquipType.SHIELD},
            {EquipType.HELMET, EquipType.CHESTPLATE, EquipType.LEGGINGS, EquipType.BOOTS},
            {EquipType.BOW}, {EquipType.CROSSBOW}, {EquipType.HORSE_ARMOR}, {EquipType.WOLF_ARMOR}};

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
                    // Books only in this tab: vanilla's search tab already lists every enchanted book.
                    for (ResourceKey<Enchantment> key : ModEnchantments.ALL) {
                        for (ItemStack book : enchantedBooks(params.holders(), key)) {
                            output.accept(book, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        }
                    }
                })
                .build();
        registrar.accept(CoreCrucible.id("main"), tab);
    }

    /** One enchanted book per level of {@code key}; empty when a datapack removed the enchantment. */
    public static List<ItemStack> enchantedBooks(HolderLookup.Provider holders, ResourceKey<Enchantment> key) {
        List<ItemStack> books = new ArrayList<>();
        Holder<Enchantment> enchantment = holders.lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
        if (enchantment == null) return books;
        for (int level = 1; level <= enchantment.value().getMaxLevel(); level++) {
            //? if >=1.21.2 {
            books.add(EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level)));
            //?} else {
            /*books.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, level)));
            *///?}
        }
        return books;
    }

    /**
     * Where mod items go in each vanilla tab, in insertion order: every anchor is a vanilla item or one placed earlier
     * in the list, so the tiers line up (Wood, Bone, Flint, Stone... Iron, Emerald, Gold, Diamond, Obsidian...).
     */
    public static Map<ResourceKey<CreativeModeTab>, List<Placement>> vanillaPlacements() {
        Map<ResourceKey<CreativeModeTab>, List<Placement>> out = new LinkedHashMap<>();
        List<Placement> tools = new ArrayList<>();
        for (EquipType[] group : TOOL_GROUPS) placeChain(tools, group);
        out.put(CreativeModeTabs.TOOLS_AND_UTILITIES, tools);

        List<Placement> combat = new ArrayList<>();
        for (EquipType[] group : COMBAT_GROUPS) placeChain(combat, group);
        placeAfter(combat, ModItems.equipment(ModTiers.REINFORCED, EquipType.SPEAR), ModItems.byName("golden_spear"));
        out.put(CreativeModeTabs.COMBAT, combat);

        List<Placement> ingredients = new ArrayList<>();
        Item anchor = Items.NETHERITE_INGOT;
        for (ModTiers tier : ModItems.ALLOY_TIERS) anchor = placeAfter(ingredients, anchor, ModItems.alloyIngot(tier));
        anchor = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
        for (ModTiers tier : ModItems.ALLOY_TIERS) {
            if (tier != ModTiers.NETHERITE) anchor = placeAfter(ingredients, anchor, ModItems.upgradeTemplate(tier));
        }
        placeAfter(ingredients, Items.IRON_NUGGET, ModItems.byName("copper_nugget"));
        out.put(CreativeModeTabs.INGREDIENTS, ingredients);
        return out;
    }

    // Walks the tiers in chain order. A vanilla item becomes the new anchor; a mod item goes right after the anchor and
    // becomes the anchor itself, so the next mod item follows it.
    private static void placeChain(List<Placement> out, EquipType[] group) {
        Item anchor = startAnchor(group[group.length - 1]);
        for (ModTiers tier : ModTiers.values()) {
            for (EquipType type : group) {
                Item item = ModItems.equipment(tier, type);
                if (item == null) continue;
                anchor = ModItems.isVanilla(tier, type) ? item : placeAfter(out, anchor, item);
            }
        }
    }

    /** Where a chain goes when its first tiers are all mod items (no vanilla item to follow yet). */
    private static Item startAnchor(EquipType lastOfGroup) {
        return switch (lastOfGroup) {
            case MACE -> Items.MACE;
            case SPEAR -> Items.TRIDENT;
            case SHEARS -> Items.SHEARS;
            case BOOTS -> Items.CHAINMAIL_BOOTS;
            case HORSE_ARMOR -> Items.LEATHER_HORSE_ARMOR;
            default -> null;
        };
    }

    private static Item placeAfter(List<Placement> out, Item anchor, Item item) {
        if (item == null) return anchor;
        out.add(new Placement(anchor, item));
        return item;
    }
}
