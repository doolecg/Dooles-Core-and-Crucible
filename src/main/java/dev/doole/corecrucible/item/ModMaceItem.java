package dev.doole.corecrucible.item;

import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//? if <1.21.2 {
/*import net.minecraft.world.item.ItemStack;
*///?}

/**
 * A tiered mace. Damage, durability and repair come from the tier. The vanilla smash attack is scaled by
 * {@link #smashFactor}, so a cheap wooden mace doesn't match the Heavy Core mace.
 */
public class ModMaceItem extends MaceItem {

    // Base attack before the tier's attack bonus is added, and the attack speed modifier (vanilla mace: -3.4).
    public static final float BASE_DAMAGE = 3.0F;
    public static final float ATTACK_SPEED = -3.4F;

    private final ModTiers tier;

    public ModMaceItem(ModTiers tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    /** Share of the vanilla smash bonus this tier deals: Wood 25% up to Netherite 100% and Reinforced 115%. */
    public static float smashFactor(ModTiers tier) {
        return switch (tier) {
            case WOOD -> 0.25F;
            case BONE, FLINT, COPPER -> 0.40F;
            case IRON -> 0.55F;
            case EMERALD -> 0.65F;
            case DIAMOND -> 0.75F;
            case OBSIDIAN -> 0.85F;
            case NETHERITE -> 1.0F;
            case REINFORCED -> 1.15F;
        };
    }

    @Override
    // The smash bonus from falling onto a target: vanilla's value, scaled by this tier's share.
    public float getAttackDamageBonus(Entity victim, float damage, DamageSource source) {
        return super.getAttackDamageBonus(victim, damage, source) * smashFactor(tier);
    }

    // The attack damage and speed shown in the tooltip and used in combat.
    public static ItemAttributeModifiers createAttributes(ModTiers tier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, BASE_DAMAGE + tier.attackBonus(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // 1.21.1 only: enchantability and repair material come from the tier. 26.x sets both as item components in ModItems.
    //? if <1.21.2 {
    /*@Override
    public int getEnchantmentValue() {
        return tier.enchantmentValue();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(tier.repairItems());
    }
    *///?}
}
