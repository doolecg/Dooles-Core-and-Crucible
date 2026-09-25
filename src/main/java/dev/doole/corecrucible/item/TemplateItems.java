package dev.doole.corecrucible.item;

import dev.doole.corecrucible.CoreCrucible;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * Builds the mod's {@link SmithingTemplateItem}s: the vanilla "Applies to / Ingredients" tooltip
 * and the empty-slot icons in the smithing screen. Text comes from
 * {@code item.dooles_core_crucible.smithing_template.<name>.*} lang keys.
 */
public final class TemplateItems {

    // Empty-slot icons the smithing table cycles through in the base slot, as a hint of what can go there.
    private static final List<String> GEAR_ICONS = List.of("helmet", "sword", "chestplate", "pickaxe", "leggings", "axe", "boots", "shovel", "hoe");

    private TemplateItems() {
    }

    /** An upgrade template: any chain piece or the previous ingot in the base slot, an ingot in the addition slot. */
    public static Item upgrade(String name, Item.Properties props) {
        List<String> base = new ArrayList<>(GEAR_ICONS);
        //? if >=1.21.2 {
        base.add("spear");
        //?}
        base.add("ingot");
        return create(name, base, List.of("ingot"), props);
    }

    // Builds the template with its tooltip lines and slot icons. 1.21.1's constructor also takes the title line.
    private static Item create(String name, List<String> baseIcons, List<String> additionIcons, Item.Properties props) {
        String key = "item." + CoreCrucible.MOD_ID + ".smithing_template." + name + ".";
        Component appliesTo = Component.translatable(key + "applies_to").withStyle(ChatFormatting.BLUE);
        Component ingredients = Component.translatable(key + "ingredients").withStyle(ChatFormatting.BLUE);
        Component baseSlot = Component.translatable(key + "base_slot_description");
        Component additionSlot = Component.translatable(key + "additions_slot_description");
        List<Identifier> baseIds = icons(baseIcons);
        List<Identifier> additionIds = icons(additionIcons);
        //? if >=1.21.2 {
        return new SmithingTemplateItem(appliesTo, ingredients, baseSlot, additionSlot, baseIds, additionIds, props);
        //?} else {
        /*Component title = Component.translatable("item.minecraft.smithing_template").withStyle(ChatFormatting.GRAY);
        return new SmithingTemplateItem(appliesTo, ingredients, title, baseSlot, additionSlot, baseIds, additionIds);
        *///?}
    }

    /** The vanilla empty-slot sprite id for each name in {@code names}, e.g. {@code pickaxe} or {@code ingot}. */
    private static List<Identifier> icons(List<String> names) {
        List<Identifier> out = new ArrayList<>();
        for (String name : names) out.add(icon(name));
        return out;
    }

    /** The vanilla empty-slot sprite for {@code name}, e.g. {@code pickaxe} or {@code ingot}. */
    private static Identifier icon(String name) {
        //? if >=1.21.2 {
        return Identifier.withDefaultNamespace("container/slot/" + name);
        //?} else {
        /*boolean armor = switch (name) {
            case "helmet", "chestplate", "leggings", "boots" -> true;
            default -> false;
        };
        return Identifier.withDefaultNamespace(armor ? "item/empty_armor_slot_" + name : "item/empty_slot_" + name);
        *///?}
    }
}
