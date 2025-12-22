package net.liferquest.blamemod.block;

import net.liferquest.blamemod.worldgen.ModDimensions;
import net.liferquest.blamemod.worldgen.NetsphereChunkPostProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@SuppressWarnings("null")
public class RedstoneBedBlock extends Block {
    public RedstoneBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Check if block is powered by redstone
        boolean isPowered = level.hasNeighborSignal(pos);

        if (!isPowered) {
            // If not powered, do nothing (or could show a message)
            player.sendSystemMessage(Component.literal("The bed needs to be powered by redstone to work!"));
            return InteractionResult.FAIL;
        }

        // If powered, teleport to Netsphere dimension
        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel netsphereLevel = serverPlayer.server.getLevel(ModDimensions.NETSPHERE_LEVEL);
            
            if (netsphereLevel == null) {
                player.sendSystemMessage(Component.literal("Netsphere dimension not found!"));
                return InteractionResult.FAIL;
            }

            // Get current position
            int currentX = player.blockPosition().getX();
            int currentZ = player.blockPosition().getZ();

            // Find safe spawn location
            BlockPos safePos = NetsphereChunkPostProcessor.findSafeSpawnLocation(netsphereLevel, currentX, currentZ);

            if (safePos == null) {
                player.sendSystemMessage(Component.literal("Could not find a safe spawn location in Netsphere!"));
                return InteractionResult.FAIL;
            }

            // Ensure the chunk is loaded
            netsphereLevel.getChunkSource().getChunk(safePos.getX() >> 4, safePos.getZ() >> 4, true);

            // Teleport the player
            serverPlayer.teleportTo(netsphereLevel, safePos.getX() + 0.5, safePos.getY() + 0.1, safePos.getZ() + 0.5, 
                serverPlayer.getYRot(), serverPlayer.getXRot());
            
            player.sendSystemMessage(Component.literal("Entering the Netsphere..."));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}

