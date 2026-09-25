package dev.doole.corecrucible.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Cavernous Echo builds client-only glowing block displays.
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setTransformation")
    void dooles_core_crucible$setTransformation(Transformation transformation);

    @Invoker("setGlowColorOverride")
    void dooles_core_crucible$setGlowColorOverride(int color);
}
