package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.enchant.EnchantmentHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Ender's Grasp and Geode Cracker handle drops as they spawn; Volatile Payload arms arrows.
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    // Every entity added to the world passes through here. If a hook took the entity over (e.g. moved a drop straight
    // into the player's inventory), cancel the spawn but report success so vanilla carries on as normal.
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void dooles_core_crucible$onSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EnchantmentHooks.onEntitySpawn((ServerLevel) (Object) this, entity)) cir.setReturnValue(true);
    }
}
