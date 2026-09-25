package dev.doole.corecrucible.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
// Whether the arrow has stuck in a block. Same meaning on both versions, only the vanilla name differs.
//? if >=1.21.2 {
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.gen.Invoker;
//?} else {
/*import net.minecraft.world.entity.projectile.AbstractArrow;
*///?}

// Volatile Payload detonates arrows that land. 1.21.1 has a field, 26.x a method.
// Tiered launchers scale the base damage; 26.x has no getter for it.
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    // The arrow's damage before velocity is applied; LauncherShots multiplies it by the launcher tier's damage multiplier.
    @Accessor("baseDamage")
    double dooles_core_crucible$baseDamage();

    //? if >=1.21.2 {
    @Invoker("isInGround")
    boolean dooles_core_crucible$inGround();
    //?} else {
    /*@Accessor("inGround")
    boolean dooles_core_crucible$inGround();
    *///?}
}
