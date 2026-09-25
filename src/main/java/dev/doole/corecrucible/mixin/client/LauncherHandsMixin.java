package dev.doole.corecrucible.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.client.LauncherClient;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.3 {
import net.minecraft.client.player.FirstPersonHandsAndItems;
//?} else {
/*import net.minecraft.client.renderer.ItemInHandRenderer;
*///?}

// First-person hands: vanilla picks which hands to draw from is(Items.BOW) / is(Items.CROSSBOW).
//? if >=26.3 {
@Mixin(FirstPersonHandsAndItems.class)
//?} else {
/*@Mixin(ItemInHandRenderer.class)
*///?}
public abstract class LauncherHandsMixin {
    //? if >=26.3 {
    @WrapOperation(method = {"evaluateWhichHandsToRender", "isChargedCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private static boolean dooles_core_crucible$tieredLauncher(ItemStack stack, Object item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
    //?} else if >=1.21.2 {
    /*@WrapOperation(method = {"evaluateWhichHandsToRender", "selectionUsingItemWhileHoldingBowLike", "isChargedCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private static boolean dooles_core_crucible$tieredLauncher(ItemStack stack, Object item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
    *///?} else {
    /*// 1.21.1's ItemStack#is takes an Item; 26.x calls the generic TypedInstance#is(Object).
    @WrapOperation(method = {"evaluateWhichHandsToRender", "selectionUsingItemWhileHoldingBowLike", "isChargedCrossbow"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean dooles_core_crucible$tieredLauncher(ItemStack stack, net.minecraft.world.item.Item item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
    *///?}
}
