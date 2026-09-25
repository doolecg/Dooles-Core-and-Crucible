package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.platform.Events;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Runs the mod's per-player work (enchantment effects, shield stats) at the end of every player tick. Events.playerTick
// ignores the client side.
@Mixin(Player.class)
public abstract class PlayerTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void dooles_core_crucible$afterTick(CallbackInfo ci) {
        Events.playerTick((Player) (Object) this);
    }
}
