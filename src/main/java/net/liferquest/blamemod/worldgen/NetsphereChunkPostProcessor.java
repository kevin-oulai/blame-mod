package net.liferquest.blamemod.worldgen;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Post-processes chunks in the Netsphere dimension to create canyon structures
 */
public class NetsphereChunkPostProcessor {
    
    private static final int CANYON_CENTER_X = 0;
    private static final int CANYON_HALF_WIDTH = 80;
    
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
        if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk)) {
            return;
        }
        
        processChunk(serverLevel, chunk);
    }
    
    private static void processChunk(ServerLevel level, ChunkAccess chunk) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        
        // Get chunk coordinates
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        
        // Fill the entire chunk with white concrete, then carve canyon
        for (int x = 0; x < 16; x++) {
            int worldX = chunkX + x;
            boolean isInCanyon = Math.abs(worldX - CANYON_CENTER_X) < CANYON_HALF_WIDTH;
            
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(chunkX + x, y, chunkZ + z);
                    
                    if (isInCanyon) {
                        // Carve canyon - set to air
                        chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                    } else {
                        // Fill with white concrete
                        chunk.setBlockState(pos, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
                    }
                }
            }
        }
        
        // Mark chunk as modified
        chunk.setUnsaved(true);
    }
}
