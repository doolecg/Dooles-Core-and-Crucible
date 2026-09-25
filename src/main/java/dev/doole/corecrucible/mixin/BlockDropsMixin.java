package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.gameplay.HoeFarming;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.2 {
import net.minecraft.world.item.ItemInstance;
//?}

// Hoe harvest bonus and replant seed: the drops of a block broken with a tool.
// Runs after vanilla has worked out a block's drops. HoeFarming returns a new list (extra crop drops, minus the seed it
// keeps back for replanting) or null to leave the drops unchanged. The two versions differ only in the tool's type.
@Mixin(Block.class)
public abstract class BlockDropsMixin {
    //? if >=1.21.2 {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void dooles_core_crucible$hoeHarvest(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                                  Entity breaker, ItemInstance tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(tool instanceof ItemStack stack)) return;
    //?} else {
    /*@Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void dooles_core_crucible$hoeHarvest(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
                                                  Entity breaker, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> cir) {
    *///?}
        List<ItemStack> drops = HoeFarming.modifyDrops(state, pos, stack, cir.getReturnValue(), level);
        if (drops != null) cir.setReturnValue(drops);
    }
}
