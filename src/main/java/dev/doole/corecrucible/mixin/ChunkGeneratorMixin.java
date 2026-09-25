package dev.doole.corecrucible.mixin;

import dev.doole.corecrucible.worldgen.OreVeins;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Ore veins go in before the chunk's other decoration, so structures and cave features cut through them.
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    // HEAD = at the very start of decoration, before vanilla places any feature in this chunk.
    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    private void dooles_core_crucible$oreVeins(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        OreVeins.generate(level, chunk, (ChunkGenerator) (Object) this);
    }
}
