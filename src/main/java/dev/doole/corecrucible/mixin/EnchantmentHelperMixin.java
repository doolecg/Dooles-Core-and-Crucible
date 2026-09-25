package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doole.corecrucible.enchant.EnchantmentHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Kinetic Resonance scales the enchanted melee damage.
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    // Takes the damage vanilla calculated (Sharpness etc. included) and returns Kinetic Resonance's adjusted value.
    @ModifyReturnValue(method = "modifyDamage", at = @At("RETURN"))
    private static float dooles_core_crucible$kineticResonance(float damage, ServerLevel level, ItemStack weapon, Entity victim, DamageSource source) {
        return EnchantmentHooks.modifyAttackDamage(level, weapon, victim, source, damage);
    }
}
