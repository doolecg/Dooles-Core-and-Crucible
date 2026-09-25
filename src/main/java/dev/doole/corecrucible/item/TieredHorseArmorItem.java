package dev.doole.corecrucible.item;

//? if <1.21.2 {

/*import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.registry.BodyArmorStats;
import dev.doole.corecrucible.registry.ModTiers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/^*
 * 1.21.1 horse armor in the chain. The armor points come from {@link BodyArmorStats}, not the material,
 * and the texture is the mod's own: 1.21.1 builds the path from the material's id, which would point the backported
 * Netherite horse armor at a vanilla texture that doesn't exist. 26.x uses {@code Item.Properties#horseArmor}.
 ^/
public class TieredHorseArmorItem extends AnimalArmorItem {
    private final ModTiers tier;
    private final Identifier texture;
    private ItemAttributeModifiers modifiers;

    public TieredHorseArmorItem(ModTiers tier, Properties properties) {
        super(ArmorMaterials.IRON, BodyType.EQUESTRIAN, false, properties);
        this.tier = tier;
        this.texture = CoreCrucible.id("textures/entity/equipment/horse_body/" + tier.prefix() + ".png");
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }

    @Override
    public int getDefense() {
        return BodyArmorStats.of(tier).horseDefense();
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (modifiers == null) {
            modifiers = ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(Identifier.withDefaultNamespace("armor.body"), getDefense(),
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY)
                    .build();
        }
        return modifiers;
    }
}
*///?}
