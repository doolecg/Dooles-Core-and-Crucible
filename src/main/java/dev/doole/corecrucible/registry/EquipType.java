package dev.doole.corecrucible.registry;

/** Every equipment category in the progression chain. */
public enum EquipType {
    // Each entry: the item id suffix (emerald_PICKAXE) and whether it's worn player armor.
    PICKAXE("pickaxe", false),
    AXE("axe", false),
    SHOVEL("shovel", false),
    SWORD("sword", false),
    SPEAR("spear", false),
    MACE("mace", false),
    HELMET("helmet", true),
    CHESTPLATE("chestplate", true),
    LEGGINGS("leggings", true),
    BOOTS("boots", true),
    // Appended so existing ordinals stay put; recipes name slots, so order here doesn't matter to data.
    HOE("hoe", false),
    BOW("bow", false),
    CROSSBOW("crossbow", false),
    FISHING_ROD("fishing_rod", false),
    // Appended so existing ordinals stay put: only Bone, Flint, Copper and Iron have shears (see existsFor).
    SHEARS("shears", false),
    // Appended so existing ordinals stay put. Horse and wolf armor follow the armor tiers but aren't worn by players,
    // so they don't count as armor here (see isBodyArmor).
    SHIELD("shield", false),
    HORSE_ARMOR("horse_armor", false),
    WOLF_ARMOR("wolf_armor", false);

    /** Tools then armor: registration and creative-tab order. */
    public static final EquipType[] IN_ORDER = {PICKAXE, AXE, SHOVEL, HOE, SWORD, SPEAR, MACE, BOW, CROSSBOW, FISHING_ROD, SHEARS, SHIELD,
            HELMET, CHESTPLATE, LEGGINGS, BOOTS, HORSE_ARMOR, WOLF_ARMOR};

    private final String suffix;
    private final boolean armor;

    EquipType(String suffix, boolean armor) {
        this.suffix = suffix;
        this.armor = armor;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isArmor() {
        return armor;
    }

    /** Bows and crossbows: stats come from {@link LauncherStats}, not the tool tier. */
    public boolean isLauncher() {
        return this == BOW || this == CROSSBOW;
    }

    /** Fishing rods: stats come from {@link RodStats}, not the tool tier. */
    public boolean isFishingRod() {
        return this == FISHING_ROD;
    }

    /** Shears: stats come from {@link ShearsStats}, not the tool tier; Iron is the vanilla item. */
    public boolean isShears() {
        return this == SHEARS;
    }

    /** Shields: stats come from {@link ShieldStats}; Wood is the vanilla shield. */
    public boolean isShield() {
        return this == SHIELD;
    }

    /** Horse and wolf armor: stats come from {@link BodyArmorStats}. */
    public boolean isBodyArmor() {
        return this == HORSE_ARMOR || this == WOLF_ARMOR;
    }

    /** Bone and Flint (Tier 1) have no mace or armor: those paths skip Tier 1. Shears stop at Iron. */
    // Which tier/type pairs exist as items. Tooltips, recipes and the upgrade path all follow this.
    public boolean existsFor(ModTiers tier) {
        if (this == SHEARS) return tier == ModTiers.BONE || tier == ModTiers.FLINT || tier == ModTiers.COPPER || tier == ModTiers.IRON;
        if (armor || isBodyArmor()) return tier.hasArmor();
        if (tier.harvestLevel() == 1 && tier != ModTiers.COPPER) return this != MACE;
        return true;
    }
}
