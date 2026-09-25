package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.item.ModMaceItem;
import dev.doole.corecrucible.item.TemplateItems;
import dev.doole.corecrucible.item.TieredBowItem;
import dev.doole.corecrucible.item.TieredCrossbowItem;
import dev.doole.corecrucible.item.TieredFishingRodItem;
import dev.doole.corecrucible.item.TieredShearsItem;
import dev.doole.corecrucible.item.TieredShieldItem;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
//? if >=1.21.2 {
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.equipment.ArmorType;
//?} else {
/*import dev.doole.corecrucible.item.TieredHorseArmorItem;
import dev.doole.corecrucible.item.TieredWolfArmorItem;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
*///?}
//? if 26.2 {
/*import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ShovelItem;
*///?}

/**
 * All mod items. Equipment that vanilla already provides is not registered;
 * {@link #equipment} returns the vanilla item for those slots instead.
 */
public final class ModItems {

    //? if >=1.21.2 {
    /** 26.x ships copper gear and spears; 1.21.1 gets mod items with the same paths. */
    public static final boolean VANILLA_COPPER_AND_SPEARS = true;
    //?} else {
    /*public static final boolean VANILLA_COPPER_AND_SPEARS = false;
    *///?}

    /** Tiers that craft alloys and upgrade templates. Netherite uses the vanilla template. */
    public static final ModTiers[] ALLOY_TIERS = {ModTiers.IRON, ModTiers.EMERALD, ModTiers.DIAMOND, ModTiers.OBSIDIAN, ModTiers.NETHERITE, ModTiers.REINFORCED};

    // Lookup tables filled in by register(): every mod item by name, gear by tier and type, and the ingots and templates.
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<ModTiers, Map<EquipType, Item>> EQUIPMENT = new EnumMap<>(ModTiers.class);
    private static final Map<ModTiers, Item> ALLOY_INGOTS = new EnumMap<>(ModTiers.class);
    private static final Map<ModTiers, Item> UPGRADE_TEMPLATES = new EnumMap<>(ModTiers.class);

    private ModItems() {
    }

    /** Called by the loader-specific registration hook. */
    public static void register(BiConsumer<Identifier, Item> registrar) {
        // 1. Alloy ingots, one per alloy tier (iron_alloy_ingot ... reinforced_alloy_ingot).
        for (ModTiers tier : ALLOY_TIERS) {
            Item.Properties props = new Item.Properties();
            if (tier.isFireResistant()) props.fireResistant();
            ALLOY_INGOTS.put(tier, register(registrar, tier.prefix() + "_alloy_ingot", Item::new, props));
        }

        for (ModTiers tier : ALLOY_TIERS) {
            // 2. Upgrade templates. Netherite is skipped because vanilla already has a netherite upgrade template.
            if (tier == ModTiers.NETHERITE) continue;
            String name = tier.prefix() + "_upgrade";
            UPGRADE_TEMPLATES.put(tier, register(registrar, name + "_smithing_template", p -> TemplateItems.upgrade(name, p), new Item.Properties()));
        }

        // 3. Gear: every tier × type that exists and isn't already a vanilla item (vanilla's iron_pickaxe is used as-is).
        for (ModTiers tier : ModTiers.values()) {
            for (EquipType type : EquipType.IN_ORDER) {
                if (!type.existsFor(tier) || isVanilla(tier, type)) continue;
                Item item = register(registrar, tier.prefix() + "_" + type.suffix(), p -> createEquipment(tier, type, p), new Item.Properties());
                Map<EquipType, Item> byType = EQUIPMENT.get(tier);
                if (byType == null) {
                    byType = new EnumMap<>(EquipType.class);
                    EQUIPMENT.put(tier, byType);
                }
                byType.put(type, item);
            }
        }

        //? if <1.21.2 {
        /*// Backports of 26.x items outside the chain. Stone spear is skipped: stone tools are removed.
        register(registrar, "golden_spear", p -> spear(Tiers.GOLD, 0.0F, p), new Item.Properties());
        register(registrar, "copper_nugget", Item::new, new Item.Properties());
        *///?}
    }

    /** True when this chain slot is filled by a vanilla item on the running version. */
    // To see which items the mod adds and which it borrows from vanilla, read this method.
    public static boolean isVanilla(ModTiers tier, EquipType type) {
        if (!type.existsFor(tier) || type == EquipType.MACE) return false;
        if (type.isArmor()) return !ModArmorMaterials.isModded(tier);
        if (type.isLauncher() || type.isFishingRod()) return tier == ModTiers.WOOD; // vanilla bow, crossbow, fishing rod
        if (type.isShears()) return tier == ModTiers.IRON; // vanilla shears; there's no vanilla copper pair
        if (type.isShield()) return tier == ModTiers.WOOD; // vanilla shield
        if (type == EquipType.WOLF_ARMOR) return tier == ModTiers.COPPER; // vanilla (armadillo scute) wolf armor
        if (type == EquipType.HORSE_ARMOR) {
            return switch (tier) {
                case IRON, DIAMOND -> true;
                case COPPER, NETHERITE -> VANILLA_COPPER_AND_SPEARS; // 26.x added copper and netherite horse armor
                default -> false;
            };
        }
        boolean vanillaTier = switch (tier) {
            case WOOD, IRON, DIAMOND, NETHERITE -> true;
            case COPPER -> VANILLA_COPPER_AND_SPEARS;
            default -> false;
        };
        return vanillaTier && (type != EquipType.SPEAR || VANILLA_COPPER_AND_SPEARS);
    }

    /** The item filling this chain slot, vanilla or modded, or null when the slot doesn't exist. */
    public static Item equipment(ModTiers tier, EquipType type) {
        if (!type.existsFor(tier)) return null;
        // Vanilla's shears, shield and wolf armor have no tier prefix (just "shears", unlike "iron_pickaxe"): there's
        // only one of each.
        if (isVanilla(tier, type)) {
            boolean single = type.isShears() || type.isShield() || type == EquipType.WOLF_ARMOR;
            return vanillaItem(single ? type.suffix() : tier.prefix() + "_" + type.suffix());
        }
        Map<EquipType, Item> byType = EQUIPMENT.get(tier);
        return byType == null ? null : byType.get(type);
    }

    public static Item alloyIngot(ModTiers tier) {
        return ALLOY_INGOTS.get(tier);
    }

    /** The upgrade template for a smithing tier; Netherite returns the vanilla template. */
    public static Item upgradeTemplate(ModTiers tier) {
        return tier == ModTiers.NETHERITE ? vanillaItem("netherite_upgrade_smithing_template") : UPGRADE_TEMPLATES.get(tier);
    }

    private static Map<Item, ModTiers> tierByItem;
    private static Map<Item, EquipType> typeByItem;

    // Builds a lookup from item to its chain tier/slot, once, the first time either is needed.
    private static void indexChain() {
        if (tierByItem != null) return;
        Map<Item, ModTiers> tiers = new HashMap<>();
        Map<Item, EquipType> types = new HashMap<>();
        for (ModTiers tier : ModTiers.values()) {
            for (EquipType type : EquipType.values()) {
                Item slot = equipment(tier, type);
                if (slot == null) continue;
                tiers.put(slot, tier);
                types.put(slot, type);
            }
        }
        typeByItem = types;
        tierByItem = tiers;
    }

    /** The chain tier of a tool, weapon or armor piece (vanilla or modded), or null when it's not in the chain. */
    public static ModTiers tierOf(Item item) {
        indexChain();
        return tierByItem.get(item);
    }

    /** The chain slot of a tool, weapon or armor piece, or null when it's not in the chain. */
    public static EquipType typeOf(Item item) {
        indexChain();
        return typeByItem.get(item);
    }

    /** Every mod item, in registration order. */
    public static Collection<Item> all() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    // Registers one item under mod_id:name, remembers it, and returns it.
    private static Item register(BiConsumer<Identifier, Item> registrar, String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        Identifier id = CoreCrucible.id(name);
        //? if >=1.21.2 {
        props.setId(ResourceKey.create(Registries.ITEM, id));
        //?}
        Item item = factory.apply(props);
        registrar.accept(id, item);
        ITEMS.put(name, item);
        return item;
    }

    // Looks up an existing vanilla item by its plain "minecraft:" path, e.g. "iron_pickaxe".
    private static Item vanillaItem(String path) {
        //? if >=1.21.2 {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
        //?} else {
        /*return BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(path));
        *///?}
    }

    // Builds the actual Item for one tier + slot combo (a "netherite_pickaxe" object, say), picking the right
    // vanilla item class and stats for each slot.
    private static Item createEquipment(ModTiers tier, EquipType type, Item.Properties p) {
        if (tier.isFireResistant()) p.fireResistant();
        // The numbers passed below are each tool type's base attack damage and attack speed (the same as vanilla's for that
        // tool). The tier's attack bonus is added on top by vanilla. The hoe passes minus the bonus, so hoes always hit for 1.
        float bonus = tier.typeBonus(type);
        //? if >=1.21.2 {
        ToolMaterial m = tier.material();
        return switch (type) {
            case PICKAXE -> new Item(p.pickaxe(m, 1.0F, -2.8F));
            case AXE -> axe(m, tier.axeDamage(), tier.axeSpeed(), p);
            case SHOVEL -> shovel(m, 1.5F, -3.0F, p);
            case HOE -> hoe(m, -tier.attackBonus(), tier.hoeSpeed(), p);
            case SWORD -> new Item(p.sword(m, 3.0F + bonus, -2.4F));
            case SPEAR -> {
                float[] s = tier.spearParams();
                yield new Item(p.spear(m, s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], s[8]));
            }
            case BOW -> new TieredBowItem(tier, launcher(tier, type, p));
            case CROSSBOW -> new TieredCrossbowItem(tier, launcher(tier, type, p)
                    .component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY));
            case FISHING_ROD -> new TieredFishingRodItem(tier, rod(tier, p));
            // Shears get their mining speed from the TOOL component, scaled from vanilla's shears by the tier.
            case SHEARS -> new TieredShearsItem(tier, p.durability(ShearsStats.of(tier).durability())
                    .repairable(tier.repairItems()).enchantable(tier.enchantmentValue())
                    .component(DataComponents.TOOL, ShearsStats.of(tier).scaledTool()));
            case SHIELD -> new TieredShieldItem(tier, shield(tier, p));
            case HORSE_ARMOR -> new Item(p.horseArmor(ModArmorMaterials.material(tier)));
            case WOLF_ARMOR -> new Item(p.wolfArmor(ModArmorMaterials.wolfMaterial(tier)).durability(BodyArmorStats.of(tier).wolfDurability()));
            case MACE -> new ModMaceItem(tier, p.durability(tier.durability())
                    .component(DataComponents.TOOL, MaceItem.createToolProperties())
                    .repairable(tier.repairItems())
                    .attributes(ModMaceItem.createAttributes(tier))
                    .enchantable(tier.enchantmentValue())
                    .component(DataComponents.WEAPON, new Weapon(1)));
            case HELMET -> new Item(p.humanoidArmor(ModArmorMaterials.material(tier), ArmorType.HELMET));
            case CHESTPLATE -> new Item(p.humanoidArmor(ModArmorMaterials.material(tier), ArmorType.CHESTPLATE));
            case LEGGINGS -> new Item(p.humanoidArmor(ModArmorMaterials.material(tier), ArmorType.LEGGINGS));
            case BOOTS -> new Item(p.humanoidArmor(ModArmorMaterials.material(tier), ArmorType.BOOTS));
        };
        //?} else {
        /*Tier t = tier.tier();
        return switch (type) {
            case PICKAXE -> new PickaxeItem(t, p.attributes(DiggerItem.createAttributes(t, 1.0F, -2.8F)));
            case AXE -> new AxeItem(t, p.attributes(DiggerItem.createAttributes(t, tier.axeDamage(), tier.axeSpeed())));
            case SHOVEL -> new ShovelItem(t, p.attributes(DiggerItem.createAttributes(t, 1.5F, -3.0F)));
            case HOE -> new HoeItem(t, p.attributes(DiggerItem.createAttributes(t, -tier.attackBonus(), tier.hoeSpeed())));
            case SWORD -> new SwordItem(t, p.attributes(swordAttributes(t, 3.0F + bonus, -2.4F).build()));
            case SPEAR -> spear(t, bonus, p);
            case BOW -> new TieredBowItem(tier, p.durability(LauncherStats.of(tier).durability(type)));
            case CROSSBOW -> new TieredCrossbowItem(tier, p.stacksTo(1).durability(LauncherStats.of(tier).durability(type))
                    .component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY));
            case FISHING_ROD -> new TieredFishingRodItem(tier, p.durability(RodStats.of(tier).durability()));
            case SHEARS -> new TieredShearsItem(tier, p.durability(ShearsStats.of(tier).durability())
                    .component(DataComponents.TOOL, ShearsStats.of(tier).scaledTool()));
            case SHIELD -> new TieredShieldItem(tier, p.durability(ShieldStats.of(tier).durability())
                    .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
            case HORSE_ARMOR -> new TieredHorseArmorItem(tier, p.stacksTo(1));
            case WOLF_ARMOR -> new TieredWolfArmorItem(tier, p.durability(BodyArmorStats.of(tier).wolfDurability()));
            case MACE -> new ModMaceItem(tier, p.durability(tier.durability())
                    .component(DataComponents.TOOL, MaceItem.createToolProperties())
                    .attributes(ModMaceItem.createAttributes(tier)));
            case HELMET -> armor(tier, ArmorItem.Type.HELMET, p);
            case CHESTPLATE -> armor(tier, ArmorItem.Type.CHESTPLATE, p);
            case LEGGINGS -> armor(tier, ArmorItem.Type.LEGGINGS, p);
            case BOOTS -> armor(tier, ArmorItem.Type.BOOTS, p);
        };
        *///?}
    }

    //? if >=1.21.2 {
    private static Item.Properties launcher(ModTiers tier, EquipType type, Item.Properties p) {
        return p.stacksTo(1).durability(LauncherStats.of(tier).durability(type)).repairable(tier.repairItems())
                .enchantable(tier.enchantmentValue());
    }

    private static Item.Properties rod(ModTiers tier, Item.Properties p) {
        return p.stacksTo(1).durability(RodStats.of(tier).durability()).repairable(tier.repairItems())
                .enchantable(tier.enchantmentValue());
    }

    // Vanilla's shield properties (Items.SHIELD), with the tier's durability, repair tag, raise time and axe disable
    // time. Vanilla's cooldown is 5 s times disable_cooldown_scale.
    private static Item.Properties shield(ModTiers tier, Item.Properties p) {
        ShieldStats stats = ShieldStats.of(tier);
        float raiseSeconds = stats.raiseTicks() / 20.0F;
        float disableScale = (float) (stats.disableSeconds() / ShieldStats.VANILLA_DISABLE_SECONDS);
        return p.durability(stats.durability())
                .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
                .repairable(tier.repairItems())
                .equippableUnswappable(EquipmentSlot.OFFHAND)
                .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(raiseSeconds, disableScale,
                        List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                        new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                        Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                        Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK)))
                .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
    }
    //?}

    // 26.2 still has AxeItem/ShovelItem; 26.3 moved stripping and pathing into Item.Properties.
    //? if >=26.3 {
    private static Item axe(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new Item(p.axe(m, damage, speed));
    }

    private static Item shovel(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new Item(p.shovel(m, damage, speed));
    }

    private static Item hoe(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new Item(p.hoe(m, damage, speed));
    }
    //?} else if 26.2 {
    /*private static Item axe(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new AxeItem(m, damage, speed, p);
    }

    private static Item shovel(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new ShovelItem(m, damage, speed, p);
    }

    private static Item hoe(ToolMaterial m, float damage, float speed, Item.Properties p) {
        return new HoeItem(m, damage, speed, p);
    }
    *///?}

    //? if <1.21.2 {
    /*// Spears don't exist on 1.21.1: a sword-like item with +1 reach.
    private static Item spear(Tier t, float bonus, Item.Properties p) {
        return new SwordItem(t, p.attributes(swordAttributes(t, 2.0F + bonus, -2.6F)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(CoreCrucible.id("spear_reach"), 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    // SwordItem.createAttributes only takes an int damage on Fabric 1.21.1; wood swords need +0.5.
    private static ItemAttributeModifiers.Builder swordAttributes(Tier t, float damage, float speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),
                        damage + t.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),
                        speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    private static Item armor(ModTiers tier, ArmorItem.Type type, Item.Properties p) {
        int multiplier = ModArmorMaterials.stats(tier).durabilityMultiplier();
        return new ArmorItem(ModArmorMaterials.holder(tier), type, p.durability(type.getDurability(multiplier)));
    }
    *///?}
}
