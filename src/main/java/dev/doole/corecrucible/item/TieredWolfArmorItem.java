package dev.doole.corecrucible.item;

//? if <1.21.2 {

/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/^*
 * 1.21.1 wolf armor in the chain: vanilla's armadillo armor points, the tier's durability and repair tag,
 * and the mod's own texture (1.21.1 gives every wolf armor in one namespace the same texture path). 26.x uses
 * {@code Item.Properties#wolfArmor}. {@code WolfArmorMixin} lets wolves wear, repair and absorb hits with it.
 ^/
public class TieredWolfArmorItem extends AnimalArmorItem {
    private final ModTiers tier;
    private final Identifier texture;

    public TieredWolfArmorItem(ModTiers tier, Properties properties) {
        super(ArmorMaterials.ARMADILLO, BodyType.CANINE, false, properties);
        this.tier = tier;
        this.texture = CoreCrucible.id("textures/entity/equipment/wolf_body/" + tier.prefix() + ".png");
    }

    public ModTiers tier() {
        return tier;
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem) {
        return repairItem.is(tier.repairItems());
    }
}
*///?}
