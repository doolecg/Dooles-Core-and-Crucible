package dev.doole.corecrucible.worldgen;

/**
 * The shape of one kind of ore vein, modelled on vanilla's large iron and copper veins.
 *
 * <p>Two noise fields are close to zero along thin sheets; where both sheets cross, they trace a long winding tube.
 * That tube is the vein. A third, slower "region" noise decides where veins exist at all, so veins cluster in
 * patches with bare rock between them. Inside the tube each block rolls: ore (sometimes the raw-ore block),
 * filler rock, or left alone.
 *
 * @param minY         lowest block the vein reaches
 * @param maxY         highest block the vein reaches
 * @param size         vein scale: 1 is roughly a vanilla iron vein's thickness; 2 is twice as long and thick
 * @param rarity       region threshold, -1..1: higher means fewer, further-apart vein patches
 * @param oreChance    share of the vein's blocks that become ore
 * @param fillerChance share of the vein's blocks that become the filler rock (tuff for iron, granite for copper...)
 * @param rawChance    share of the ore blocks that become the raw-ore block instead, where the ore has one
 * @param patch        block size of the region noise's features: smaller patches cut veins into shorter pieces,
 *                     more of them, with the same total ore and thickness
 */
public record VeinShape(int minY, int maxY, double size, double rarity, double oreChance, double fillerChance,
                        double rawChance, double patch) {

    /** A shape with the default patch size. */
    public VeinShape(int minY, int maxY, double size, double rarity, double oreChance, double fillerChance, double rawChance) {
        this(minY, maxY, size, rarity, oreChance, fillerChance, rawChance, REGION_SCALE);
    }

    // What classify() says a block should become.
    public static final int NONE = 0;
    public static final int ORE = 1;
    public static final int RAW = 2;
    public static final int FILLER = 3;

    /** How close to zero both sheet noises must be, at full strength. Sets the vein's thickness: veins are about
     * THICKNESS × SHEET_SCALE blocks thick, so change the two together to keep that size. */
    static final double THICKNESS = 0.0569;
    /** Block distance between the sheets at size 1. Wider spacing means fewer veins: yield falls with its square. */
    static final double SHEET_SCALE = 45;
    /** Default block size of the region noise's features, so patches are a few hundred blocks apart. */
    static final double REGION_SCALE = 160;
    /** Veins thin out over this much region noise above the threshold instead of stopping dead. */
    static final double FADE = 0.1;
    /**
     * The region noise changes by less than this across a 4-block cell at the default patch size, so a cell test with
     * it is safe. Smaller patches change faster, so the margin grows with {@code REGION_SCALE / patch}.
     */
    static final double CELL_MARGIN = 0.1;

    /** Region noise minus the fade near the top and bottom of the vein's height range. */
    // Near the top and bottom of the height range the region value is lowered, so veins fade out instead of ending in
    // a flat cut.
    private double region(Sampler s, double x, double y, double z) {
        double band = Math.max(8, (maxY - minY) * 0.2);
        double edge = Math.min(y - minY, maxY - y) / band;
        double taper = edge >= 1 ? 0 : 0.3 * (1 - Math.max(0, edge));
        return s.region.sample(x / patch, y / patch, z / patch) - taper;
    }

    /** One vein kind's noise fields for one world seed. */
    public static final class Sampler {
        private final VeinShape shape;
        private final VeinNoise region;
        private final VeinNoise sheetA;
        private final VeinNoise sheetB;
        private final long seed;
        private final double sheetScale;

        public Sampler(VeinShape shape, long seed) {
            this.shape = shape;
            this.seed = VeinNoise.mix(seed);
            this.region = new VeinNoise(this.seed + 1);
            this.sheetA = new VeinNoise(this.seed + 2);
            this.sheetB = new VeinNoise(this.seed + 3);
            this.sheetScale = SHEET_SCALE * shape.size;
        }

        public VeinShape shape() {
            return shape;
        }

        /** Cheap pre-check for the 4×4×4 cell centred here: false means no block in it can be part of a vein. */
        public boolean mayContain(int centerX, int centerY, int centerZ) {
            return mayContain(centerX, centerY, centerZ, shape.rarity);
        }

        /** {@link #mayContain(int, int, int)} with another rarity, e.g. a kind's favored-biome one. */
        public boolean mayContain(int centerX, int centerY, int centerZ, double rarity) {
            return centerY >= shape.minY - 2 && centerY <= shape.maxY + 2
                    && shape.region(this, centerX, centerY, centerZ) > rarity - CELL_MARGIN * REGION_SCALE / shape.patch;
        }

        /** What this block becomes: {@link #NONE}, {@link #ORE}, {@link #RAW} or {@link #FILLER}. */
        public int classify(int x, int y, int z) {
            return classify(x, y, z, shape.rarity);
        }

        /** {@link #classify(int, int, int)} with another rarity, e.g. a kind's favored-biome one. */
        public int classify(int x, int y, int z, double rarity) {
            if (y < shape.minY || y > shape.maxY) return NONE;
            double strength = (shape.region(this, x, y, z) - rarity) / FADE;
            if (strength <= 0) return NONE;
            double thickness = THICKNESS * Math.min(1, strength);
            // Inside the vein only where both sheet noises are near zero (where the two sheets cross).
            if (Math.abs(sheetA.sample(x / sheetScale, y / sheetScale, z / sheetScale)) >= thickness) return NONE;
            if (Math.abs(sheetB.sample(x / sheetScale, y / sheetScale, z / sheetScale)) >= thickness) return NONE;

            // A random number fixed for this block and seed, so the same block always rolls the same way.
            long roll = VeinNoise.mix(seed ^ (x * 0x2545F491L) ^ (y * 0x9E3779B1L) ^ ((long) z << 32));
            double r = VeinNoise.unit(roll);
            if (r < shape.oreChance) {
                return VeinNoise.unit(VeinNoise.mix(roll)) < shape.rawChance ? RAW : ORE;
            }
            return r < shape.oreChance + shape.fillerChance ? FILLER : NONE;
        }
    }
}
