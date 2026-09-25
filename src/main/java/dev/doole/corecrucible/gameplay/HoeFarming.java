package dev.doole.corecrucible.gameplay;

import dev.doole.corecrucible.registry.EquipType;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hoe farming stats: a till area, a harvest bonus on mature crops, and replanting from Iron up.
 * The table and offsets are pure and unit-tested; the hooks are called by {@code ItemStackUseOnMixin},
 * {@code BlockDropsMixin} and {@code ServerPlayerGameModeMixin}.
 */
public final class HoeFarming {

    // How much ground one right-click tills. The label is what the tooltip shows.
    public enum Area {
        SINGLE("1×1"), ROW("3×1"), SQUARE("3×3");

        private final String label;

        Area(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Set while the extra blocks are tilled, so those uses don't spread again. */
    private static boolean tilling;
    /** Crops to put back at age 0 once the player's break finishes, by position. */
    private static final Map<BlockPos, BlockState> REPLANT = new HashMap<>();

    private HoeFarming() {
    }

    // Till area per tier: Obsidian and up 3×3, Iron to Diamond a 3-wide row, below Iron a single block. These hoe numbers
    // aren't in the config file; change them here.
    public static Area area(ModTiers tier) {
        if (tier.ordinal() >= ModTiers.OBSIDIAN.ordinal()) return Area.SQUARE;
        if (tier.ordinal() >= ModTiers.IRON.ordinal()) return Area.ROW;
        return Area.SINGLE;
    }

    /** Extra produce on a mature crop, as a fraction of what dropped. */
    // Edit these to change each hoe tier's extra crop drops (0.25 = a quarter more).
    public static double harvestBonus(ModTiers tier) {
        return switch (tier) {
            case WOOD -> 0.0;
            case BONE, FLINT -> 0.05;
            case COPPER -> 0.10;
            case IRON -> 0.15;
            case EMERALD -> 0.20;
            case DIAMOND -> 0.25;
            case OBSIDIAN -> 0.30;
            case NETHERITE -> 0.35;
            case REINFORCED -> 0.50;
        };
    }

    // Iron hoes and up replant the crop they harvest.
    public static boolean replants(ModTiers tier) {
        return tier.ordinal() >= ModTiers.IRON.ordinal();
    }

    /**
     * The (x, z) offsets tilled besides the clicked block. A row runs along ({@code sideX}, {@code sideZ}),
     * the direction to the player's right, so it's perpendicular to where they face.
     */
    public static List<int[]> extraOffsets(Area area, int sideX, int sideZ) {
        List<int[]> out = new ArrayList<>();
        switch (area) {
            case SINGLE -> {
            }
            case ROW -> {
                out.add(new int[]{-sideX, -sideZ});
                out.add(new int[]{sideX, sideZ});
            }
            case SQUARE -> {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx != 0 || dz != 0) out.add(new int[]{dx, dz});
                    }
                }
            }
        }
        return out;
    }

    /** Bonus produce for {@code dropped} items, rounded up with probability equal to the fraction. */
    public static int bonusCount(int dropped, double bonus, double roll) {
        if (dropped <= 0 || bonus <= 0) return 0;
        return roundRandom(dropped * bonus, roll);
    }

    /** {@code floor(value)}, plus 1 when {@code roll} (in [0, 1)) is below the fractional part. */
    static int roundRandom(double value, double roll) {
        int whole = (int) Math.floor(value);
        return roll < value - whole ? whole + 1 : whole;
    }

    // ---- Hooks ------------------------------------------------------------------------------------------------

    private static ModTiers hoeTier(ItemStack stack) {
        Item item = stack.getItem();
        return ModItems.typeOf(item) == EquipType.HOE ? ModItems.tierOf(item) : null;
    }

    /** After a hoe's use on a block: tills the rest of the tier's area. Sneaking tills one block. */
    public static void afterUseOn(ItemStack stack, UseOnContext context, InteractionResult result) {
        if (tilling || !result.consumesAction() || !(context.getLevel() instanceof ServerLevel)) return;
        Player player = context.getPlayer();
        ModTiers tier = hoeTier(stack);
        if (player == null || player.isShiftKeyDown() || tier == null) return;
        // The player's right-hand direction, so a row runs sideways across their view.
        Direction side = player.getDirection().getClockWise();
        BlockPos origin = context.getClickedPos();
        tilling = true;
        try {
            for (int[] offset : extraOffsets(area(tier), side.getStepX(), side.getStepZ())) {
                // Stop if the hoe broke (or was swapped) partway through.
                if (player.getItemInHand(context.getHand()) != stack || stack.isEmpty()) break;
                BlockPos pos = origin.offset(offset[0], 0, offset[1]);
                Vec3 hit = context.getClickLocation().add(offset[0], 0, offset[1]);
                // Re-uses the hoe's own right-click on each extra block, so vanilla's rules (what can be tilled, durability loss)
                // apply to every one.
                stack.getItem().useOn(new UseOnContext(player, context.getHand(), new BlockHitResult(hit, context.getClickedFace(), pos, false)));
            }
        } finally {
            tilling = false;
        }
    }

    /** Adds the harvest bonus to a mature crop's drops and takes a seed back for replanting. Null when unchanged. */
    public static List<ItemStack> modifyDrops(BlockState state, BlockPos pos, ItemStack tool, List<ItemStack> drops, ServerLevel level) {
        ModTiers tier = tool == null ? null : hoeTier(tool);
        if (tier == null || !isMatureCrop(state)) return null;
        // A crop block's item is its seed (wheat seeds for wheat). Anything else it drops is the produce (wheat).
        Item seed = state.getBlock().asItem();
        Item produce = null;
        int produced = 0;
        for (ItemStack drop : drops) {
            if (!drop.is(seed) && (produce == null || drop.is(produce))) {
                produce = drop.getItem();
                produced += drop.getCount();
            }
        }
        if (produce == null) {
            // Carrots, potatoes, nether wart, cocoa: the produce is the seed.
            produce = seed;
            for (ItemStack drop : drops) {
                if (drop.is(seed)) produced += drop.getCount();
            }
        }
        List<ItemStack> out = new ArrayList<>(drops);
        int bonus = bonusCount(produced, harvestBonus(tier), level.getRandom().nextDouble());
        if (bonus > 0) out.add(new ItemStack(produce, bonus));
        // Keep one seed back from the drops; afterBreak plants it once the block is actually gone.
        if (replants(tier)) {
            for (ItemStack drop : out) {
                if (drop.is(seed)) {
                    drop.shrink(1);
                    REPLANT.put(pos.immutable(), youngest(state));
                    break;
                }
            }
            out.removeIf(ItemStack::isEmpty);
        }
        return out;
    }

    /** At the end of a player's block break: puts a harvested crop back at age 0. */
    public static void afterBreak(ServerLevel level, BlockPos pos) {
        BlockState crop = REPLANT.remove(pos);
        if (crop != null && level.getBlockState(pos).isAir() && crop.canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, crop);
        }
        // Drops computed outside a player break (e.g. by another mod) never get consumed.
        if (REPLANT.size() > 64) REPLANT.clear();
    }

    // Only fully grown crops, nether wart and cocoa get the bonus and the replant.
    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        return false;
    }

    /** The same block at age 0, keeping its other properties (cocoa's facing). */
    private static BlockState youngest(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) return state.setValue(age, 0);
        }
        return state.getBlock().defaultBlockState();
    }
}
