package net.liferquest.blamemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Post-processes chunks in the Netsphere dimension to create canyon structures
 */
public class NetsphereChunkPostProcessor {

    private static final int CANYON_HALF_WIDTH = 80;
    private static final int NOISE_AMPLITUDE = 40; // (currently unused, safe to keep)
    private static final int BASE_X = 0;
    private static final int ANCHOR_STEP = 128; // bigger = smoother curve
    private static final int AMPLITUDE = 80;    // sideways drift

    // Floor generation constants
    private static final int FLOOR_SPACING = 14;        // vertical distance between floors
    private static final int FLOOR_THICKNESS = 2;       // (unused because thickness varies by type; safe to keep)
    private static final int FLOOR_LEDGE = 10;          // how far ledges extend into canyon
    private static final double FLOOR_THRESHOLD = 0.35; // noise threshold for ledge placement
    private static final long FLOOR_NOISE_SALT = 0x5EED1E5FL; // salt for floor noise

    // Floor type constants
    private static final int FLOOR_TYPE_CLEAN_SLAB = 0;
    private static final int FLOOR_TYPE_INDUSTRIAL = 1;
    private static final int FLOOR_TYPE_BROKEN = 2;

    private static final long FLOOR_TYPE_SALT = 0xF1001E5FL;

    // Ladder generation constants
    private static final double LADDER_PROBABILITY = 0.002; // very low chance per column
    private static final long LADDER_SALT = 0x1ADD31L;
    private static final int LADDER_HEIGHT = 10; // (not used explicitly; ladder length is based on next floor)

    // Ramp generation constants
    private static final double RAMP_PROBABILITY = 0.08; // probability per floor per chunk
    private static final long RAMP_SALT = 0x12A3445L;
    private static final int RAMP_WIDTH = 7; // width in Z direction
    private static final int RAMP_DEPTH = 6; // depth into wall in X direction (near canyon, shallow)
    private static final int RAMP_HEADROOM = 3; // blocks of air above stairs for player clearance

    // -------------------------
    // Facade carving (deeper)
    // -------------------------
    private static final int FACADE_BAND_THICKNESS = 3; // how close to canyon wall (inside wall)
    private static final int FACADE_DEPTH = 7;          // max carving depth into wall
    private static final int FACADE_Z_SPACING = 10;     // repeat along the wall (Z)
    private static final int FACADE_Y_SPACING = 12;     // repeat vertically (Y)
    private static final int ARCH_WIDTH = 6;
    private static final int ARCH_HEIGHT = 7;
    private static final long FACADE_NOISE_SALT = 0xFACAD3L;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Only process on server side
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // Only process Netsphere dimension
        if (!serverLevel.dimension().equals(ModDimensions.NETSPHERE_LEVEL)) return;

        ChunkAccess chunk = event.getChunk();

        // Only process if chunk is a LevelChunk (fully loaded)
        if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk)) return;

        processChunk(serverLevel, levelChunk);
    }

    private static void processChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight(), 256);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Get chunk coordinates (block coords)
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();

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

                // Ladder columns: only near canyon wall
                int distFromWall = isInCanyon ? (CANYON_HALF_WIDTH - distFromCenter) : Integer.MAX_VALUE;
                boolean hasLadder = isInCanyon && distFromWall == 1
                        && hash01(level.getSeed() ^ LADDER_SALT, worldX, worldZ) < LADDER_PROBABILITY;

                for (int y = minY; y < maxY; y++) {
                    pos.set(worldX, y, worldZ);

                    // Floor identity
                    int floorIndex = floorDiv(y, FLOOR_SPACING);
                    int floorType = getFloorType(level.getSeed(), floorIndex);

                    // Determine thickness + block palette for this floor type
                    int floorThickness;
                    net.minecraft.world.level.block.state.BlockState floorBlock;

                    if (floorType == FLOOR_TYPE_CLEAN_SLAB) {
                        floorThickness = 1;
                        floorBlock = Blocks.SMOOTH_STONE.defaultBlockState();
                    } else if (floorType == FLOOR_TYPE_INDUSTRIAL) {
                        floorThickness = 2 + (hash01(level.getSeed() ^ FLOOR_TYPE_SALT, worldX, worldZ) > 0.5 ? 1 : 0);

                        int layerInFloor = modFloor(y, FLOOR_SPACING);
                        if (layerInFloor == floorThickness - 1) {
                            floorBlock = Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState();
                        } else {
                            floorBlock = Blocks.POLISHED_ANDESITE.defaultBlockState();
                        }
                    } else { // FLOOR_TYPE_BROKEN
                        floorThickness = 1;
                        floorBlock = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                    }

                    boolean isFloorLayer = modFloor(y, FLOOR_SPACING) < floorThickness;

                    // -------------------------
                    // RAMP (connects to floor above with proper headroom)
                    // -------------------------
                    if (!isInCanyon) {
                        int floorTopY = floorIndex * FLOOR_SPACING + floorThickness;

                        // Calculate next floor info for ramp connection
                        int nextFloorIndex = floorIndex + 1;
                        int nextFloorType = getFloorType(level.getSeed(), nextFloorIndex);
                        int nextFloorThickness;
                        if (nextFloorType == FLOOR_TYPE_CLEAN_SLAB) {
                            nextFloorThickness = 1;
                        } else if (nextFloorType == FLOOR_TYPE_INDUSTRIAL) {
                            nextFloorThickness = 2 + (hash01(level.getSeed() ^ FLOOR_TYPE_SALT, worldX, worldZ) > 0.5 ? 1 : 0);
                        } else {
                            nextFloorThickness = 1;
                        }
                        int nextFloorBaseY = nextFloorIndex * FLOOR_SPACING;
                        int nextFloorTopY = nextFloorBaseY + nextFloorThickness;

                        RampInfo rampInfo = getRampInfo(
                                level.getSeed(),
                                chunkX, chunkZ,
                                floorIndex,
                                worldX, worldZ,
                                centerX,
                                floorTopY,
                                nextFloorBaseY
                        );

                        if (rampInfo != null) {
                            net.minecraft.core.Direction stairFacing = (worldX < centerX)
                                    ? net.minecraft.core.Direction.EAST
                                    : net.minecraft.core.Direction.WEST;

                            // Place stair block at ramp Y
                            if (y == rampInfo.rampY) {
                                chunk.setBlockState(
                                        pos,
                                        Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                                                .setValue(net.minecraft.world.level.block.StairBlock.FACING, stairFacing),
                                        false
                                );
                            }
                            // Headroom above the stair (3 blocks for player clearance)
                            else if (y > rampInfo.rampY && y <= rampInfo.rampY + RAMP_HEADROOM) {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                            // Landing at top of ramp (connects to next floor)
                            else if (rampInfo.isAtTop && y >= nextFloorBaseY && y < nextFloorTopY) {
                                // Place landing floor blocks
                                if (nextFloorType == FLOOR_TYPE_CLEAN_SLAB) {
                                    chunk.setBlockState(pos, Blocks.SMOOTH_STONE.defaultBlockState(), false);
                                } else if (nextFloorType == FLOOR_TYPE_INDUSTRIAL) {
                                    int layerInFloor = y - nextFloorBaseY;
                                    if (layerInFloor == nextFloorThickness - 1) {
                                        chunk.setBlockState(pos, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), false);
                                    } else {
                                        chunk.setBlockState(pos, Blocks.POLISHED_ANDESITE.defaultBlockState(), false);
                                    }
                                } else {
                                    chunk.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
                                }
                            }
                            // Headroom above landing
                            else if (rampInfo.isAtTop && y >= nextFloorTopY && y <= nextFloorTopY + RAMP_HEADROOM) {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                            // Solid support under the ramp
                            else if (y < rampInfo.rampY) {
                                chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                            }
                            // Above headroom stays solid
                            else {
                                chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                            }
                            continue;
                        }
                    }

                    // Erosion / placement rules
                    boolean canErode = hasFloorNoise;
                    if (floorType == FLOOR_TYPE_BROKEN && floorBlock == Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()) {
                        canErode = true;
                    }

                    if (isFloorLayer && canErode) {
                        if (isInCanyon) {
                            // Inside canyon: only place ledges near walls
                            if (distFromWall <= FLOOR_LEDGE) {
                                boolean shouldErode = false;
                                if (distFromWall == FLOOR_LEDGE) {
                                    double erosionNoise = hash01(level.getSeed() ^ 0xE051091L, worldX, worldZ + y);
                                    shouldErode = erosionNoise < 0.15;
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
                            // Outside canyon: normal floor band
                            chunk.setBlockState(pos, floorBlock, false);
                        }
                    } else {
                        // Not a floor layer
                        if (isInCanyon) {
                            if (hasLadder) {
                                int floorBaseY = floorIndex * FLOOR_SPACING + floorThickness;

                                int nextFloorIndex = floorIndex + 1;
                                int nextFloorType = getFloorType(level.getSeed(), nextFloorIndex);

                                int nextFloorThickness;
                                if (nextFloorType == FLOOR_TYPE_CLEAN_SLAB) {
                                    nextFloorThickness = 1;
                                } else if (nextFloorType == FLOOR_TYPE_INDUSTRIAL) {
                                    nextFloorThickness = 2 + (hash01(level.getSeed() ^ FLOOR_TYPE_SALT, worldX, worldZ) > 0.5 ? 1 : 0);
                                } else {
                                    nextFloorThickness = 1;
                                }

                                int nextFloorY = nextFloorIndex * FLOOR_SPACING + nextFloorThickness;

                                if (y >= floorBaseY && y < nextFloorY) {
                                    net.minecraft.core.Direction facing = worldX < centerX
                                            ? net.minecraft.core.Direction.EAST
                                            : net.minecraft.core.Direction.WEST;

                                    chunk.setBlockState(
                                            pos,
                                            Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, facing),
                                            false
                                    );
                                } else {
                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                }
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // -------------------------
                            // Facade carving (deeper cavities)
                            // -------------------------
                            if (isInFacadeBand(worldX, centerX)) {
                                int localZ = Math.floorMod(worldZ, FACADE_Z_SPACING) - (FACADE_Z_SPACING / 2);
                                int localY = Math.floorMod(y, FACADE_Y_SPACING);

                                int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;

                                if (localY < ARCH_HEIGHT && Math.abs(localZ) <= (ARCH_WIDTH / 2)) {
                                    double n = valueNoise2D(level.getSeed() ^ FACADE_NOISE_SALT, worldZ, y, 32);

                                    boolean allowFacade = switch (floorType) {
                                        case FLOOR_TYPE_INDUSTRIAL -> n > 0.5;
                                        case FLOOR_TYPE_BROKEN -> n > 0.75;
                                        default -> n > 0.4;
                                    };

                                    if (allowFacade && isInsideArch(localZ, localY)) {
                                        if (distIntoWall >= 0 && distIntoWall < FACADE_DEPTH) {
                                            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                            continue;
                                        }
                                    }
                                }
                            }

                            // Default solid wall
                            chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                        }
                    }
                }
            }
        }

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

        int c0 = BASE_X + (int) Math.round((hashSigned(seed, z0) * AMPLITUDE));
        int c1 = BASE_X + (int) Math.round((hashSigned(seed, z1) * AMPLITUDE));

        return (int) Math.round(lerp(c0, c1, t));
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

        return (int) ((h & 0x7FFFFFFFL) % 3);
    }

    // Helper class to hold ramp information
    private static class RampInfo {
        final int rampY;
        final boolean isAtTop;

        RampInfo(int rampY, boolean isAtTop) {
            this.rampY = rampY;
            this.isAtTop = isAtTop;
        }
    }

    // Ramp Z center per floor+chunk, or Integer.MIN_VALUE if no ramp this floor in this chunk.
    private static int rampZCenterFor(long seed, int chunkBlockX, int chunkBlockZ, int floorIndex) {
        int cx = chunkBlockX >> 4;
        int cz = chunkBlockZ >> 4;

        long h = seed ^ RAMP_SALT ^ (long) floorIndex * 1315423911L ^ cx * 73428767L ^ cz * 912367L;

        if ((h & 0xFF) >= (int) (RAMP_PROBABILITY * 256)) return Integer.MIN_VALUE;

        int zOffset = (int) ((h >>> 8) & 15); // 0..15
        return (cz << 4) + zOffset;
    }

    // Returns RampInfo for ramp at (worldX, worldZ), or null if not part of ramp.
    // The ramp rises from floorTopY to nextFloorBaseY over RAMP_DEPTH blocks.
    private static RampInfo getRampInfo(
            long seed,
            int chunkBlockX, int chunkBlockZ,
            int floorIndex,
            int worldX, int worldZ,
            int centerX,
            int floorTopY,
            int nextFloorBaseY
    ) {
        int rampZCenter = rampZCenterFor(seed, chunkBlockX, chunkBlockZ, floorIndex);
        if (rampZCenter == Integer.MIN_VALUE) return null;

        // Must be in wall (not inside canyon)
        int distFromCenter = Math.abs(worldX - centerX);
        if (distFromCenter < CANYON_HALF_WIDTH) return null;

        // Depth into wall near canyon
        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
        if (distIntoWall < 0 || distIntoWall >= RAMP_DEPTH) return null;

        // Z band
        int distZ = Math.abs(worldZ - rampZCenter);
        if (distZ > (RAMP_WIDTH / 2)) return null;

        // Calculate ramp height: rises from floorTopY to nextFloorBaseY over RAMP_DEPTH
        int heightDifference = nextFloorBaseY - floorTopY;
        // Interpolate height based on distance into wall
        // distIntoWall ranges from 0 to RAMP_DEPTH-1
        // At distIntoWall=0: rampY = floorTopY
        // At distIntoWall=RAMP_DEPTH-1: rampY = nextFloorBaseY - 1 (one block below next floor)
        int rampY;
        if (RAMP_DEPTH == 1) {
            // Edge case: single step ramp
            rampY = floorTopY;
        } else {
            rampY = floorTopY + (int) Math.round((heightDifference - 1) * (distIntoWall / (double) (RAMP_DEPTH - 1)));
        }
        
        // Check if we're at the top of the ramp (last step before landing)
        boolean isAtTop = (distIntoWall == RAMP_DEPTH - 1);

        return new RampInfo(rampY, isAtTop);
    }

    // Returns true if position is in the facade carving band (just inside canyon wall)
    private static boolean isInFacadeBand(int worldX, int centerX) {
        int distFromCenter = Math.abs(worldX - centerX);
        if (distFromCenter < CANYON_HALF_WIDTH) return false;

        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
        return distIntoWall < FACADE_BAND_THICKNESS;
    }

    // Returns true if point is inside a simple arch shape (rectangle + semicircle top)
    private static boolean isInsideArch(int z, int y) {
        if (y < ARCH_HEIGHT - ARCH_WIDTH / 2) {
            return Math.abs(z) <= ARCH_WIDTH / 2;
        }

        int dz = z;
        int dy = y - (ARCH_HEIGHT - ARCH_WIDTH / 2);
        int r = ARCH_WIDTH / 2;
        return (dz * dz + dy * dy) <= (r * r);
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
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
