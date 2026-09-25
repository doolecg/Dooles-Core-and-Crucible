package dev.doole.corecrucible.mixin;

//? if <1.21.2 {

/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doole.corecrucible.gameplay.ShieldGuard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/^*
 * 1.21.1 shields. An axe puts the shield in use on a fixed 100-tick cooldown: the tier's time instead,
 * on the shield actually raised (vanilla names Items.SHIELD there; NeoForge already names the item in use). Blocking
 * wears only Items.SHIELD down on Fabric; NeoForge already checks canPerformAction, hence require = 0.
 * 26.x reads both from the shield's blocks_attacks component.
 ^/
@Mixin(Player.class)
public abstract class PlayerShieldMixin {

    @WrapOperation(method = "disableShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V"))
    private void dooles_core_crucible$tieredDisable(ItemCooldowns cooldowns, Item item, int ticks, Operation<Void> original) {
        ItemStack using = ((Player) (Object) this).getUseItem();
        if (using.getItem() instanceof ShieldItem) {
            original.call(cooldowns, using.getItem(), ShieldGuard.disableTicks(using, ticks));
        } else {
            original.call(cooldowns, item, ticks);
        }
    }

    @WrapOperation(method = "hurtCurrentlyUsedShield",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private boolean dooles_core_crucible$tieredShieldWear(ItemStack stack, Item item, Operation<Boolean> original) {
        return original.call(stack, item) || (item == Items.SHIELD && stack.getItem() instanceof ShieldItem);
    }
}
*///?}
