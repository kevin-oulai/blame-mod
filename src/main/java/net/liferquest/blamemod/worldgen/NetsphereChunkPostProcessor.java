package net.liferquest.blamemod.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
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
    private static final int FLOOR_WALL_EXTENT = 5;      // how far floors extend into walls from canyon edge
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

    // Vertical shaft generation constants (alternative to ladders)
    private static final double SHAFT_PROBABILITY = 0.25; // probability per floor per chunk
    private static final long SHAFT_SALT = 0x5A1F7L;
    private static final int SHAFT_WIDTH = 3; // 3x3 shaft
    private static final int SHAFT_DEPTH = 8; // depth into wall from canyon edge

    // -------------------------
    // Facade carving (increased frequency)
    // -------------------------
    private static final int FACADE_BAND_THICKNESS = 4; // increased from 3 - wider band
    private static final int FACADE_DEPTH = 12;          // increased from 7 - deeper carving
    private static final int FACADE_Z_SPACING = 8;       // decreased from 10 - more frequent
    private static final int FACADE_Y_SPACING = 10;       // decreased from 12 - more frequent vertically
    private static final int ARCH_WIDTH = 6;
    private static final int ARCH_HEIGHT = 7;
    private static final long FACADE_NOISE_SALT = 0xFACAD3L;

    // Corridor generation constants (old system - kept for backward compatibility)
    private static final double CORRIDOR_PROBABILITY = 0.7; // chance a facade leads to a corridor
    private static final long CORRIDOR_SALT_OLD = 0xC0C1D0C1L;
    private static final int CORRIDOR_MIN_LENGTH_OLD = 8;
    private static final int CORRIDOR_MAX_LENGTH_OLD = 24;
    private static final int CORRIDOR_WIDTH_OLD = 3;
    private static final int CORRIDOR_HEIGHT_OLD = 4;
    private static final double CORRIDOR_BRANCH_PROBABILITY = 0.15; // chance to branch at each step
    private static final int CORRIDOR_MAX_BRANCHES_OLD = 3; // max branches per corridor (old system)

    // Bridge generation constants
    private static final int BRIDGE_SEGMENT_Z = 256; // Z segment size for bridge placement
    private static final double MEGABRIDGE_PROB = 0.16; // probability for megabridges
    private static final double WALKWAY_PROB = 0.12; // probability for walkways
    private static final int MEGABRIDGE_HALF_THICKNESS = 2; // vertical thickness = 2*+1 (so 5 blocks total)
    private static final int MEGABRIDGE_WIDTH = 9; // width in Z direction
    private static final int WALKWAY_WIDTH = 2; // width in Z direction
    private static final int WALKWAY_THICKNESS = 1; // vertical thickness
    private static final long MEGABRIDGE_SALT = 0xE3A4B01D63L;
    private static final long WALKWAY_SALT = 0x4A1C4A41L;

    // Corridor generation constants (new system)
    private static final int CORRIDOR_SEGMENT = 128; // Z segment size for corridor placement
    private static final double CORRIDOR_PROB = 0.08; // probability for corridor sites
    private static final int CORRIDOR_MIN_LENGTH = 64; // minimum corridor length
    private static final int CORRIDOR_MAX_LENGTH = 128; // maximum corridor length
    private static final double CORRIDOR_TURN_PROB = 0.15; // probability of turning left/right
    private static final int CORRIDOR_TURN_SEGMENT = 32; // segment size for turn checks
    private static final int CORRIDOR_MIN_DEPTH = 4; // minimum depth from canyon face
    private static final int CORRIDOR_MAX_DEPTH = 80; // maximum depth from canyon face (extended deep)
    private static final int CORRIDOR_MIN_WIDTH = 2; // minimum corridor width
    private static final int CORRIDOR_MAX_WIDTH = 5; // maximum corridor width
    private static final int CORRIDOR_MIN_HEIGHT = 2; // minimum corridor height
    private static final int CORRIDOR_MAX_HEIGHT = 16; // maximum corridor height
    private static final double CORRIDOR_BRANCH_PROB = 0.25; // probability of branching at each segment
    private static final int CORRIDOR_MAX_BRANCHES = 16; // max branches per corridor
    private static final int CORRIDOR_BRANCH_SEGMENT = 16; // segment size for branch checks
    private static final double CORRIDOR_LIGHT_BROKEN_PROB = 0.95; // probability of broken lights
    private static final int CORRIDOR_STAIRS_SEGMENT = 12; // segment size for stairs
    private static final double CORRIDOR_STAIRS_PROB = 0.25; // probability of stairs up/down
    private static final long CORRIDOR_SALT = 0xC0C1D0C1L; // salt for corridor generation

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Only process on server side
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // Only process Netsphere dimension
        if (!serverLevel.dimension().equals(ModDimensions.NETSPHERE_LEVEL)) return;

        ChunkAccess chunk = event.getChunk();

        // Only process if chunk is a LevelChunk (fully loaded)
        if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk)) return;

        // Process chunk to ensure it has our custom generation
        processChunk(serverLevel, levelChunk);
    }

    // Also process chunks before they're saved to ensure fresh generation is processed
    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save event) {
        // Only process on server side
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        // Only process Netsphere dimension
        if (!serverLevel.dimension().equals(ModDimensions.NETSPHERE_LEVEL)) return;

        ChunkAccess chunk = event.getChunk();

        // Only process if chunk is a LevelChunk (fully loaded)
        if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk)) return;

        // Process chunk before saving to ensure it has our custom generation
        // This ensures freshly generated chunks are processed, not just loaded ones
        processChunk(serverLevel, levelChunk);
    }

    public static void processChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
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
                    // VERTICAL SHAFT (alternative to ladders for floor-to-floor travel)
                    // -------------------------
                    if (!isInCanyon) {
                        int floorTopY = floorIndex * FLOOR_SPACING + floorThickness;
                        int nextFloorIndex = floorIndex + 1;
                        int nextFloorBaseY = nextFloorIndex * FLOOR_SPACING;

                        ShaftInfo shaftInfo = getShaftInfo(
                                level.getSeed(),
                                chunkX, chunkZ,
                                floorIndex,
                                worldX, worldZ,
                                centerX
                        );

                        if (shaftInfo != null) {
                        // Check if this position is within the shaft
                        int localX = worldX - shaftInfo.shaftX;
                        int localZ = worldZ - shaftInfo.shaftZ;
                        
                        if (Math.abs(localX) <= SHAFT_WIDTH / 2 && Math.abs(localZ) <= SHAFT_WIDTH / 2) {
                            // Inside shaft: carve vertical passage
                            if (y >= floorTopY && y < nextFloorBaseY) {
                                // Carve the shaft (3x3 air passage)
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            } else if (y == floorTopY - 1 || y == nextFloorBaseY) {
                                // Place platform at floor levels
                                chunk.setBlockState(pos, Blocks.IRON_BARS.defaultBlockState(), false);
                            }
                            continue;
                        }
                    }
                    }

                    // -------------------------
                    // BRIDGE GENERATION (megabridges)
                    // -------------------------
                    int floorTopY = floorIndex * FLOOR_SPACING + floorThickness;
                    int segZ = floorDiv(worldZ, BRIDGE_SEGMENT_Z);
                    boolean hasBridge = hasMegabridge(level.getSeed(), floorIndex, segZ);
                    int bridgeZCenter = hasBridge ? megabridgeZCenter(level.getSeed(), floorIndex, segZ) : 0;
                    int distZ = hasBridge ? Math.abs(worldZ - bridgeZCenter) : Integer.MAX_VALUE;
                    boolean inBridgeZRange = hasBridge && distZ <= MEGABRIDGE_WIDTH / 2;
                    
                    // Check if bridge is broken (deterministic break chance)
                    boolean isBridgeBroken = false;
                    if (hasBridge) {
                        double breakChance = hash01(level.getSeed() ^ MEGABRIDGE_SALT ^ 0xDEADL, floorIndex, segZ);
                        isBridgeBroken = breakChance < 0.3; // 30% chance to be broken (adjust as needed)
                    }
                    
                    if (isInCanyon && inBridgeZRange) {
                        // Bridge spans from leftWallXAt+1 to rightWallXAt-1 (inside the void)
                        int leftWall = leftWallXAt(centerX);
                        int rightWall = rightWallXAt(centerX);
                        boolean inBridgeXRange = worldX > leftWall && worldX < rightWall;
                        
                        if (inBridgeXRange) {
                            // Check if we're in the broken middle section
                            boolean inBrokenSection = isBridgeBroken && Math.abs(worldX - centerX) < 12;
                            
                            if (!inBrokenSection) {
                                // Support beam 1 block below centerline
                                if (y == floorTopY - 2) {
                                    chunk.setBlockState(pos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), false);
                                    continue;
                                }
                                
                                // Bridge blocks in [floorTopY-1, floorTopY+MEGABRIDGE_HALF_THICKNESS]
                                if (y >= floorTopY - 1 && y <= floorTopY + MEGABRIDGE_HALF_THICKNESS) {
                                    // Use SMOOTH_STONE or POLISHED_DEEPSLATE for bridge blocks
                                    if (y == floorTopY - 1 || y == floorTopY + MEGABRIDGE_HALF_THICKNESS) {
                                        // Top and bottom layers use polished deepslate
                                        chunk.setBlockState(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), false);
                                    } else {
                                        // Middle layers use smooth stone
                                        chunk.setBlockState(pos, Blocks.SMOOTH_STONE.defaultBlockState(), false);
                                    }
                                    continue;
                                }
                            }
                            // If inBrokenSection, leave as air (don't place bridge blocks)
                        }
                    }
                    
                    // Bridge anchors embedded in walls (1-2 blocks into wall)
                    if (!isInCanyon && inBridgeZRange) {
                        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
                        // Anchor blocks in [CANYON_HALF_WIDTH, CANYON_HALF_WIDTH+2] (1-2 blocks into wall)
                        if (distIntoWall >= 0 && distIntoWall < 2) {
                            // Same Y range as bridge
                            if (y >= floorTopY - 1 && y <= floorTopY + MEGABRIDGE_HALF_THICKNESS) {
                                // Use darker material for anchor housings
                                if (y == floorTopY - 1 || y == floorTopY + MEGABRIDGE_HALF_THICKNESS) {
                                    chunk.setBlockState(pos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), false);
                                } else {
                                    chunk.setBlockState(pos, Blocks.DEEPSLATE_TILES.defaultBlockState(), false);
                                }
                                continue;
                            }
                        }
                    }

                    // -------------------------
                    // WALKWAY GENERATION
                    // -------------------------
                    if (isInCanyon) {
                        boolean hasWalk = hasWalkway(level.getSeed(), floorIndex, segZ);
                        int walkwayZCenter = hasWalk ? walkwayZCenter(level.getSeed(), floorIndex, segZ) : 0;
                        int walkwayDistZ = hasWalk ? Math.abs(worldZ - walkwayZCenter) : Integer.MAX_VALUE;
                        boolean inWalkwayZRange = hasWalk && walkwayDistZ <= WALKWAY_WIDTH / 2;
                        
                        if (inWalkwayZRange) {
                            int walkwayY = floorTopY; // or floorTopY+1, using floorTopY for now
                            
                            // Walkway spans from leftWallXAt+1 to rightWallXAt-1 (inside the void)
                            int leftWall = leftWallXAt(centerX);
                            int rightWall = rightWallXAt(centerX);
                            boolean inWalkwayXRange = worldX > leftWall && worldX < rightWall;
                            
                            if (inWalkwayXRange) {
                                // Check if walkway is hanging variant (rare)
                                boolean isHanging = hasWalk && hash01(level.getSeed() ^ WALKWAY_SALT ^ 0x14A6B1L, floorIndex, segZ) < 0.15;
                                
                                // Hanging chains: place CHAIN blocks only at walkway edges (left and right)
                                if (isHanging && walkwayDistZ == WALKWAY_WIDTH / 2) {
                                    // Chains from walkwayY+6 down to walkwayY+1, only at edges
                                    if (y >= walkwayY + 1 && y <= walkwayY + 6) {
                                        chunk.setBlockState(pos, Blocks.CHAIN.defaultBlockState(), false);
                                        continue;
                                    }
                                }
                                
                                // Railing at walkway edges
                                if (walkwayDistZ == WALKWAY_WIDTH / 2 && y == walkwayY + 1) {
                                    chunk.setBlockState(pos, Blocks.IRON_BARS.defaultBlockState(), false);
                                    continue;
                                }
                                
                                // Walkway blocks: thickness WALKWAY_THICKNESS at walkwayY
                                if (y >= walkwayY && y < walkwayY + WALKWAY_THICKNESS) {
                                    int layerInWalkway = y - walkwayY;
                                    if (layerInWalkway == 0) {
                                        // Bottom layer: IRON_BLOCK
                                        chunk.setBlockState(pos, Blocks.IRON_BLOCK.defaultBlockState(), false);
                                    } else if (layerInWalkway == WALKWAY_THICKNESS - 1) {
                                        // Top layer: LIGHT_GRAY_CONCRETE
                                        chunk.setBlockState(pos, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
                                    } else {
                                        // Middle layers: SMOOTH_STONE
                                        chunk.setBlockState(pos, Blocks.SMOOTH_STONE.defaultBlockState(), false);
                                    }
                                    continue;
                                }
                            }
                        }
                    }

                    // Erosion / placement rules
                    boolean canErode = hasFloorNoise;
                    // Disable erosion/scrambling for broken floors - it messes up other features
                    if (floorType == FLOOR_TYPE_BROKEN) {
                        canErode = false;
                    }

                    // Check if there's a ladder at this position (needed for floor carving)
                    // Since hasLadder is deterministic per (x,z), check if we're in any floor's ladder range
                    boolean isLadderPosition = false;
                    net.minecraft.core.Direction ladderFacing = null;
                    
                    if (isInCanyon && hasLadder) {
                        // Check current floor's ladder range
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
                        
                        if (y >= floorBaseY && y <= nextFloorY) {
                            isLadderPosition = true;
                            ladderFacing = worldX < centerX ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
                        } else {
                            // Also check if we're in the previous floor's ladder range (for next floor's floor layers)
                            int prevFloorIndex = floorIndex - 1;
                            if (prevFloorIndex >= 0) {
                                int prevFloorBaseY = prevFloorIndex * FLOOR_SPACING;
                                int prevFloorType = getFloorType(level.getSeed(), prevFloorIndex);
                                int prevFloorThickness;
                                if (prevFloorType == FLOOR_TYPE_CLEAN_SLAB) {
                                    prevFloorThickness = 1;
                                } else if (prevFloorType == FLOOR_TYPE_INDUSTRIAL) {
                                    prevFloorThickness = 2 + (hash01(level.getSeed() ^ FLOOR_TYPE_SALT, worldX, worldZ) > 0.5 ? 1 : 0);
                                } else {
                                    prevFloorThickness = 1;
                                }
                                int prevFloorTopY = prevFloorBaseY + prevFloorThickness;
                                
                                // Previous floor's ladder extends to current floor's top (nextFloorY)
                                if (y >= prevFloorTopY && y <= nextFloorY) {
                                    isLadderPosition = true;
                                    ladderFacing = worldX < centerX ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
                                }
                            }
                        }
                    }

                    if (isFloorLayer) {
                        // Check if we're on a bridge or walkway - if so, skip floor placement
                        boolean onBridge = false;
                        boolean onWalkway = false;
                        
                        if (isInCanyon) {
                            // Check if on megabridge (must check X, Y, and Z ranges)
                            if (inBridgeZRange && y >= floorTopY - 1 && y <= floorTopY + MEGABRIDGE_HALF_THICKNESS) {
                                int leftWall = leftWallXAt(centerX);
                                int rightWall = rightWallXAt(centerX);
                                boolean inBridgeXRange = worldX > leftWall && worldX < rightWall;
                                if (inBridgeXRange) {
                                    boolean inBrokenSection = isBridgeBroken && Math.abs(worldX - centerX) < 12;
                                    if (!inBrokenSection) {
                                        onBridge = true;
                                    }
                                }
                            }
                            
                            // Check if on walkway (must check X, Y, and Z ranges)
                            boolean hasWalk = hasWalkway(level.getSeed(), floorIndex, segZ);
                            if (hasWalk) {
                                int walkwayZCenter = walkwayZCenter(level.getSeed(), floorIndex, segZ);
                                int walkwayDistZ = Math.abs(worldZ - walkwayZCenter);
                                if (walkwayDistZ <= WALKWAY_WIDTH / 2) {
                                    int walkwayY = floorTopY;
                                    if (y >= walkwayY && y < walkwayY + WALKWAY_THICKNESS) {
                                        int leftWall = leftWallXAt(centerX);
                                        int rightWall = rightWallXAt(centerX);
                                        boolean inWalkwayXRange = worldX > leftWall && worldX < rightWall;
                                        if (inWalkwayXRange) {
                                            onWalkway = true;
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (onBridge || onWalkway) {
                            // Don't apply floor placement to bridges/walkways
                            continue;
                        }
                        
                        if (isInCanyon) {
                            // Inside canyon: only place ledges near walls
                            // But carve out space for ladders
                            if (isLadderPosition) {
                                // Carve through floor for ladder
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            } else if (distFromWall <= FLOOR_LEDGE) {
                                // For broken floors, always place blocks without erosion
                                if (floorType == FLOOR_TYPE_BROKEN) {
                                    chunk.setBlockState(pos, floorBlock, false);
                                } else if (canErode) {
                                    // For other floors, apply erosion logic
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
                                    // canErode is false but not broken floor - just place the block
                                    chunk.setBlockState(pos, floorBlock, false);
                                }
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // Outside canyon: only place floors within limited distance from canyon edge
                            int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
                            if (distIntoWall <= FLOOR_WALL_EXTENT) {
                                chunk.setBlockState(pos, floorBlock, false);
                            } else {
                                // Too far into wall - don't place floor
                                chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                            }
                        }
                    } else {
                        // Not a floor layer
                        if (isInCanyon) {
                            if (isLadderPosition && ladderFacing != null) {
                                // Place ladder block
                                chunk.setBlockState(
                                        pos,
                                        Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, ladderFacing),
                                        false
                                );
                            } else {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                            }
                        } else {
                            // -------------------------
                            // Corridor generation (new system - horizontal corridors along Z with branches, stairs, variations)
                            // -------------------------
                            if (!isInCanyon) {
                                int corridorSegZ = floorDiv(worldZ, CORRIDOR_SEGMENT);
                                boolean hasCorridor = hasCorridorSite(level.getSeed(), worldX, worldZ, corridorSegZ, floorIndex);
                                
                                if (hasCorridor) {
                                    int baseY = corridorBaseY(level.getSeed(), floorIndex, corridorSegZ);
                                    int depth = corridorDepth(level.getSeed(), floorIndex, corridorSegZ);
                                    
                                    // Calculate local Z within segment for variations
                                    int corridorBaseSegZ = corridorSegZ * CORRIDOR_SEGMENT;
                                    int actualLocalZ = worldZ - corridorBaseSegZ;
                                    
                                    // Get corridor parameters
                                    int corridorLen = corridorLength(level.getSeed(), floorIndex, corridorSegZ);
                                    int startOffset = corridorStartOffset(level.getSeed(), floorIndex, corridorSegZ);
                                    int direction = corridorDirection(level.getSeed(), floorIndex, corridorSegZ);
                                    
                                    // Check if we're within the corridor's actual length range (accounting for start offset)
                                    int relativePos = actualLocalZ - startOffset;
                                    if (relativePos < 0 || relativePos >= corridorLen) {
                                        // Outside corridor length range, skip
                                        continue;
                                    }
                                    
                                    // Get variable width and height
                                    int width = corridorWidth(level.getSeed(), floorIndex, corridorSegZ, relativePos);
                                    int height = corridorHeight(level.getSeed(), floorIndex, corridorSegZ, relativePos);
                                    
                                    // Get stairs Y offset
                                    int stairsYOffset = corridorStairsYOffset(level.getSeed(), floorIndex, corridorSegZ, relativePos);
                                    int currentBaseY = baseY + stairsYOffset;
                                    
                                    // Check if y is in the corridor height range (with stairs offset)
                                    if (y >= currentBaseY && y < currentBaseY + height) {
                                        int depthFromFace = Math.abs(worldX - centerX) - CANYON_HALF_WIDTH;
                                        
                                        // Determine if we're in the corridor based on direction
                                        boolean inMainCorridor = false;
                                        
                                        if (direction == 0 || direction == 1) {
                                            // Z-direction corridor (original behavior)
                                            inMainCorridor = depthFromFace >= depth && depthFromFace < depth + width;
                                        } else {
                                            // X-direction corridor (left/right)
                                            // For X-direction, the corridor runs along X axis at a fixed Z
                                            // Check if we're at the right Z position (within width/2 of the corridor center Z)
                                            int corridorCenterZ = corridorBaseSegZ + startOffset + (corridorLen / 2);
                                            int distFromCorridorZ = Math.abs(worldZ - corridorCenterZ);
                                            // For X-direction, we check depth and width in Z direction
                                            inMainCorridor = depthFromFace >= depth && depthFromFace < depth + width && distFromCorridorZ < (corridorLen / 2);
                                        }
                                        
                                        // Check for branches (extend corridors deeper at branch points)
                                        boolean hasBranch = hasCorridorBranch(level.getSeed(), floorIndex, corridorSegZ, relativePos);
                                        if (hasBranch && depthFromFace >= depth + width && depthFromFace < depth + width + 8 && (direction == 0 || direction == 1)) {
                                            // Branch extends 8 blocks deeper (only for Z-direction corridors)
                                            inMainCorridor = true;
                                        }
                                        
                                        // Perpendicular connector tunnels (access tunnels from facade to corridor)
                                        // Generate every 12 blocks along Z, width 2 blocks (only for Z-direction corridors)
                                        boolean inConnector = false;
                                        if (direction == 0 || direction == 1) {
                                            int connectorZ = (worldZ / 12) * 12; // Round down to nearest multiple of 12
                                            int distFromConnectorZ = Math.abs(worldZ - connectorZ);
                                            inConnector = distFromConnectorZ < 2 && depthFromFace >= 0 && depthFromFace <= depth;
                                        }
                                        
                                        // Check if connector meets main corridor (door frame location)
                                        boolean atConnectorJunction = inConnector && depthFromFace == depth;
                                        
                                        if (inMainCorridor || inConnector) {
                                            // Door frame blocks where connector meets main corridor (check first to override)
                                            if (atConnectorJunction && (y == currentBaseY || y == currentBaseY + height - 1)) {
                                                chunk.setBlockState(pos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), false);
                                            } else if (y == currentBaseY) {
                                                // Floor: just air (removed blackstone/polished deepslate tiles)
                                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                            } else if (y == currentBaseY + height - 1) {
                                                // Ceiling: place SEA_LANTERN every 8 blocks, sometimes broken
                                                if ((worldZ & 7) == 0) { // Every 8 blocks
                                                    double brokenChance = hash01(level.getSeed() ^ CORRIDOR_SALT ^ 0xB000300L, worldX, worldZ);
                                                    if (brokenChance < CORRIDOR_LIGHT_BROKEN_PROB) {
                                                        // Broken light: use dead lantern or nothing
                                                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                                    } else {
                                                        chunk.setBlockState(pos, Blocks.SEA_LANTERN.defaultBlockState(), false);
                                                    }
                                                } else {
                                                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                                }
                                            } else {
                                                // Middle layers: just air
                                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                            }
                                            
                                            continue;
                                        }
                                    }
                                }
                            }
                            
                            // -------------------------
                            // Facade carving (increased frequency + corridors)
                            // -------------------------
                            int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
                            
                            if (isInFacadeBand(worldX, centerX)) {
                                int localZ = Math.floorMod(worldZ, FACADE_Z_SPACING) - (FACADE_Z_SPACING / 2);
                                int localY = Math.floorMod(y, FACADE_Y_SPACING);

                                if (localY < ARCH_HEIGHT && Math.abs(localZ) <= (ARCH_WIDTH / 2)) {
                                    double n = valueNoise2D(level.getSeed() ^ FACADE_NOISE_SALT, worldZ, y, 32);

                                    // Lower thresholds for more frequent carving
                                    boolean allowFacade = switch (floorType) {
                                        case FLOOR_TYPE_INDUSTRIAL -> n > 0.3;  // was 0.5
                                        case FLOOR_TYPE_BROKEN -> n > 0.5;     // was 0.75
                                        default -> n > 0.2;                    // was 0.4
                                    };

                                    if (allowFacade && isInsideArch(localZ, localY)) {
                                        if (distIntoWall >= 0 && distIntoWall < FACADE_DEPTH) {
                                            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                            
                                            // Check if this facade should lead to a corridor
                                            if (distIntoWall == FACADE_DEPTH - 1 && localY >= ARCH_HEIGHT / 2 - 1 && localY <= ARCH_HEIGHT / 2 + 1) {
                                                // At the back of the facade, check for corridor
                                                if (isCorridorEntry(level.getSeed(), worldX, worldZ, y)) {
                                                    // This will be handled by corridor generation
                                                    // For now, just ensure it's carved
                                                }
                                            }
                                            continue;
                                        }
                                    }
                                }
                            }
                            
                            // Corridor generation (intricate networks)
                            if (distIntoWall >= FACADE_DEPTH && distIntoWall < FACADE_DEPTH + CORRIDOR_MAX_LENGTH_OLD) {
                                CorridorInfo corridorInfo = getCorridorInfo(
                                        level.getSeed(),
                                        worldX, worldZ, y,
                                        centerX, floorIndex
                                );
                                
                                if (corridorInfo != null && isInCorridor(worldX, worldZ, y, corridorInfo)) {
                                    // Carve corridor space
                                    int corridorLocalY = y - corridorInfo.baseY;
                                    if (corridorLocalY >= 0 && corridorLocalY < CORRIDOR_HEIGHT_OLD) {
                                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                                        continue;
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

    // Check if a megabridge exists at the given floor and segment
    private static boolean hasMegabridge(long seed, int floorIndex, int segZ) {
        double prob = hash01(seed ^ MEGABRIDGE_SALT, floorIndex, segZ);
        return prob < MEGABRIDGE_PROB;
    }

    // Check if a walkway exists at the given floor and segment
    private static boolean hasWalkway(long seed, int floorIndex, int segZ) {
        double prob = hash01(seed ^ WALKWAY_SALT, floorIndex, segZ);
        return prob < WALKWAY_PROB;
    }

    // Returns deterministic Z center for a megabridge within the segment
    private static int megabridgeZCenter(long seed, int floorIndex, int segZ) {
        double offsetFrac = hash01(seed ^ MEGABRIDGE_SALT ^ 0xBEEFL, floorIndex, segZ);
        int offset = (int) (offsetFrac * BRIDGE_SEGMENT_Z);
        // Clamp to ensure int-cast safety (offset in [0, BRIDGE_SEGMENT_Z))
        if (offset >= BRIDGE_SEGMENT_Z) offset = BRIDGE_SEGMENT_Z - 1;
        return segZ * BRIDGE_SEGMENT_Z + offset;
    }

    // Returns deterministic Z center for a walkway within the segment
    private static int walkwayZCenter(long seed, int floorIndex, int segZ) {
        double offsetFrac = hash01(seed ^ WALKWAY_SALT ^ 0xCAFEL, floorIndex, segZ);
        int offset = (int) (offsetFrac * BRIDGE_SEGMENT_Z);
        // Clamp to ensure int-cast safety (offset in [0, BRIDGE_SEGMENT_Z))
        if (offset >= BRIDGE_SEGMENT_Z) offset = BRIDGE_SEGMENT_Z - 1;
        return segZ * BRIDGE_SEGMENT_Z + offset;
    }

    // Returns the X coordinate of the left wall face at the given canyon center
    private static int leftWallXAt(int centerX) {
        return centerX - CANYON_HALF_WIDTH;
    }

    // Returns the X coordinate of the right wall face at the given canyon center
    private static int rightWallXAt(int centerX) {
        return centerX + CANYON_HALF_WIDTH;
    }

    // Check if a corridor site exists at the given chunk and segment
    // chunkX and chunkZ are world coordinates (will be converted to chunk coordinates)
    private static boolean hasCorridorSite(long seed, int chunkX, int chunkZ, int segZ, int floorIndex) {
        int chunkCoordX = chunkX >> 4;
        // Combine floorIndex into the hash by mixing it with segZ
        long hashSeed = seed ^ CORRIDOR_SALT ^ ((long) floorIndex * 0x9E3779B97F4A7C15L);
        double prob = hash01(hashSeed, chunkCoordX, segZ);
        return prob < CORRIDOR_PROB;
    }

    // Returns deterministic base Y for a corridor at the given floor and segment
    private static int corridorBaseY(long seed, int floorIndex, int segZ) {
        int base = floorIndex * FLOOR_SPACING + 2;
        double offsetFrac = hash01(seed ^ CORRIDOR_SALT ^ 0xBEEFL, floorIndex, segZ);
        int offset = (int) (offsetFrac * 4); // offset in [0, 3]
        if (offset >= 4) offset = 3;
        return base + offset;
    }

    // Returns deterministic depth for a corridor at the given floor and segment
    private static int corridorDepth(long seed, int floorIndex, int segZ) {
        double depthFrac = hash01(seed ^ CORRIDOR_SALT ^ 0xCAFEL, floorIndex, segZ);
        int depthRange = CORRIDOR_MAX_DEPTH - CORRIDOR_MIN_DEPTH + 1;
        int depth = CORRIDOR_MIN_DEPTH + (int) (depthFrac * depthRange);
        // Clamp to ensure int-cast safety
        if (depth > CORRIDOR_MAX_DEPTH) depth = CORRIDOR_MAX_DEPTH;
        return depth;
    }

    // Returns deterministic width for a corridor at a specific Z position
    private static int corridorWidth(long seed, int floorIndex, int segZ, int localZ) {
        int branchSeg = localZ / CORRIDOR_BRANCH_SEGMENT;
        long widthSeed = seed ^ CORRIDOR_SALT ^ 0x501D700L ^ ((long) floorIndex * 0x9E3779B97F4A7C15L) ^ ((long) branchSeg * 0xC13FA9A902A6328FL);
        double widthFrac = hash01(widthSeed, segZ, branchSeg);
        int widthRange = CORRIDOR_MAX_WIDTH - CORRIDOR_MIN_WIDTH + 1;
        int width = CORRIDOR_MIN_WIDTH + (int) (widthFrac * widthRange);
        if (width > CORRIDOR_MAX_WIDTH) width = CORRIDOR_MAX_WIDTH;
        return width;
    }

    // Returns deterministic height for a corridor at a specific Z position
    private static int corridorHeight(long seed, int floorIndex, int segZ, int localZ) {
        int branchSeg = localZ / CORRIDOR_BRANCH_SEGMENT;
        long heightSeed = seed ^ CORRIDOR_SALT ^ 0x831670L ^ ((long) floorIndex * 0x9E3779B97F4A7C15L) ^ ((long) branchSeg * 0xC13FA9A902A6328FL);
        double heightFrac = hash01(heightSeed, segZ, branchSeg);
        int heightRange = CORRIDOR_MAX_HEIGHT - CORRIDOR_MIN_HEIGHT + 1;
        int height = CORRIDOR_MIN_HEIGHT + (int) (heightFrac * heightRange);
        if (height > CORRIDOR_MAX_HEIGHT) height = CORRIDOR_MAX_HEIGHT;
        return height;
    }

    // Returns Y offset for stairs (can be -1, 0, or +1 for down, level, up)
    private static int corridorStairsYOffset(long seed, int floorIndex, int segZ, int localZ) {
        int stairsSeg = localZ / CORRIDOR_STAIRS_SEGMENT;
        long stairsSeed = seed ^ CORRIDOR_SALT ^ 0x574415L ^ ((long) floorIndex * 0x9E3779B97F4A7C15L) ^ ((long) stairsSeg * 0xC13FA9A902A6328FL);
        double stairsFrac = hash01(stairsSeed, segZ, stairsSeg);
        if (stairsFrac < CORRIDOR_STAIRS_PROB) {
            return -1; // Down
        } else if (stairsFrac < CORRIDOR_STAIRS_PROB * 2) {
            return 1; // Up
        }
        return 0; // Level
    }

    // Check if there's a branch at this position
    private static boolean hasCorridorBranch(long seed, int floorIndex, int segZ, int localZ) {
        int branchSeg = localZ / CORRIDOR_BRANCH_SEGMENT;
        long branchSeed = seed ^ CORRIDOR_SALT ^ 0x8040C00L ^ ((long) floorIndex * 0x9E3779B97F4A7C15L) ^ ((long) branchSeg * 0xC13FA9A902A6328FL);
        double branchFrac = hash01(branchSeed, segZ, branchSeg);
        return branchFrac < CORRIDOR_BRANCH_PROB;
    }

    // Returns deterministic length for a corridor at the given floor and segment
    private static int corridorLength(long seed, int floorIndex, int segZ) {
        double lengthFrac = hash01(seed ^ CORRIDOR_SALT ^ 0x1356750L, floorIndex, segZ);
        int lengthRange = CORRIDOR_MAX_LENGTH - CORRIDOR_MIN_LENGTH + 1;
        int length = CORRIDOR_MIN_LENGTH + (int) (lengthFrac * lengthRange);
        // Clamp to ensure int-cast safety
        if (length > CORRIDOR_MAX_LENGTH) length = CORRIDOR_MAX_LENGTH;
        return length;
    }

    // Returns deterministic start offset for a corridor within its segment
    // This allows corridors to start at different positions, not always at segment start
    private static int corridorStartOffset(long seed, int floorIndex, int segZ) {
        int maxOffset = Math.max(0, CORRIDOR_SEGMENT - CORRIDOR_MIN_LENGTH);
        double offsetFrac = hash01(seed ^ CORRIDOR_SALT ^ 0x5744070L, floorIndex, segZ);
        int offset = (int) (offsetFrac * (maxOffset + 1));
        if (offset > maxOffset) offset = maxOffset;
        return offset;
    }

    // Returns corridor direction: 0=Z+, 1=Z-, 2=X+ (right), 3=X- (left)
    private static int corridorDirection(long seed, int floorIndex, int segZ) {
        double dirFrac = hash01(seed ^ CORRIDOR_SALT ^ 0x013C700L, floorIndex, segZ);
        return (int) (dirFrac * 4);
    }

    // Returns turn direction at a position: -1=left, 0=straight, 1=right
    // currentDir: 0=Z+, 1=Z-, 2=X+, 3=X-
    private static int corridorTurn(long seed, int floorIndex, int segZ, int localPos) {
        int turnSeg = localPos / CORRIDOR_TURN_SEGMENT;
        long turnSeed = seed ^ CORRIDOR_SALT ^ 0x7470000L ^ ((long) floorIndex * 0x9E3779B97F4A7C15L) ^ ((long) turnSeg * 0xC13FA9A902A6328FL);
        double turnFrac = hash01(turnSeed, segZ, turnSeg);
        if (turnFrac < CORRIDOR_TURN_PROB) {
            return -1; // Turn left
        } else if (turnFrac < CORRIDOR_TURN_PROB * 2) {
            return 1; // Turn right
        }
        return 0; // Straight
    }

    // Helper class to hold vertical shaft information
    private static class ShaftInfo {
        final int shaftX;
        final int shaftZ;

        ShaftInfo(int shaftX, int shaftZ) {
            this.shaftX = shaftX;
            this.shaftZ = shaftZ;
        }
    }

    // Returns ShaftInfo for vertical shaft, or null if no shaft at this location
    private static ShaftInfo getShaftInfo(
            long seed,
            int chunkBlockX, int chunkBlockZ,
            int floorIndex,
            int worldX, int worldZ,
            int centerX
    ) {
        int cx = chunkBlockX >> 4;
        int cz = chunkBlockZ >> 4;

        long h = seed ^ SHAFT_SALT ^ (long) floorIndex * 1315423911L ^ cx * 73428767L ^ cz * 912367L;

        if ((h & 0xFF) >= (int) (SHAFT_PROBABILITY * 256)) return null;

        // Determine shaft position
        int xOffset = (int) ((h >>> 8) & 15); // 0..15
        int zOffset = (int) ((h >>> 12) & 15); // 0..15
        
        int shaftX = (cx << 4) + xOffset;
        int shaftZ = (cz << 4) + zOffset;

        // Must be in wall (not inside canyon)
        int distFromCenter = Math.abs(shaftX - centerX);
        if (distFromCenter < CANYON_HALF_WIDTH) return null;

        // Depth into wall
        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
        if (distIntoWall < 0 || distIntoWall >= SHAFT_DEPTH) return null;

        // Check if this position is within the shaft area
        int localX = worldX - shaftX;
        int localZ = worldZ - shaftZ;
        if (Math.abs(localX) <= SHAFT_WIDTH / 2 && Math.abs(localZ) <= SHAFT_WIDTH / 2) {
            return new ShaftInfo(shaftX, shaftZ);
        }

        return null;
    }

    // Helper class to hold corridor information
    private static class CorridorInfo {
        final int startX, startZ, startY;
        final int baseY;
        final int length;
        final int direction; // 0=X+, 1=X-, 2=Z+, 3=Z-
        final int[] branches; // branch points and directions

        CorridorInfo(int startX, int startZ, int startY, int baseY, int length, int direction, int[] branches) {
            this.startX = startX;
            this.startZ = startZ;
            this.startY = startY;
            this.baseY = baseY;
            this.length = length;
            this.direction = direction;
            this.branches = branches;
        }
    }

    // Check if a facade entry should lead to a corridor
    private static boolean isCorridorEntry(long seed, int worldX, int worldZ, int y) {
        double prob = hash01(seed ^ CORRIDOR_SALT, worldX, worldZ ^ y);
        return prob < CORRIDOR_PROBABILITY;
    }

    // Get corridor information for a given position
    private static CorridorInfo getCorridorInfo(
            long seed,
            int worldX, int worldZ, int y,
            int centerX, int floorIndex
    ) {
        // Find the facade entry point (at FACADE_DEPTH - 1)
        int distFromCenter = Math.abs(worldX - centerX);
        int distIntoWall = distFromCenter - CANYON_HALF_WIDTH;
        
        if (distIntoWall < FACADE_DEPTH) return null;

        // Check if we're near a potential corridor start
        int facadeEntryX = (worldX < centerX) ? centerX - CANYON_HALF_WIDTH - FACADE_DEPTH + 1 
                                               : centerX + CANYON_HALF_WIDTH + FACADE_DEPTH - 1;
        
        // Align to grid for deterministic corridor placement
        int gridX = floorDiv(worldX, FACADE_Z_SPACING) * FACADE_Z_SPACING;
        int gridZ = floorDiv(worldZ, FACADE_Z_SPACING) * FACADE_Z_SPACING;
        int gridY = floorDiv(y, FACADE_Y_SPACING) * FACADE_Y_SPACING;

        // Check if this is a corridor entry point
        if (Math.abs(worldX - facadeEntryX) <= 2 && 
            Math.abs(worldZ - gridZ) <= 2 &&
            Math.abs(y - gridY - FACADE_Y_SPACING / 2) <= 2) {
            
            // Generate corridor parameters deterministically
            long corridorSeed = seed ^ CORRIDOR_SALT ^ (long) gridX * 0x9E3779B97F4A7C15L 
                                ^ (long) gridZ * 0xC13FA9A902A6328FL ^ (long) gridY * 0x5EED1E5FL;
            
            int length = CORRIDOR_MIN_LENGTH_OLD + (int) ((hash01(corridorSeed, 0, 0) * (CORRIDOR_MAX_LENGTH_OLD - CORRIDOR_MIN_LENGTH_OLD)));
            int direction = (int) (hash01(corridorSeed, 1, 0) * 4); // 0-3
            
            // Generate branches
            int[] branches = new int[CORRIDOR_MAX_BRANCHES * 2]; // [position, direction] pairs
            int branchCount = 0;
            for (int i = 2; i < length - 2 && branchCount < CORRIDOR_MAX_BRANCHES; i++) {
                if (hash01(corridorSeed, i, 0) < CORRIDOR_BRANCH_PROBABILITY) {
                    branches[branchCount * 2] = i;
                    branches[branchCount * 2 + 1] = (int) (hash01(corridorSeed, i, 1) * 4);
                    branchCount++;
                }
            }
            
            return new CorridorInfo(facadeEntryX, gridZ, gridY + FACADE_Y_SPACING / 2, 
                                    gridY + FACADE_Y_SPACING / 2, length, direction, branches);
        }

        return null;
    }

    // Check if a position is inside a corridor
    private static boolean isInCorridor(int worldX, int worldZ, int y, CorridorInfo corridor) {
        // Check main corridor
        int dx = worldX - corridor.startX;
        int dz = worldZ - corridor.startZ;
        int dy = y - corridor.baseY;
        
        if (dy < 0 || dy >= CORRIDOR_HEIGHT_OLD) return false;

        boolean inMain = false;
        switch (corridor.direction) {
            case 0: // X+
                inMain = (dx >= 0 && dx < corridor.length && Math.abs(dz) <= CORRIDOR_WIDTH_OLD / 2);
                break;
            case 1: // X-
                inMain = (dx <= 0 && dx > -corridor.length && Math.abs(dz) <= CORRIDOR_WIDTH_OLD / 2);
                break;
            case 2: // Z+
                inMain = (dz >= 0 && dz < corridor.length && Math.abs(dx) <= CORRIDOR_WIDTH_OLD / 2);
                break;
            case 3: // Z-
                inMain = (dz <= 0 && dz > -corridor.length && Math.abs(dx) <= CORRIDOR_WIDTH_OLD / 2);
                break;
        }

        if (inMain) return true;

        // Check branches
        for (int i = 0; i < corridor.branches.length; i += 2) {
            if (corridor.branches[i] == 0) break; // End of branches
            
            int branchPos = corridor.branches[i];
            int branchDir = corridor.branches[i + 1];
            
            int branchX = corridor.startX;
            int branchZ = corridor.startZ;
            
            // Calculate branch start position based on main corridor direction
            switch (corridor.direction) {
                case 0: branchX += branchPos; break;
                case 1: branchX -= branchPos; break;
                case 2: branchZ += branchPos; break;
                case 3: branchZ -= branchPos; break;
            }
            
            int bdx = worldX - branchX;
            int bdz = worldZ - branchZ;
            
            boolean inBranch = false;
            switch (branchDir) {
                case 0: // X+
                    inBranch = (bdx >= 0 && bdx < CORRIDOR_MAX_LENGTH_OLD / 2 && Math.abs(bdz) <= CORRIDOR_WIDTH_OLD / 2);
                    break;
                case 1: // X-
                    inBranch = (bdx <= 0 && bdx > -CORRIDOR_MAX_LENGTH / 2 && Math.abs(bdz) <= CORRIDOR_WIDTH_OLD / 2);
                    break;
                case 2: // Z+
                    inBranch = (bdz >= 0 && bdz < CORRIDOR_MAX_LENGTH / 2 && Math.abs(bdx) <= CORRIDOR_WIDTH_OLD / 2);
                    break;
                case 3: // Z-
                    inBranch = (bdz <= 0 && bdz > -CORRIDOR_MAX_LENGTH / 2 && Math.abs(bdx) <= CORRIDOR_WIDTH_OLD / 2);
                    break;
            }
            
            if (inBranch) return true;
        }

        return false;
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

    /**
     * Finds a safe spawn location in the Netsphere dimension
     * @param level The server level
     * @param preferredX Preferred X coordinate (will be adjusted to be in canyon)
     * @param preferredZ Preferred Z coordinate
     * @return BlockPos with safe spawn location, or null if none found
     */
    public static net.minecraft.core.BlockPos findSafeSpawnLocation(ServerLevel level, int preferredX, int preferredZ) {
        long seed = level.getSeed();
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight(), 256);
        
        // Calculate canyon center at preferred Z
        int canyonCenterX = computeCanyonCenterX(seed, preferredZ);
        
        // Find a safe X position inside the canyon (not too close to walls, near center)
        // Try positions from center outward
        int[] xOffsets = {0, 10, -10, 20, -20, 30, -30, 40, -40, 50, -50, 60, -60, 70, -70};
        
        for (int xOffset : xOffsets) {
            int testX = canyonCenterX + xOffset;
            int distFromCenter = Math.abs(testX - canyonCenterX);
            
            // Must be inside canyon (with some margin from walls)
            if (distFromCenter >= CANYON_HALF_WIDTH - 5) continue;
            
            // Try to find a safe Y position on a floor
            // Check floors from middle of build height downward, then upward
            int startFloor = (minY + maxY) / 2 / FLOOR_SPACING;
            
                    // Check floors downward first
            for (int floorOffset = 0; floorOffset < 20; floorOffset++) {
                int floorIndex = startFloor - floorOffset;
                if (floorIndex < 0) break;
                
                int floorBaseY = floorIndex * FLOOR_SPACING;
                int floorType = getFloorType(seed, floorIndex);
                int floorThickness;
                if (floorType == FLOOR_TYPE_CLEAN_SLAB) {
                    floorThickness = 1;
                } else if (floorType == FLOOR_TYPE_INDUSTRIAL) {
                    floorThickness = 2 + (hash01(seed ^ FLOOR_TYPE_SALT, testX, preferredZ) > 0.5 ? 1 : 0);
                } else {
                    floorThickness = 1;
                }
                int floorTopY = floorBaseY + floorThickness;
                
                // Check if this floor is in valid Y range
                if (floorTopY < minY || floorTopY >= maxY) continue;
                
                // Check if there's a solid floor block and air above
                net.minecraft.core.BlockPos floorPos = new net.minecraft.core.BlockPos(testX, floorTopY, preferredZ);
                net.minecraft.core.BlockPos abovePos = floorPos.above();
                
                // Generate and ensure chunks are fully loaded and post-processed
                int chunkX = floorPos.getX() >> 4;
                int chunkZ = floorPos.getZ() >> 4;
                var chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, true);
                
                if (chunkAccess != null && chunkAccess instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
                    // Ensure chunk is post-processed (safe to call multiple times)
                    processChunk(level, levelChunk);
                    
                    var floorState = level.getBlockState(floorPos);
                    var aboveState = level.getBlockState(abovePos);
                    
                    // Check if floor is solid and above is air (or passable)
                    if (floorState.canOcclude() && (aboveState.isAir() || aboveState.getCollisionShape(level, abovePos).isEmpty())) {
                        // Found a safe spawn location
                        return abovePos; // Spawn on top of floor
                    }
                }
            }
            
            // Also check floors upward
            for (int floorOffset = 1; floorOffset < 20; floorOffset++) {
                int floorIndex = startFloor + floorOffset;
                
                int floorBaseY = floorIndex * FLOOR_SPACING;
                int floorType = getFloorType(seed, floorIndex);
                int floorThickness;
                if (floorType == FLOOR_TYPE_CLEAN_SLAB) {
                    floorThickness = 1;
                } else if (floorType == FLOOR_TYPE_INDUSTRIAL) {
                    floorThickness = 2 + (hash01(seed ^ FLOOR_TYPE_SALT, testX, preferredZ) > 0.5 ? 1 : 0);
                } else {
                    floorThickness = 1;
                }
                int floorTopY = floorBaseY + floorThickness;
                
                // Check if this floor is in valid Y range
                if (floorTopY < minY || floorTopY >= maxY) continue;
                
                // Check if there's a solid floor block and air above
                net.minecraft.core.BlockPos floorPos = new net.minecraft.core.BlockPos(testX, floorTopY, preferredZ);
                net.minecraft.core.BlockPos abovePos = floorPos.above();
                
                // Generate and ensure chunks are fully loaded and post-processed
                int chunkX = floorPos.getX() >> 4;
                int chunkZ = floorPos.getZ() >> 4;
                var chunkAccess = level.getChunkSource().getChunk(chunkX, chunkZ, true);
                
                if (chunkAccess != null && chunkAccess instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
                    // Ensure chunk is post-processed (safe to call multiple times)
                    processChunk(level, levelChunk);
                    
                    var floorState = level.getBlockState(floorPos);
                    var aboveState = level.getBlockState(abovePos);
                    
                    // Check if floor is solid and above is air (or passable)
                    if (floorState.canOcclude() && (aboveState.isAir() || aboveState.getCollisionShape(level, abovePos).isEmpty())) {
                        // Found a safe spawn location
                        return abovePos; // Spawn on top of floor
                    }
                }
            }
        }
        
        // Fallback: return a position near canyon center at a reasonable Y
        int fallbackY = Math.max(minY + 64, (minY + maxY) / 2);
        return new net.minecraft.core.BlockPos(canyonCenterX, fallbackY, preferredZ);
    }
}
