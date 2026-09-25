package dev.doole.corecrucible.enchant;

import dev.doole.corecrucible.CoreCrucible;
import dev.doole.corecrucible.registry.ModEnchantments;
import dev.doole.corecrucible.registry.ModItems;
import dev.doole.corecrucible.registry.ModTiers;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Vein Resonance, ported from Applied-Enchantments: while the player holds the Vein Resonance key,
 * breaking a block also breaks the rest of the {@link VeinShape} they picked. The client reports the key and shape
 * with {@link ModePayload}; the same {@link #plan} runs on the client for the preview outline and on the server for
 * the break.
 */
public final class VeinMining {
    /** Vein limit for tools outside the tier chain. */
    // RADIUS: how far (in blocks) from the first block a vein or tunnel can reach.
    private static final int DEFAULT_LIMIT = 16;
    private static final int RADIUS = 16;
    public static final TagKey<Block> VEIN_MINEABLE = TagKey.create(Registries.BLOCK, CoreCrucible.id("vein_mineable"));

    /** Sent by the client whenever the Vein Resonance key or its selected shape changes. */
    public record ModePayload(boolean active, int shape) implements CustomPacketPayload {
        public static final Type<ModePayload> TYPE = new Type<>(CoreCrucible.id("vein_mode"));
        public static final StreamCodec<ByteBuf, ModePayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ModePayload::active, ByteBufCodecs.VAR_INT, ModePayload::shape, ModePayload::new);

        @Override
        public Type<ModePayload> type() {
            return TYPE;
        }
    }

    // The latest key state and shape each online player's client has sent.
    private static final Map<UUID, ModePayload> MODES = new HashMap<>();

    private VeinMining() {
    }

    /** Server side, on the server thread. */
    public static void onModePayload(ServerPlayer player, ModePayload payload) {
        MODES.put(player.getUUID(), payload);
    }

    /** Drops the modes of players who left. */
    public static void cleanUp(MinecraftServer server) {
        MODES.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    public static void clear() {
        MODES.clear();
    }

    /** The shape the player's key is held for, or null while it's released. */
    public static VeinShape activeShape(Player player, int enchantmentLevel) {
        ModePayload mode = MODES.get(player.getUUID());
        return mode != null && mode.active() ? VeinShape.effective(mode.shape(), enchantmentLevel) : null;
    }

    public static int level(Level level, ItemStack tool) {
        return EnchantmentHooks.level(level, ModEnchantments.VEIN_RESONANCE, tool);
    }

    /** Most extra blocks one break may take: 8 × (harvest level + 2) for chain tools, scaled by level (III is full). */
    public static int limit(ItemStack tool, int enchantmentLevel) {
        ModTiers tier = ModItems.tierOf(tool.getItem());
        int base = tier != null ? 8 * (tier.harvestLevel() + 2) : DEFAULT_LIMIT;
        return Math.max(1, Math.round(base * enchantmentLevel / 3.0F));
    }

    private static int radius(int enchantmentLevel) {
        return Math.max(RADIUS, Math.round(RADIUS * enchantmentLevel / 3.0F));
    }

    /**
     * The face of {@code pos} the player is looking at. The server calls this after the block is gone, so it traces
     * against a full cube rather than the world.
     */
    public static Direction facing(Player player, BlockPos pos) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 2.0));
        BlockHitResult hit = Shapes.block().clip(eye, end, pos);
        return hit != null ? hit.getDirection() : Direction.orderedByNearest(player)[0].getOpposite();
    }

    /**
     * Every extra block the break of {@code origin} (which held {@code state}) takes along, nearest first, not
     * counting {@code origin}. Empty when the tool can't harvest {@code state}.
     */
    public static List<BlockPos> plan(Level level, Player player, BlockPos origin, BlockState state, Direction face,
                                      ItemStack tool, int enchantmentLevel, VeinShape shape) {
        if (enchantmentLevel <= 0 || state.isAir() || !player.hasCorrectToolForDrops(state)) {
            return List.of();
        }
        // How many extra blocks, and how far, this tool and level allow.
        int limit = limit(tool, enchantmentLevel);
        int radius = radius(enchantmentLevel);
        if (shape == VeinShape.SHAPELESS) {
            return connected(level, origin, state, limit, radius);
        }

        // Work out directions relative to the player: "into" goes into the face they hit; forward is their facing when they
        // mine straight up or down.
        Direction into = face.getOpposite();
        Direction forward = into.getAxis().isHorizontal() ? into : player.getDirection();
        // The cross-section of a tunnel: "right" and "up" as the player sees the face.
        Direction up = into.getAxis().isHorizontal() ? Direction.UP : forward;
        Direction right = into.getAxis().isHorizontal() ? into.getClockWise() : forward.getClockWise();

        List<BlockPos> candidates = new ArrayList<>();
        switch (shape) {
            case SMALL_TUNNEL -> {
                for (int d = 0; d < radius; d++) candidates.add(origin.relative(into, d));
            }
            case SMALL_SQUARE -> addSquare(candidates, origin, right, up);
            case LARGE_TUNNEL -> {
                for (int d = 0; d < radius; d++) addSquare(candidates, origin.relative(into, d), right, up);
            }
            case MINING_TUNNEL -> {
                // Two tall: the mined block plus the one that lines the tunnel up with the player's feet and head.
                Direction second = into.getAxis().isVertical() ? forward
                        : origin.getY() > player.getBlockY() ? Direction.DOWN : Direction.UP;
                for (int d = 0; d < radius; d++) {
                    BlockPos step = origin.relative(into, d);
                    candidates.add(step);
                    candidates.add(step.relative(second));
                }
            }
            case ESCAPE_TUNNEL, MINESHAFT -> {
                // Climbs (or descends) one block per step from the player's feet, clearing three blocks of headroom.
                BlockPos base = new BlockPos(origin.getX(), player.getBlockY(), origin.getZ());
                int rise = shape == VeinShape.ESCAPE_TUNNEL ? 1 : -1;
                for (int d = 0; d < radius; d++) {
                    BlockPos step = base.relative(forward, d).above(d * rise);
                    candidates.add(step);
                    candidates.add(step.above());
                    candidates.add(step.above(2));
                }
            }
            default -> {
            }
        }

        // Keep candidates this tool can actually mine, nearest first, up to the limit.
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(origin);
        for (BlockPos pos : candidates) {
            if (result.size() >= limit) break;
            if (seen.add(pos) && canShapeMine(level, player, pos, state, tool)) result.add(pos);
        }
        return result;
    }

    private static void addSquare(List<BlockPos> out, BlockPos centre, Direction right, Direction up) {
        out.add(centre);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a != 0 || b != 0) out.add(centre.relative(right, a).relative(up, b));
            }
        }
    }

    /** Tunnels and squares take the mined kind of block and anything else the tool is made for (a pickaxe: stone). */
    private static boolean canShapeMine(Level level, Player player, BlockPos pos, BlockState origin, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0.0F || !player.hasCorrectToolForDrops(state)) {
            return false;
        }
        return sameVein(origin, state) || tool.getDestroySpeed(state) > 1.0F;
    }

    // Shapeless: a flood fill. Starting at the mined block, keep adding touching blocks (diagonals too) of the same kind,
    // until the limit or the radius is reached.
    private static List<BlockPos> connected(Level level, BlockPos origin, BlockState state, int limit, int radius) {
        List<BlockPos> vein = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && vein.size() < limit) {
            BlockPos current = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                if (vein.size() >= limit) break;
                if (Math.abs(next.getX() - origin.getX()) > radius || Math.abs(next.getY() - origin.getY()) > radius
                        || Math.abs(next.getZ() - origin.getZ()) > radius) {
                    continue;
                }
                BlockPos immutable = next.immutable();
                if (seen.add(immutable) && sameVein(state, level.getBlockState(immutable))) {
                    vein.add(immutable);
                    queue.add(immutable);
                }
            }
        }
        return vein;
    }

    /** Same block, or both in one ore / log family (e.g. stone and deepslate iron ore, oak log and oak wood). */
    @SuppressWarnings("deprecation")
    private static boolean sameVein(BlockState origin, BlockState other) {
        if (other.isAir()) return false;
        if (origin.getBlock() == other.getBlock()) return true;
        if (!other.is(VEIN_MINEABLE)) return false;
        for (TagKey<Block> tag : origin.getBlock().builtInRegistryHolder().tags().toList()) {
            if (isFamilyTag(tag) && other.is(tag)) return true;
        }
        return false;
    }

    // Tags that group blocks into one "family" for vein mining: the c:ores tags (e.g. iron ore and deepslate iron ore) and
    // vanilla's *_ores, *_logs and *_stems tags.
    private static boolean isFamilyTag(TagKey<Block> tag) {
        String namespace = tag.location().getNamespace();
        String path = tag.location().getPath();
        if (namespace.equals("c")) return path.startsWith("ores/");
        return namespace.equals("minecraft") && !path.equals("overworld_natural_logs")
                && (path.endsWith("_ores") || path.endsWith("_logs") || path.endsWith("_stems"));
    }
}
