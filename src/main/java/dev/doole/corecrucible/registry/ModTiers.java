package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.config.BalanceConfig;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
//? if >=1.21.2 {
import net.minecraft.world.item.ToolMaterial;
//?} else {
/*import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
*///?}

/**
 * The tool tiers, Wood to Reinforced, with their default stats. Players can change them in
 * {@code config/dooles_core_crucible.json} under {@code tiers}. Stats here drive both the mod's own items and the
 * overrides applied to the vanilla items that join the chain.
 */
public enum ModTiers {
    // Each line below is one tier, in chain order (the order matters: the next tier up is the next line).
    //   prefix: start of the item ids (wooden_pickaxe, emerald_sword...)
    //   harvest level: which ores it can mine (0 wood, 1 stone, 2 iron, 3 diamond, 4+ above); the incorrect-for tag
    //     is the list of blocks it CAN'T mine, which is what Minecraft actually checks
    //   durability / speed / attack bonus / enchantability: the defaults; the config file can override these four
    //   axe damage / axe speed: the axe's base attack damage and speed (the attack bonus is added on top)
    //   spear tuning: the nine numbers vanilla's Item.Properties#spear takes on 26.x; null where vanilla already has
    //     that spear (wood, copper, iron, diamond, netherite)
    // prefix, harvest level, durability, speed, attack bonus, enchantability, axe damage, axe speed, incorrect-for tag, 26.x spear tuning
    WOOD("wooden", 0, 60, 2.0F, 0.0F, 15, 6.0F, -3.2F, vanillaTag("incorrect_for_wooden_tool"),
            null),
    BONE("bone", 1, 130, 4.0F, 1.0F, 15, 7.0F, -3.2F, vanillaTag("incorrect_for_stone_tool"),
            new float[]{0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F}),
    FLINT("flint", 1, 170, 4.5F, 1.0F, 5, 7.0F, -3.2F, vanillaTag("incorrect_for_stone_tool"),
            new float[]{0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F}),
    COPPER("copper", 1, 300, 5.0F, 1.0F, 13, 7.0F, -3.2F, vanillaTag("incorrect_for_stone_tool"),
            null),
    IRON("iron", 2, 600, 6.0F, 2.0F, 14, 6.0F, -3.1F, vanillaTag("incorrect_for_iron_tool"),
            null),
    EMERALD("emerald", 3, 1050, 7.5F, 2.5F, 18, 5.5F, -3.05F, vanillaTag("incorrect_for_diamond_tool"),
            new float[]{1.0F, 1.01F, 0.55F, 2.75F, 10.5F, 6.6F, 5.1F, 10.6F, 4.6F}),
    DIAMOND("diamond", 3, 1600, 8.0F, 3.0F, 10, 5.0F, -3.0F, vanillaTag("incorrect_for_diamond_tool"),
            null),
    OBSIDIAN("obsidian", 4, 2400, 8.5F, 3.5F, 10, 5.0F, -3.0F, modTag("incorrect_for_obsidian_tool"),
            new float[]{1.1F, 1.14F, 0.45F, 2.75F, 9.5F, 6.0F, 5.1F, 9.4F, 4.6F}),
    NETHERITE("netherite", 5, 3300, 9.0F, 4.0F, 15, 5.0F, -3.0F, vanillaTag("incorrect_for_netherite_tool"),
            null),
    REINFORCED("reinforced", 6, 4500, 10.0F, 5.0F, 15, 5.0F, -3.0F, modTag("incorrect_for_reinforced_tool"),
            new float[]{1.2F, 1.3F, 0.35F, 2.25F, 8.5F, 5.0F, 5.1F, 8.0F, 4.6F});

    private final String prefix;
    private final int harvestLevel;
    private final int durability;
    private final float speed;
    private final float attackBonus;
    private final int enchantmentValue;
    private final float axeDamage;
    private final float axeSpeed;
    private final TagKey<Block> incorrectBlocks;
    private final TagKey<Item> repairItems;
    private final float[] spearParams;

    ModTiers(String prefix, int harvestLevel, int durability, float speed, float attackBonus, int enchantmentValue,
             float axeDamage, float axeSpeed, TagKey<Block> incorrectBlocks, float[] spearParams) {
        this.prefix = prefix;
        this.harvestLevel = harvestLevel;
        this.durability = durability;
        this.speed = speed;
        this.attackBonus = attackBonus;
        this.enchantmentValue = enchantmentValue;
        this.axeDamage = axeDamage;
        this.axeSpeed = axeSpeed;
        this.incorrectBlocks = incorrectBlocks;
        // Repair material tag, e.g. data/dooles_core_crucible/tags/item/repair/emerald.json (written by gen_data.py).
        this.repairItems = TagKey.create(Registries.ITEM, CoreCrucible.id("repair/" + name().toLowerCase(Locale.ROOT)));
        this.spearParams = spearParams;
    }

    private static TagKey<Block> vanillaTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }

    private static TagKey<Block> modTag(String path) {
        return TagKey.create(Registries.BLOCK, CoreCrucible.id(path));
    }

    /** Item id prefix, e.g. {@code wooden} or {@code emerald}. */
    public String prefix() {
        return prefix;
    }

    public int harvestLevel() {
        return harvestLevel;
    }

    // These four getters return the config file's value if the player set one, else the default above.
    public int durability() {
        return BalanceConfig.get(key("durability"), durability);
    }

    public float speed() {
        return BalanceConfig.get(key("mining_speed"), speed);
    }

    public float attackBonus() {
        return BalanceConfig.get(key("attack_bonus"), attackBonus);
    }

    public int enchantmentValue() {
        return BalanceConfig.get(key("enchantability"), enchantmentValue);
    }

    /** This tier's config key, {@code tiers.<tier>.<field>}. */
    private String key(String field) {
        return "tiers." + name().toLowerCase(Locale.ROOT) + "." + field;
    }

    /** The {@code tiers} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (ModTiers tier : values()) {
            out.put(tier.key("durability"), (double) tier.durability);
            out.put(tier.key("mining_speed"), (double) tier.speed);
            out.put(tier.key("attack_bonus"), (double) tier.attackBonus);
            out.put(tier.key("enchantability"), (double) tier.enchantmentValue);
        }
    }

    public float axeDamage() {
        return axeDamage;
    }

    public float axeSpeed() {
        return axeSpeed;
    }

    /** Hoe attack speed: vanilla's for the vanilla tiers, filled in between for the mod tiers. */
    public float hoeSpeed() {
        return switch (this) {
            case WOOD -> -3.0F;
            case BONE, FLINT, COPPER -> -2.0F;
            case IRON -> -1.0F;
            case EMERALD -> -0.5F;
            default -> 0.0F;
        };
    }

    public TagKey<Block> incorrectBlocks() {
        return incorrectBlocks;
    }

    /** {@code dooles_core_crucible:repair/<tier>}, shared by the tier's tools and armor. */
    public TagKey<Item> repairItems() {
        return repairItems;
    }

    /** Vanilla spear tuning on 26.x (see {@code Item.Properties#spear}); null where vanilla supplies the spear. */
    public float[] spearParams() {
        return spearParams;
    }

    /** Wood swords and spears get +0.5 on top of the tier bonus. */
    public float typeBonus(EquipType type) {
        return this == WOOD && (type == EquipType.SWORD || type == EquipType.SPEAR) ? 0.5F : 0.0F;
    }

    // Armor starts at Copper (no wooden, bone or flint armor).
    public boolean hasArmor() {
        return ordinal() >= COPPER.ordinal();
    }

    // Items of these tiers float in lava and don't burn, like vanilla netherite.
    public boolean isFireResistant() {
        return this == OBSIDIAN || this == NETHERITE || this == REINFORCED;
    }

    // The vanilla tool material built from this tier's (configured) stats. 26.x calls it ToolMaterial, 1.21.1 Tier.
    //? if >=1.21.2 {
    private ToolMaterial material;

    public ToolMaterial material() {
        if (material == null) {
            material = new ToolMaterial(incorrectBlocks, durability(), speed(), attackBonus(), enchantmentValue(), repairItems);
        }
        return material;
    }
    //?} else {
    /*private Tier tier;

    public Tier tier() {
        if (tier == null) {
            ModTiers self = this;
            tier = new Tier() {
                @Override
                public int getUses() {
                    return self.durability();
                }

                @Override
                public float getSpeed() {
                    return self.speed();
                }

                @Override
                public float getAttackDamageBonus() {
                    return self.attackBonus();
                }

                @Override
                public TagKey<Block> getIncorrectBlocksForDrops() {
                    return self.incorrectBlocks;
                }

                @Override
                public int getEnchantmentValue() {
                    return self.enchantmentValue();
                }

                @Override
                public Ingredient getRepairIngredient() {
                    return Ingredient.of(self.repairItems);
                }
            };
        }
        return tier;
    }
    *///?}
}
