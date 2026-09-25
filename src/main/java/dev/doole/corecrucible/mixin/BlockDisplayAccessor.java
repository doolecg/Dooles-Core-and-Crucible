package dev.doole.corecrucible.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Invoker mixin: lets Cavernous Echo set which block a block display entity shows (the method is private in vanilla).
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
    @Invoker("setBlockState")
    void dooles_core_crucible$setBlockState(BlockState state);
}
