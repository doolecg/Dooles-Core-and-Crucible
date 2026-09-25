package dev.doole.corecrucible.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.client.LauncherClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The bow-draw zoom checks is(Items.BOW); tiered bows zoom too.
// LauncherClient.isLauncher keeps vanilla's answer and also answers yes when the check is for Items.BOW or
// Items.CROSSBOW and the held item is one of our tiered ones.
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    //? if >=1.21.2 {
    // 26.x calls the generic TypedInstance#is(Object); 1.21.1's ItemStack#is takes an Item.
    @WrapOperation(method = "getFieldOfViewModifier",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private boolean dooles_core_crucible$tieredBowZoom(ItemStack stack, Object item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
    //?} else {
    /*@WrapOperation(method = "getFieldOfViewModifier",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean dooles_core_crucible$tieredBowZoom(ItemStack stack, net.minecraft.world.item.Item item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
    *///?}
}
