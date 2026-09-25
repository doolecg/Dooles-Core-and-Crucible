package dev.doole.corecrucible.mixin.client;

//? if <1.21.2 {

/*import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// 1.21.1: model properties for the tiered bows and crossbows. The method is private in vanilla.
@Mixin(ItemProperties.class)
public interface ItemPropertiesInvoker {
    @Invoker("register")
    static void dooles_core_crucible$register(Item item, Identifier id, ClampedItemPropertyFunction function) {
        throw new AssertionError();
    }
}
*///?}
