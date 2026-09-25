package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.enchant.EnchantmentHooks;
import dev.doole.corecrucible.gameplay.HoeFarming;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Brackets every player block break so the mining enchantments can see its drops and act once the block is gone,
// and so a hoe's harvest can replant.
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    // HEAD = before vanilla breaks the block: remember the block state while it still exists.
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void dooles_core_crucible$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentHooks.onBeforeBreak(player, level.getBlockState(pos));
    }

    // RETURN = after vanilla has broken the block. The return value says whether the break actually happened.
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void dooles_core_crucible$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        HoeFarming.afterBreak(level, pos);
        EnchantmentHooks.onAfterBreak(player, pos, cir.getReturnValueZ());
    }
}
