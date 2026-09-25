package dev.doole.corecrucible.registry;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.config.BalanceConfig;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
//? if >=1.21.2 {
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
//?} else {
/*import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
*///?}

/** Armor stats for the mod's own armor tiers. Iron, Diamond and Netherite stay vanilla. */
public final class ModArmorMaterials {

    /**
     * Defense is helmet / chestplate / leggings / boots. The equip sound is looked up only when a material is built,
     * so reading the stats (e.g. {@link #isModded} in unit tests) doesn't need Minecraft's registries loaded.
     */
    public record Stats(ModTiers tier, int helmet, int chestplate, int leggings, int boots, float toughness,
                        float knockbackResistance, int durabilityMultiplier, int enchantmentValue,
                        Supplier<Holder<SoundEvent>> equipSound) {

        public int defense(EquipType type) {
            return switch (type) {
                case HELMET -> helmet;
                case CHESTPLATE -> chestplate;
                case LEGGINGS -> leggings;
                case BOOTS -> boots;
                default -> 0;
            };
        }

        public Identifier id() {
            return CoreCrucible.id(tier.prefix());
        }

        /** These stats with the balance config's values in place of the defaults. */
        Stats configured() {
            String k = key(tier);
            return new Stats(tier, BalanceConfig.get(k + "helmet", helmet), BalanceConfig.get(k + "chestplate", chestplate),
                    BalanceConfig.get(k + "leggings", leggings), BalanceConfig.get(k + "boots", boots),
                    BalanceConfig.get(k + "toughness", toughness), BalanceConfig.get(k + "knockback_resistance", knockbackResistance),
                    BalanceConfig.get(k + "durability_multiplier", durabilityMultiplier),
                    BalanceConfig.get(k + "enchantability", enchantmentValue), equipSound);
        }
    }

    private static final Map<ModTiers, Stats> STATS = new EnumMap<>(ModTiers.class);

    static {
        //? if <1.21.2 {
        /*add(new Stats(ModTiers.COPPER, 2, 4, 3, 1, 0.0F, 0.0F, 11, 8, () -> SoundEvents.ARMOR_EQUIP_IRON));
        *///?}
        // Default armor stats for the tiers whose armor the mod adds (copper only on 1.21.1; 26.x has vanilla copper armor).
        // Order: tier, helmet, chestplate, leggings, boots armor points, toughness, knockback resistance, durability multiplier
        // (vanilla iron is 15, diamond 33), enchantability, equip sound. Change them here, or in the config under "armor".
        add(new Stats(ModTiers.EMERALD, 3, 6, 5, 2, 1.0F, 0.0F, 24, 18, () -> SoundEvents.ARMOR_EQUIP_DIAMOND));
        add(new Stats(ModTiers.OBSIDIAN, 3, 8, 6, 3, 2.0F, 0.1F, 42, 10, () -> SoundEvents.ARMOR_EQUIP_NETHERITE));
        add(new Stats(ModTiers.REINFORCED, 4, 9, 7, 4, 4.0F, 0.15F, 45, 15, () -> SoundEvents.ARMOR_EQUIP_NETHERITE));
    }

    private ModArmorMaterials() {
    }

    private static void add(Stats stats) {
        STATS.put(stats.tier(), stats);
    }

    /** True when this tier's armor is a mod item on the running version. */
    public static boolean isModded(ModTiers tier) {
        return STATS.containsKey(tier);
    }

    /** The tier's armor stats, config applied; null for a tier whose armor is vanilla. */
    public static Stats stats(ModTiers tier) {
        Stats stats = STATS.get(tier);
        return stats == null ? null : stats.configured();
    }

    private static String key(ModTiers tier) {
        return "armor." + tier.name().toLowerCase(Locale.ROOT) + ".";
    }

    /** The {@code armor} section of the balance config ({@code config/dooles_core_crucible.json}). */
    public static void writeDefaults(Map<String, Double> out) {
        for (Stats s : STATS.values()) {
            String k = key(s.tier());
            out.put(k + "helmet", (double) s.helmet());
            out.put(k + "chestplate", (double) s.chestplate());
            out.put(k + "leggings", (double) s.leggings());
            out.put(k + "boots", (double) s.boots());
            out.put(k + "toughness", (double) s.toughness());
            out.put(k + "knockback_resistance", (double) s.knockbackResistance());
            out.put(k + "durability_multiplier", (double) s.durabilityMultiplier());
            out.put(k + "enchantability", (double) s.enchantmentValue());
        }
    }

    //? if >=1.21.2 {
    private static final Map<ModTiers, ArmorMaterial> MATERIALS = new EnumMap<>(ModTiers.class);

    // Builds the vanilla ArmorMaterial for this tier the first time it's asked for, then reuses it.
    public static ArmorMaterial material(ModTiers tier) {
        ArmorMaterial existing = MATERIALS.get(tier);
        if (existing != null) return existing;
        Stats s = stats(tier);
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.HELMET, s.helmet());
        defense.put(ArmorType.CHESTPLATE, s.chestplate());
        defense.put(ArmorType.LEGGINGS, s.leggings());
        defense.put(ArmorType.BOOTS, s.boots());
        defense.put(ArmorType.BODY, BodyArmorStats.of(tier).horseDefense());
        ArmorMaterial material = new ArmorMaterial(s.durabilityMultiplier(), defense, s.enchantmentValue(), s.equipSound().get(),
                s.toughness(), s.knockbackResistance(), tier.repairItems(),
                ResourceKey.create(EquipmentAssets.ROOT_ID, s.id()));
        MATERIALS.put(tier, material);
        return material;
    }

    private static final Map<ModTiers, ArmorMaterial> WOLF_MATERIALS = new EnumMap<>(ModTiers.class);

    /**
     * The material a tier's wolf armor is built from: vanilla's armadillo scute armor points and sound,
     * the tier's repair tag, and the tier's equipment asset for the {@code wolf_body} texture. Durability is set on the
     * item itself, from {@link BodyArmorStats}.
     */
    public static ArmorMaterial wolfMaterial(ModTiers tier) {
        ArmorMaterial existing = WOLF_MATERIALS.get(tier);
        if (existing != null) return existing;
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        defense.put(ArmorType.BODY, BodyArmorStats.WOLF_DEFENSE);
        ArmorMaterial material = new ArmorMaterial(4, defense, 10, SoundEvents.ARMOR_EQUIP_WOLF, 0.0F, 0.0F, tier.repairItems(),
                ResourceKey.create(EquipmentAssets.ROOT_ID, CoreCrucible.id(tier.prefix())));
        WOLF_MATERIALS.put(tier, material);
        return material;
    }
    //?} else {
    /*// Builds and registers a vanilla ArmorMaterial for every modded tier.
    public static void register(BiConsumer<Identifier, ArmorMaterial> registrar) {
        for (Stats base : STATS.values()) {
            Stats s = base.configured();
            Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.HELMET, s.helmet());
            defense.put(ArmorItem.Type.CHESTPLATE, s.chestplate());
            defense.put(ArmorItem.Type.LEGGINGS, s.leggings());
            defense.put(ArmorItem.Type.BOOTS, s.boots());
            List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(s.id())); // textures/models/armor/<tier>_layer_N.png
            registrar.accept(s.id(), new ArmorMaterial(defense, s.enchantmentValue(), s.equipSound().get(),
                    () -> Ingredient.of(s.tier().repairItems()), layers, s.toughness(), s.knockbackResistance()));
        }
    }

    public static Holder<ArmorMaterial> holder(ModTiers tier) {
        return BuiltInRegistries.ARMOR_MATERIAL.getHolderOrThrow(ResourceKey.create(Registries.ARMOR_MATERIAL, STATS.get(tier).id()));
    }
    *///?}
}
