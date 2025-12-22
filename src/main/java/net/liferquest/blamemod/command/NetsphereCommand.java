package net.liferquest.blamemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.liferquest.blamemod.worldgen.ModDimensions;
import net.liferquest.blamemod.worldgen.NetsphereChunkPostProcessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to teleport players to the Netsphere dimension
 */
@SuppressWarnings("null")
public class NetsphereCommand {
    
    /**
     * Registers the /netsphere command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("netsphere")
                        .requires(source -> source.hasPermission(2)) // Requires operator permission
                        .executes(NetsphereCommand::execute)
        );
    }
    
    /**
     * Executes the netsphere teleportation
     */
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check if the source is a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }
        
        // Get the Netsphere dimension
        ServerLevel netsphereLevel = source.getServer().getLevel(ModDimensions.NETSPHERE_LEVEL);
        
        if (netsphereLevel == null) {
            source.sendFailure(Component.literal("Netsphere dimension not found!"));
            return 0;
        }
        
        // Get current position
        int currentX = player.blockPosition().getX();
        int currentZ = player.blockPosition().getZ();
        
        // Find safe spawn location (inside canyon, on a floor)
        BlockPos safePos = NetsphereChunkPostProcessor.findSafeSpawnLocation(netsphereLevel, currentX, currentZ);
        
        if (safePos == null) {
            source.sendFailure(Component.literal("Could not find a safe spawn location!"));
            return 0;
        }
        
        // Ensure the chunk is loaded
        netsphereLevel.getChunk(safePos);
        
        // Teleport the player to safe location
        player.teleportTo(netsphereLevel, safePos.getX() + 0.5, safePos.getY() + 0.1, safePos.getZ() + 0.5, player.getYRot(), player.getXRot());
        
        source.sendSuccess(() -> Component.literal(String.format("Teleported to the Netsphere at (%d, %d, %d)!", 
                safePos.getX(), safePos.getY(), safePos.getZ())), true);
        return 1;
    }
}
