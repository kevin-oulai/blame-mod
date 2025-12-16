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
    
    // Floor type constants
    private static final int FLOOR_TYPE_CLEAN_SLAB = 0;
    private static final int FLOOR_TYPE_INDUSTRIAL = 1;
    private static final int FLOOR_TYPE_BROKEN = 2;
    /**
     * Salt value used for floor type terrain generation.
     * Applied to noise functions to create variation in floor block placement.
     */
    private static final long FLOOR_TYPE_SALT = 0xF1001E5FL;
    
    // Ladder generation constants
    private static final double LADDER_PROBABILITY = 0.002; // very low chance per column
    private static final long LADDER_SALT = 0x1ADD31L;
    private static final int LADDER_HEIGHT = 10; // blocks of ladder per placement
    
    // Ramp generation constants
    private static final double RAMP_PROBABILITY = 0.3; // chance per floor per chunk
    private static final long RAMP_SALT = 0x12A3445L;
    private static final int RAMP_WIDTH = 5; // width in Z direction
    private static final int RAMP_DEPTH = 12; // depth into wall in X direction

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
                
                // Check if this column should have ladders (only near walls for support)
                int distFromWall = isInCanyon ? (CANYON_HALF_WIDTH - distFromCenter) : Integer.MAX_VALUE;
                boolean hasLadder = isInCanyon && distFromWall <= 2 && hash01(level.getSeed() ^ LADDER_SALT, worldX, worldZ) < LADDER_PROBABILITY;

                for (int y = minY; y < maxY; y++) {
                    pos.set(chunkX + x, y, chunkZ + z);
                    
                    // Compute floor index and type
                    int floorIndex = floorDiv(y, FLOOR_SPACING);
                    int floorType = getFloorType(level.getSeed(), floorIndex);
                    
                    // Determine thickness based on floor type
                    int floorThickness;
                    net.minecraft.world.level.block.state.BlockState floorBlock;
                    if (floorType == FLOOR_TYPE_CLEAN_SLAB) {
                        floorThickness = 1; // thin slab
                        floorBlock = Blocks.SMOOTH_STONE.defaultBlockState();
                    } else if (floorType == FLOOR_TYPE_INDUSTRIAL) {
                        // Vary between 2-3 based on position
                        floorThickness = 2 + (hash01(level.getSeed() ^ FLOOR_TYPE_SALT, worldX, worldZ) > 0.5 ? 1 : 0);
                        // Use slab for top layer, solid block for base
                        int layerInFloor = modFloor(y, FLOOR_SPACING);
                        if (layerInFloor == floorThickness - 1) {
                            floorBlock = Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState();
                        } else {
                            floorBlock = Blocks.POLISHED_ANDESITE.defaultBlockState();
                        }
                    } else { // FLOOR_TYPE_BROKEN
                        floorThickness = 1; // thin but will be broken
                        // Check if block above will be solid wall (for structural support)
                        boolean hasWallAbove = !isInCanyon || distFromWall > FLOOR_LEDGE;
                        if (hasWallAbove) {
                            // Use full block when supporting wall above
                            floorBlock = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                        } else {
                            // Use slab when no wall above
                            floorBlock = Blocks.LIGHT_GRAY_CONCRETE_SLAB.defaultBlockState();
                        }
                    }
                    
                    // Check if this Y is within a floor layer
                    boolean isFloorLayer = modFloor(y, FLOOR_SPACING) < floorThickness;
                    
                    // For broken floors (type 2), apply erosion only to slabs
                    boolean canErode = hasFloorNoise;
                    if (floorType == FLOOR_TYPE_BROKEN && floorBlock == Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()) {
                        canErode = true; // Full blocks always place
                    }
                    
                    if (isFloorLayer && canErode) {
                        if (isInCanyon) {
                            // Inside canyon: only place floor as ledge near walls
                            if (distFromWall <= FLOOR_LEDGE) {
                                // Apply erosion near canyon-facing edges (far from wall)
                                boolean shouldErode = false;
                                if (distFromWall > FLOOR_LEDGE - 3) {
                                    // Extra noise for erosion near canyon edge
                                    double erosionNoise = hash01(level.getSeed() ^ 0xE051091L, worldX, worldZ + y);
                                    shouldErode = erosionNoise < 0.15; // 15% chance to erode
                                }
                                
                                if (!shouldErode) {
                                    chunk.setBlockState(pos, floorBlock, false);
                                } else {
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                }
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // Outside canyon: check for ramp
                            int rampYOffset = getRampYOffset(level.getSeed(), chunkX, chunkZ, floorIndex, worldX, worldZ, centerX);
                            if (rampYOffset >= 0) {
                                // In ramp area - check if we should carve (air) or place stairs
                                int floorBaseY = floorIndex * FLOOR_SPACING;
                                int targetY = floorBaseY + rampYOffset;
                                
                                if (y == targetY) {
                                    // Place stair at ramp level
                                    chunk.setBlockState(pos, Blocks.STONE_STAIRS.defaultBlockState(), false);
                                } else if (y < targetY) {
                                    // Below ramp - leave as air
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                } else {
                                    // Above ramp - solid wall
                                    chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                                }
                            } else {
                                // Not in ramp - place floor
                                chunk.setBlockState(pos, floorBlock, false);
                            }
                        }
                    } else {
                        // Not a floor layer
                        if (isInCanyon) {
                            // Place ladder if this column has ladders and Y is between floor base and next floor
                            if (hasLadder) {
                                int floorBaseY = floorIndex * FLOOR_SPACING + floorThickness;
                                int nextFloorY = (floorIndex + 1) * FLOOR_SPACING;
                                if (y >= floorBaseY && y < nextFloorY) {
                                    // Determine ladder facing based on which side of canyon
                                    net.minecraft.core.Direction facing = worldX < centerX ? 
                                        net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
                                    chunk.setBlockState(pos, Blocks.LADDER.defaultBlockState()
                                        .setValue(net.minecraft.world.level.block.LadderBlock.FACING, facing), false);
                                } else {
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                }
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // Outside canyon: check for ramp first
                            int rampYOffset = getRampYOffset(level.getSeed(), chunkX, chunkZ, floorIndex, worldX, worldZ, centerX);
                            if (rampYOffset >= 0) {
                                // In ramp area - always solid wall when not in floor layer
                                chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                            } else {
                                // Not in ramp - normal solid wall
                                chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                            }
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
        return computeCanyonCenterX(level.getSeed(), worldZ);
    }
    
    // Public method for external use (e.g., commands)
    public static int computeCanyonCenterX(long seed, int worldZ) {
        seed = seed ^ 0xC0FFEE1234ABCDL;

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

    // Deterministic floor type based on floor index
    private static int getFloorType(long seed, int floorIndex) {
        long h = (seed ^ FLOOR_TYPE_SALT) ^ (floorIndex * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        
        // Map to [0, 3) then floor to get 0, 1, or 2
        int type = (int)((h & 0x7FFFFFFFL) % 3);
        return type;
    }
    
    // Check if chunk has ramp for given floor index, and if so, return ramp Z center
    private static int getRampZCenter(long seed, int chunkX, int chunkZ, int floorIndex) {
        double prob = hash01(seed ^ RAMP_SALT, chunkX, chunkZ + floorIndex * 1000);
        if (prob < RAMP_PROBABILITY) {
            // Has ramp, determine Z position within chunk
            int zOffset = (int)(hash01(seed ^ RAMP_SALT ^ 0x999L, chunkX, chunkZ + floorIndex * 1000) * 16);
            return chunkZ + zOffset;
        }
        return Integer.MIN_VALUE; // no ramp
    }
    
    // Returns Y offset for ramp at given position, or -1 if not in ramp
    private static int getRampYOffset(long seed, int chunkX, int chunkZ, int floorIndex, int worldX, int worldZ, int centerX) {
        int rampZCenter = getRampZCenter(seed, chunkX, chunkZ, floorIndex);
        if (rampZCenter == Integer.MIN_VALUE) return -1;
        
        // Check if in ramp Z range
        int distZ = Math.abs(worldZ - rampZCenter);
        if (distZ >= RAMP_WIDTH / 2) return -1;
        
        // Check if in wall on one side of canyon
        int distFromCenter = Math.abs(worldX - centerX);
        if (distFromCenter < CANYON_HALF_WIDTH) return -1; // inside canyon
        
        // In wall - check depth into wall
        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
        if (distIntoWall >= RAMP_DEPTH) return -1; // too far into wall
        
        // Ramp rises 1 block per 1-2 blocks horizontally
        return distIntoWall / 2;
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