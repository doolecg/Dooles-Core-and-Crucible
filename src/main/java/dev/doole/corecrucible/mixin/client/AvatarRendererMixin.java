package dev.doole.corecrucible.mixin.client;

//? if >=1.21.2 {

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.client.LauncherClient;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// The charged-crossbow arm pose checks is(Items.CROSSBOW) on 26.x (the generic is(Object)); 1.21.1's PlayerRenderer uses instanceof.
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    // Two overloads exist; only the (Avatar, ItemStack, InteractionHand) one has the crossbow check.
    @WrapOperation(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private static boolean dooles_core_crucible$tieredCrossbowPose(ItemStack stack, Object item, Operation<Boolean> original) {
        return LauncherClient.isLauncher(stack, item, original.call(stack, item));
    }
}
//?}
