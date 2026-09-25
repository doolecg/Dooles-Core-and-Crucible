package dev.doole.corecrucible.mixin;

//? if >=1.21.2 {
import dev.doole.corecrucible.inventory.SmithingRecipePlacer;
import dev.doole.corecrucible.recipe.SmithingRequirements;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The smithing recipe book sends vanilla's place-recipe packet, but vanilla only places into a
 * {@code RecipeBookMenu}. For the smithing table this does the same checks, then fills the table or answers with a
 * ghost recipe, as vanilla does for the crafting table.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlaceRecipe", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    // Runs just after vanilla's first bookkeeping in handlePlaceRecipe. For any other menu, return and let vanilla go on.
    private void dooles_core_crucible$placeSmithing(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (!(player.containerMenu instanceof SmithingMenu)) return;
        ci.cancel();
        // The same sanity checks vanilla makes: the right open menu, the player can still use it, and the recipe is one they
        // have unlocked.
        if (player.isSpectator() || player.containerMenu.containerId != packet.containerId() || !player.containerMenu.stillValid(player)) return;
        RecipeManager.ServerDisplayInfo info = player.level().getServer().getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (info == null || !player.getRecipeBook().contains(info.parent().id())) return;
        SmithingRequirements req = SmithingRequirements.of(info.parent().value());
        if (req == null) return;
        // Missing items: send the recipe back as a ghost so the client shows what's needed.
        if (!SmithingRecipePlacer.place(player, req, packet.useMaxItems())) {
            player.connection.send(new ClientboundPlaceGhostRecipePacket(player.containerMenu.containerId, info.display().display()));
        }
    }
}
//?}
