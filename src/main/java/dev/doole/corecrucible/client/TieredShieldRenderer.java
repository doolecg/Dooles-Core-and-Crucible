package dev.doole.corecrucible.client;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.item.TieredShieldItem;
import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
//? if >=1.21.2 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponentMap;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.doole.corecrucible.mixin.client.ItemPropertiesInvoker;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
*///?}

/**
 * Draws tiered shields with vanilla's shield model and the tier's own texture, stitched into vanilla's
 * shield atlas from {@code textures/entity/shield/<tier>_base.png} and {@code <tier>_base_nopattern.png}.
 *
 * <p>26.x: a {@code dooles_core_crucible:shield} special model renderer, named in each shield's item definition with a
 * {@code tier} field. It is vanilla's own {@code ShieldSpecialRenderer}, handed a sprite lookup that swaps vanilla's
 * two base sprites for the tier's, so banners, glint and both 26.x versions work exactly like the vanilla shield.
 * 1.21.1: a built-in item renderer copied from vanilla's shield branch in {@code BlockEntityWithoutLevelRenderer},
 * registered per loader (Fabric {@code BuiltinItemRendererRegistry}, NeoForge {@code IClientItemExtensions}).
 */
public final class TieredShieldRenderer {
    /** The special model renderer type used in the item definitions. */
    public static final Identifier ID = CoreCrucible.id("shield");

    private TieredShieldRenderer() {
    }

    /** Every tiered shield item (the vanilla one is Wood and keeps vanilla's renderer). */
    public static List<Item> shields() {
        List<Item> out = new ArrayList<>();
        for (ModTiers tier : ModTiers.values()) {
            if (ModItems.equipment(tier, EquipType.SHIELD) instanceof TieredShieldItem shield) out.add(shield);
        }
        return out;
    }

    /** The shield-atlas sprite for a tier: {@code dooles_core_crucible:entity/shield/<tier>_base[_nopattern]}. */
    static Identifier sprite(ModTiers tier, boolean patterned) {
        return CoreCrucible.id("entity/shield/" + tier.prefix() + (patterned ? "_base" : "_base_nopattern"));
    }

    //? if >=1.21.2 {
    /** The item definition's {@code {"type": "dooles_core_crucible:shield", "tier": "iron"}}. */
    public record Unbaked(String tier) implements SpecialModelRenderer.Unbaked<DataComponentMap> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("tier").forGetter(Unbaked::tier)).apply(i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<DataComponentMap> bake(SpecialModelRenderer.BakingContext context) {
            ModTiers modTier = tierOf(tier);
            SpriteId patterned = new SpriteId(Sheets.SHIELD_SHEET, sprite(modTier, true));
            SpriteId plain = new SpriteId(Sheets.SHIELD_SHEET, sprite(modTier, false));
            SpriteGetter vanilla = context.sprites();
            // Hand vanilla's shield renderer a sprite lookup that answers "shield base" with this tier's texture instead.
            SpriteGetter sprites = id -> {
                if (id.equals(Sheets.SHIELD_BASE)) return vanilla.get(patterned);
                if (id.equals(Sheets.SHIELD_BASE_NO_PATTERN)) return vanilla.get(plain);
                return vanilla.get(id);
            };
            return new ShieldSpecialRenderer(sprites, new ShieldModel(context.entityModelSet().bakeLayer(ModelLayers.SHIELD)));
        }
    }

    private static ModTiers tierOf(String prefix) {
        for (ModTiers tier : ModTiers.values()) {
            if (tier.prefix().equals(prefix)) return tier;
        }
        throw new IllegalArgumentException("Unknown shield tier: " + prefix);
    }
    //?} else {
    /*private static ShieldModel model;

    /^* 1.21.1: vanilla's "blocking" item-model property, for the raised pose, on every tiered shield. ^/
    public static void registerItemProperties() {
        for (Item shield : shields()) {
            ItemPropertiesInvoker.dooles_core_crucible$register(shield, Identifier.withDefaultNamespace("blocking"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        }
    }

    /^* 1.21.1: vanilla's shield rendering with the tier's texture. ^/
    public static void render(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof TieredShieldItem shield)) return;
        if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        BannerPatternLayers patterns = stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        DyeColor baseColor = stack.get(DataComponents.BASE_COLOR);
        boolean patterned = !patterns.layers().isEmpty() || baseColor != null;
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        Material material = new Material(Sheets.SHIELD_SHEET, sprite(shield.tier(), patterned));
        VertexConsumer consumer = material.sprite()
                .wrap(ItemRenderer.getFoilBufferDirect(buffers, model.renderType(material.atlasLocation()), true, stack.hasFoil()));
        model.handle().render(pose, consumer, light, overlay);
        if (patterned) {
            BannerRenderer.renderPatterns(pose, buffers, light, overlay, model.plate(), material, false,
                    Objects.requireNonNullElse(baseColor, DyeColor.WHITE), patterns, stack.hasFoil());
        } else {
            model.plate().render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }
    *///?}
}
