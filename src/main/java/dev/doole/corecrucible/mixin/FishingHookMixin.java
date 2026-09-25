package dev.doole.corecrucible.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.registry.RodStats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.21.2 {
/*import net.minecraft.world.item.Item;
*///?}

/**
 * Tiered fishing rod stats: lure, luck, reel strength and cast distance. Applied on the
 * {@code FishingHook} itself so any rod that casts one (vanilla or ours) is covered the same way.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    // Shadow fields: vanilla's private luck and lure values. @Mutable lets us change them even though they're final.
    @Shadow
    @Final
    @Mutable
    private int luck;
    @Shadow
    @Final
    @Mutable
    private int lureSpeed;

    // Remembered for pullEntity, which runs later than the constructor.
    @Unique
    private double dooles_core_crucible$reel = 1.0;

    // Applies a tiered rod's stats once, right after vanilla sets up the hook's flight, luck and lure. The rod is
    // the main-hand item if it's a fishing rod, else the off-hand one, matching how vanilla itself picks a hand.
    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At("TAIL"))
    private void dooles_core_crucible$onCast(Player player, Level level, int ctorLuck, int ctorLureSpeed, CallbackInfo ci) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack rod = mainHand.getItem() instanceof FishingRodItem ? mainHand : player.getOffhandItem();
        RodStats stats = RodStats.of(rod);
        if (stats == null) return;
        dooles_core_crucible$reel = stats.reel();
        luck += stats.luck();
        // Lure levels are worth 100 ticks each (vanilla's 5s-per-level, ×20 ticks/s); Lure V (500) never bites faster.
        lureSpeed = Math.min(500, lureSpeed + stats.lure() * 100);
        Entity self = (Entity) (Object) this;
        self.setDeltaMovement(self.getDeltaMovement().scale(stats.cast()));
    }

    // Vanilla pulls a hooked entity in at a flat 0.1 strength; tiered rods reel harder. The caught-item toss in
    // retrieve() is left alone: it's tuned to land at the player, and a stronger pull would overshoot.
    // Changes the number passed to Vec3.scale inside pullEntity (the pull strength).
    @ModifyArg(method = "pullEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private double dooles_core_crucible$reelStrength(double scale) {
        return scale * dooles_core_crucible$reel;
    }

    //? if >=1.21.2 {
    // 26.x calls the generic TypedInstance#is(Object); vanilla only keeps the hook alive if you hold
    // Items.FISHING_ROD, so a tiered hook would otherwise be discarded on the very next tick. NeoForge already
    // widens this itself (to canPerformAction, which FishingRodItem's subclasses inherit), hence require = 0.
    @WrapOperation(method = "shouldStopFishing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredRodCast(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof FishingRodItem;
    }
    //?} else {
    /*// 1.21.1's ItemStack#is takes an Item. NeoForge already widens this itself, hence require = 0.
    @WrapOperation(method = "shouldStopFishing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredRodCast(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof FishingRodItem;
    }
    *///?}
}
