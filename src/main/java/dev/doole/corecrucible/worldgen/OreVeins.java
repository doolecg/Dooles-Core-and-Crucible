package dev.doole.corecrucible.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Overworld ores generate as long veins, like vanilla's large iron and copper veins, instead of small blobs.
 * Vanilla's own ore placements are switched off by data overrides ({@code count: 0}, written by
 * {@code tools/gen_data.py}); each {@link VeinKind} stands in for one of them and only runs in the biomes that still list
 * that placement, so biome-specific ores (Emerald in mountains, extra Gold in badlands, big Copper in dripstone
 * caves) stay where they were. Vanilla's large iron and copper veins are untouched.
 *
 * <p>Between the veins, each kind also scatters a few small pockets (1 to {@link VeinKind#pocketSize()} ore blocks) per
 * chunk, like vanilla's blobs but smaller, so ore turns up everywhere and the big veins can stay rare.
 *
 * <p>{@code ChunkGeneratorMixin} calls {@link #generate} at the start of each chunk's decoration, so structures and
 * cave features placed afterwards cut through the veins naturally.
 */
public final class OreVeins {

    /** The blocks one vein kind is made of. Air for {@code raw} or {@code filler} means none. */
    record VeinBlocks(Block stoneOre, Block deepOre, Block raw, Block filler) {
    }

    // Fillers: iron keeps vanilla's tuff and copper its granite; the others get a rock of their own.
    // Which blocks each vein kind is made of: stone ore, deepslate ore, raw-ore block (AIR = none) and filler rock.
    private static VeinBlocks blocks(VeinKind kind) {
        return switch (kind) {
            case COAL_UPPER, COAL_LOWER -> new VeinBlocks(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.AIR, Blocks.ANDESITE);
            case IRON_UPPER, IRON_MIDDLE, IRON_SMALL -> new VeinBlocks(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.TUFF);
            case COPPER, COPPER_LARGE -> new VeinBlocks(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK, Blocks.GRANITE);
            case GOLD, GOLD_EXTRA -> new VeinBlocks(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.RAW_GOLD_BLOCK, Blocks.SMOOTH_BASALT);
            case REDSTONE -> new VeinBlocks(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.AIR, Blocks.GRANITE);
            case LAPIS -> new VeinBlocks(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.AIR, Blocks.DIORITE);
            case DIAMOND -> new VeinBlocks(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.AIR, Blocks.CALCITE);
            case EMERALD -> new VeinBlocks(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.AIR, Blocks.CALCITE);
        };
    }

    private static final VeinKind[] KINDS = VeinKind.values();

    /** Samplers per world seed. Worlds share a seed across dimensions, so this stays tiny. */
    private static final Map<Long, VeinShape.Sampler[]> SAMPLERS = new ConcurrentHashMap<>();

    private OreVeins() {
    }

    // Builds (once per world seed) the noise for every vein kind. Cached because it's needed for every chunk.
    private static VeinShape.Sampler[] samplers(long seed) {
        if (SAMPLERS.size() > 8) SAMPLERS.clear();
        return SAMPLERS.computeIfAbsent(seed, s -> {
            VeinShape.Sampler[] out = new VeinShape.Sampler[KINDS.length];
            for (int i = 0; i < out.length; i++) out[i] = KINDS[i].sampler(s);
            return out;
        });
    }

    /** Places every vein that passes through this chunk. */
    public static void generate(WorldGenLevel level, ChunkAccess chunk, ChunkGenerator generator) {
        VeinShape.Sampler[] samplers = samplers(level.getSeed());
        ChunkPos chunkPos = chunk.getPos();
        int baseX = chunkPos.getMinBlockX();
        int baseZ = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        var placedFeatures = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);

        for (int i = 0; i < samplers.length; i++) {
            VeinKind kind = KINDS[i];
            VeinShape.Sampler sampler = samplers[i];
            // The vanilla ore placement this kind replaces. Its biome list decides where the veins may go.
            ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(kind.replaces));
            Optional<? extends Holder<PlacedFeature>> gate = placedFeatures.get(key);
            if (gate.isEmpty()) continue;
            PlacedFeature gateFeature = gate.get().value();
            VeinBlocks blocks = blocks(kind);

            double rarity = kind.shape().rarity();
            // In the kind's favored biome (forests for Coal, dripstone caves for Iron) the threshold is lower: more veins.
            double loosest = kind.favored == VeinKind.Favored.NONE ? rarity : Math.min(rarity, kind.favoredRarity());
            // Walk the vein's height range in 4×4×4 cells, the size of a biome cell.
            for (int cellY = Math.floorDiv(kind.shape().minY(), 4); cellY <= Math.floorDiv(kind.shape().maxY(), 4); cellY++) {
                int y0 = cellY * 4;
                if (level.isOutsideBuildHeight(y0) || chunk.getSection(chunk.getSectionIndex(y0)).hasOnlyAir()) continue;
                for (int cellX = 0; cellX < 4; cellX++) {
                    for (int cellZ = 0; cellZ < 4; cellZ++) {
                        int x0 = baseX + cellX * 4;
                        int z0 = baseZ + cellZ * 4;
                        if (!sampler.mayContain(x0 + 2, y0 + 2, z0 + 2, loosest)) continue;
                        pos.set(x0 + 2, y0 + 2, z0 + 2);
                        Holder<Biome> biome = level.getBiome(pos);
                        // Skip cells in biomes that don't have this ore in vanilla.
                        if (!generator.getBiomeGenerationSettings(biome).hasFeature(gateFeature)) continue;
                        double cellRarity = isFavored(kind.favored, biome) ? loosest : rarity;
                        if (cellRarity != loosest && !sampler.mayContain(x0 + 2, y0 + 2, z0 + 2, cellRarity)) continue;
                        placeCell(level, chunk, blocks, sampler, cellRarity, pos, x0, y0, z0);
                    }
                }
            }
            placePockets(level, chunk, generator, kind, gateFeature, blocks, pos, baseX, baseZ, i);
        }
    }

    /** Scatters this kind's small pockets through the chunk. Seeded by world, chunk and kind, so it's repeatable. */
    private static void placePockets(WorldGenLevel level, ChunkAccess chunk, ChunkGenerator generator, VeinKind kind,
                                     PlacedFeature gateFeature, VeinBlocks blocks, BlockPos.MutableBlockPos pos,
                                     int baseX, int baseZ, int kindIndex) {
        Random random = new Random(VeinNoise.mix(level.getSeed() ^ ((long) baseX << 32) ^ (baseZ & 0xFFFFFFFFL)) + kindIndex);
        double pockets = kind.pockets();
        // e.g. 2.3 pockets per chunk = 2 pockets, plus a 30% chance of a third.
        int count = (int) pockets + (random.nextDouble() < pockets % 1 ? 1 : 0);
        int minY = kind.shape().minY();
        int height = kind.shape().maxY() - minY + 1;
        for (int n = 0; n < count; n++) {
            int x = baseX + random.nextInt(16);
            int y = minY + random.nextInt(height);
            int z = baseZ + random.nextInt(16);
            int size = 1 + random.nextInt(kind.pocketSize());
            if (level.isOutsideBuildHeight(y) || chunk.getSection(chunk.getSectionIndex(y)).hasOnlyAir()) continue;
            pos.set(x, y, z);
            if (!generator.getBiomeGenerationSettings(level.getBiome(pos)).hasFeature(gateFeature)) continue;
            placePocket(level, chunk, blocks, pos, random, size, minY, minY + height - 1);
        }
    }

    private static final int[][] SIDES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /**
     * Grows a pocket of {@code size} touching blocks from {@code start}, kept inside the chunk and the kind's height
     * range, and turns them to ore.
     */
    private static void placePocket(WorldGenLevel level, ChunkAccess chunk, VeinBlocks blocks, BlockPos.MutableBlockPos start,
                                    Random random, int size, int minY, int maxY) {
        int minX = start.getX() & ~15;
        int minZ = start.getZ() & ~15;
        List<BlockPos> cells = new ArrayList<>(size);
        cells.add(start.immutable());
        for (int tries = 0; cells.size() < size && tries < size * 6; tries++) {
            BlockPos from = cells.get(random.nextInt(cells.size()));
            int[] side = SIDES[random.nextInt(SIDES.length)];
            BlockPos next = from.offset(side[0], side[1], side[2]);
            if (next.getX() < minX || next.getX() > minX + 15 || next.getZ() < minZ || next.getZ() > minZ + 15) continue;
            if (next.getY() < minY || next.getY() > maxY || level.isOutsideBuildHeight(next.getY()) || cells.contains(next)) continue;
            cells.add(next);
        }
        for (BlockPos cell : cells) {
            Block ore = oreFor(chunk.getBlockState(cell), cell.getY(), blocks);
            if (ore != null) level.setBlock(cell, ore.defaultBlockState(), 2);
        }
    }

    private static boolean isFavored(VeinKind.Favored favored, Holder<Biome> biome) {
        return switch (favored) {
            case FOREST -> biome.is(BiomeTags.IS_FOREST);
            case DRIPSTONE -> biome.is(Biomes.DRIPSTONE_CAVES);
            case NONE -> false;
        };
    }

    // Places the vein's blocks in one 4×4×4 cell: each block is asked whether it's ore, raw ore, filler or nothing.
    // Only stone-like blocks are replaced (see oreFor), so caves, water and dirt are left alone.
    private static void placeCell(WorldGenLevel level, ChunkAccess chunk, VeinBlocks blocks, VeinShape.Sampler sampler,
                                  double rarity, BlockPos.MutableBlockPos pos, int x0, int y0, int z0) {
        for (int y = y0; y < y0 + 4; y++) {
            for (int x = x0; x < x0 + 4; x++) {
                for (int z = z0; z < z0 + 4; z++) {
                    int kind = sampler.classify(x, y, z, rarity);
                    if (kind == VeinShape.NONE) continue;
                    pos.set(x, y, z);
                    BlockState host = chunk.getBlockState(pos);
                    Block ore = oreFor(host, y, blocks);
                    if (ore == null) continue;
                    Block block = switch (kind) {
                        case VeinShape.RAW -> blocks.raw() == Blocks.AIR ? ore : blocks.raw();
                        case VeinShape.FILLER -> blocks.filler();
                        default -> ore;
                    };
                    if (block != Blocks.AIR) level.setBlock(pos, block.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * The ore variant for this host rock, or null when the vein can't replace it. Tuff counts as deepslate below
     * y=0 and as stone above (26.3's rule; 1.21.1 always treats it as deepslate, a difference only tuff notices).
     */
    private static Block oreFor(BlockState host, int y, VeinBlocks blocks) {
        if (host.is(Blocks.TUFF)) return y < 0 ? blocks.deepOre() : blocks.stoneOre();
        if (host.is(BlockTags.STONE_ORE_REPLACEABLES)) return blocks.stoneOre();
        if (host.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) return blocks.deepOre();
        return null;
    }
}
