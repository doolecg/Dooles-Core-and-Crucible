package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.platform.Events;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Runs the mod's once-per-server-tick work (timers for the enchantments) at the end of every server tick.
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void dooles_core_crucible$afterTick(BooleanSupplier hasTime, CallbackInfo ci) {
        Events.serverTick((MinecraftServer) (Object) this);
    }
}
