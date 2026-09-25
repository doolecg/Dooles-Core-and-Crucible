package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doole.corecrucible.enchant.EnchantmentHooks;
import dev.doole.corecrucible.gameplay.ArrowMath;
import dev.doole.corecrucible.item.LauncherShots;
import dev.doole.corecrucible.platform.Events;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.2 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
//?} else {
/*import dev.doole.corecrucible.gameplay.ShieldGuard;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
*///?}

/**
 * Armor piercing for arrows from tiered launchers, plus the enchantment hooks: Ender's Grasp on death drops,
 * Gravitic Anchor (no vertical knockback, no Levitation), Soul Tether (no teleporting) and the pre-damage event. Also
 * the tiered shields: side cover on every version, and the raise time that 1.21.1 hard-codes.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // Gravitic Anchor: the vertical speed before knockback was applied, restored afterwards. NaN = not anchored.
    @Unique
    private double dooles_core_crucible$anchoredY = Double.NaN;

    // Arrow armor pierce: vanilla has already reduced the damage by armor; give back part of what the armor took.
    @ModifyReturnValue(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"))
    private float dooles_core_crucible$arrowPierce(float afterArmor, @Local(argsOnly = true) DamageSource source, @Local(argsOnly = true) float damage) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            return ArrowMath.pierced(damage, afterArmor, LauncherShots.pierceOf(arrow));
        }
        return afterArmor;
    }

    // Shield side cover: lets ShieldGuard lower the damage before vanilla handles the hit.
    //? if >=1.21.2 {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float dooles_core_crucible$hurtAmount(float amount, ServerLevel level, DamageSource source) {
        return Events.livingHurtAmount((LivingEntity) (Object) this, source, amount);
    }
    //?} else {
    /*@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float dooles_core_crucible$hurtAmount(float amount, DamageSource source) {
        return Events.livingHurtAmount((LivingEntity) (Object) this, source, amount);
    }

    // 1.21.1 hard-codes the 5-tick raise delay in isBlocking (26.x reads it from blocks_attacks).
    @ModifyConstant(method = "isBlocking", constant = @Constant(intValue = 5))
    private int dooles_core_crucible$raiseTicks(int vanilla) {
        return ShieldGuard.raiseTicks(((LivingEntity) (Object) this).getUseItem(), vanilla);
    }
    *///?}

    // Pre-damage enchantment hooks: returning false from hurt cancels the hit entirely.
    //? if >=1.21.2 {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$beforeHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Events.livingHurt((LivingEntity) (Object) this, source)) cir.setReturnValue(false);
    }
    //?} else {
    /*@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$beforeHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Events.livingHurt((LivingEntity) (Object) this, source)) cir.setReturnValue(false);
    }
    *///?}

    // Ender's Grasp: mark the start and end of a mob's death drops, so drops spawned in between (ServerLevelMixin) can go
    // straight to the killer's inventory.
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void dooles_core_crucible$beforeDeathDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        EnchantmentHooks.onBeforeDeathDrops(source);
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void dooles_core_crucible$afterDeathDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        EnchantmentHooks.onAfterDeathDrops();
    }

    // Gravitic Anchor, part 1: before knockback, remember the current vertical speed.
    //? if >=1.21.2 {
    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "knockback(DDD)V", at = @At("HEAD"))
    *///?}
    private void dooles_core_crucible$rememberHeight(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        dooles_core_crucible$anchoredY = EnchantmentHooks.hasGraviticAnchor(self) ? self.getDeltaMovement().y : Double.NaN;
    }

    // Gravitic Anchor, part 2: after knockback, put the vertical speed back so only horizontal knockback remains.
    //? if >=1.21.2 {
    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "knockback(DDD)V", at = @At("TAIL"))
    *///?}
    private void dooles_core_crucible$cancelVerticalKnockback(CallbackInfo ci) {
        if (!Double.isNaN(dooles_core_crucible$anchoredY)) {
            LivingEntity self = (LivingEntity) (Object) this;
            Vec3 motion = self.getDeltaMovement();
            self.setDeltaMovement(motion.x, dooles_core_crucible$anchoredY, motion.z);
            dooles_core_crucible$anchoredY = Double.NaN;
        }
    }

    // Gravitic Anchor also makes the wearer immune to Levitation.
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$resistLevitation(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect().equals(MobEffects.LEVITATION) && EnchantmentHooks.hasGraviticAnchor((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    // randomTeleport is overloaded differently per version and by NeoForge, so every overload is targeted.
    @Inject(method = "randomTeleport*", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$soulTether(CallbackInfoReturnable<Boolean> cir) {
        if (EnchantmentHooks.isTethered((LivingEntity) (Object) this)) cir.setReturnValue(false);
    }
}
