package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.enchant.EnchantmentHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Aegis Reflection turns projectiles back like a Breeze does.
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "deflection", at = @At("RETURN"), cancellable = true)
    // Vanilla already decided how the projectile bounces off; only step in when it would pass straight through (NONE).
    private void dooles_core_crucible$aegisReflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (cir.getReturnValue() == ProjectileDeflection.NONE && (Object) this instanceof LivingEntity defender
                && EnchantmentHooks.aegisReflects(defender, projectile)) {
            cir.setReturnValue(ProjectileDeflection.REVERSE);
        }
    }

    // Gravitic Anchor: pushes can't lift the wearer (a mace smash, a Warden's sonic boom). Argument 1 is the upward push.
    @ModifyVariable(method = "push(DDD)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double dooles_core_crucible$anchorPush(double dy) {
        return dy > 0.0 && (Object) this instanceof LivingEntity living && EnchantmentHooks.hasGraviticAnchor(living) ? 0.0 : dy;
    }
}
