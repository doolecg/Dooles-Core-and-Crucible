package dev.doole.corecrucible.mixin.client;

import dev.doole.corecrucible.client.VeinMiningClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Client tick hook: once per client tick, VeinMiningClient checks the Vein Resonance key and tells the server when it changes.
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void dooles_core_crucible$afterTick(CallbackInfo ci) {
        VeinMiningClient.tick((Minecraft) (Object) this);
    }
}
