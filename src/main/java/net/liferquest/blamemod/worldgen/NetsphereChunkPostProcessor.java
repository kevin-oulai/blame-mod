package net.liferquest.blamemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


/**
 * Post-processes chunks in the Netsphere dimension to create canyon structures
 */
public class NetsphereChunkPostProcessor {
    
    private static final int CANYON_HALF_WIDTH = 80;
    private static final int NOISE_AMPLITUDE = 40;
    private static final int BASE_X = 0;
    private static final int ANCHOR_STEP = 128; // bigger = smoother curve
    private static final int AMPLITUDE = 80;  // sideways drift
    
    // Floor generation constants
    private static final int FLOOR_SPACING = 14;     // vertical distance between floors
    private static final int FLOOR_THICKNESS = 2;    // how thick each floor is
    private static final int FLOOR_LEDGE = 10;       // how far ledges extend into canyon
    private static final double FLOOR_THRESHOLD = 0.35; // noise threshold for ledge placement
    private static final long FLOOR_NOISE_SALT = 0x5EED1E5FL; // salt for floor noise

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Only process on server side
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // Only process Netsphere dimension
        if (!serverLevel.dimension().equals(ModDimensions.NETSPHERE_LEVEL)) {
            return;
        }
        
        ChunkAccess chunk = event.getChunk();

        // Only process if chunk is a LevelChunk (fully loaded)
        if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk)) {
            return;
        }

        processChunk(serverLevel, levelChunk);
    }
    
    private static void processChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight(), 256);
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        // Get chunk coordinates
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        
        // Fill the entire chunk with white concrete, then carve canyon with floors
        long floorSeed = level.getSeed() ^ FLOOR_NOISE_SALT;
        
        for (int x = 0; x < 16; x++) {
            int worldX = chunkX + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = chunkZ + z;
                int centerX = computeCanyonCenterX(level, worldZ);
                int distFromCenter = Math.abs(worldX - centerX);
                boolean isInCanyon = distFromCenter < CANYON_HALF_WIDTH;
                
                // Sample noise once per column for floor generation
                double noise = valueNoise2D(floorSeed, worldX, worldZ, 24);
                boolean hasFloorNoise = noise > FLOOR_THRESHOLD;

                for (int y = minY; y < maxY; y++) {
                    pos.set(chunkX + x, y, chunkZ + z);
                    
                    // Check if this Y is within a floor layer
                    boolean isFloorLayer = modFloor(y, FLOOR_SPACING) < FLOOR_THICKNESS;
                    
                    if (isFloorLayer && hasFloorNoise) {
                        if (isInCanyon) {
                            // Inside canyon: only place floor as ledge near walls
                            int distFromWall = CANYON_HALF_WIDTH - distFromCenter;
                            if (distFromWall <= FLOOR_LEDGE) {
                                chunk.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // Outside canyon: place floor
                            chunk.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
                        }
                    } else {
                        // Not a floor layer
                        if (isInCanyon) {
                            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                        } else {
                            chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                        }
                    }
                }
            }
        }
        
        // Mark chunk as modified
        chunk.setUnsaved(true);
    }

    // Returns centerX for a given worldZ, smoothly varying.
    private static int computeCanyonCenterX(ServerLevel level, int worldZ) {
        long seed = level.getSeed() ^ 0xC0FFEE1234ABCDL;

        int z0 = floorDiv(worldZ, ANCHOR_STEP) * ANCHOR_STEP;
        int z1 = z0 + ANCHOR_STEP;

        double t = (worldZ - z0) / (double) ANCHOR_STEP;
        t = smoothstep(t);

        int c0 = BASE_X + (int)Math.round((hashSigned(seed, z0) * AMPLITUDE));
        int c1 = BASE_X + (int)Math.round((hashSigned(seed, z1) * AMPLITUDE));

        return (int)Math.round(lerp(c0, c1, t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    // Deterministic hash -> [-1, 1]
    private static double hashSigned(long seed, int z) {
        long h = seed ^ (z * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);

        double u01 = (h >>> 11) * (1.0 / (1L << 53)); // [0,1)
        return (u01 * 2.0) - 1.0; // [-1,1)
    }

    // Deterministic 2D hash -> [0, 1)
    private static double hash01(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC13FA9A902A6328FL);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return (h >>> 11) * (1.0 / (1L << 53)); // [0,1)
    }

    // 2D value noise using grid-based interpolation -> [0, 1)
    private static double valueNoise2D(long seed, int x, int z, int step) {
        int x0 = floorDiv(x, step) * step;
        int z0 = floorDiv(z, step) * step;
        int x1 = x0 + step;
        int z1 = z0 + step;

        double tx = (x - x0) / (double) step;
        double tz = (z - z0) / (double) step;
        tx = smoothstep(tx);
        tz = smoothstep(tz);

        double v00 = hash01(seed, x0, z0);
        double v10 = hash01(seed, x1, z0);
        double v01 = hash01(seed, x0, z1);
        double v11 = hash01(seed, x1, z1);

        double v0 = lerp(v00, v10, tx);
        double v1 = lerp(v01, v11, tx);
        return lerp(v0, v1, tz);
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
        // correct toward -infinity
        if ((a ^ b) < 0 && (r * b != a)) r--;
        return r;
    }

    // Positive modulo that works correctly for negative values
    private static int modFloor(int a, int m) {
        int r = a % m;
        if (r < 0) r += m;
        return r;
    }
}