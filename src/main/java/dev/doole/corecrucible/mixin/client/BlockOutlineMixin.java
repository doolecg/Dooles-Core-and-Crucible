package dev.doole.corecrucible.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doole.corecrucible.client.VeinMiningClient;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import net.minecraft.client.renderer.extract.LevelExtractor;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}

// Vein Resonance preview: the vanilla block outline grows to cover every block the next break would take.
//? if >=1.21.2 {
@Mixin(LevelExtractor.class)
//?} else {
/*@Mixin(LevelRenderer.class)
*///?}
public abstract class BlockOutlineMixin {
    //? if >=1.21.2 {
    @ModifyExpressionValue(method = "extractBlockOutline", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    //?} else {
    /*@ModifyExpressionValue(method = "renderHitOutline", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    *///?}
    // Swaps the shape of the targeted block's outline for one covering all the blocks Vein Resonance would break.
    private VoxelShape dooles_core_crucible$veinPreview(VoxelShape original) {
        return VeinMiningClient.outlineShape(original);
    }
}
