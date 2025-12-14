package net.liferquest.blamemod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.liferquest.blamemod.worldgen.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to teleport players to the Netsphere dimension
 */
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
        double x = player.getX();
        double z = player.getZ();
        
        // Find safe Y position (spawn point or default)
        double y = netsphereLevel.getSharedSpawnPos().getY();
        if (y < netsphereLevel.getMinBuildHeight()) {
            y = netsphereLevel.getMinBuildHeight() + 64; // Safe default height
        }
        
        // Teleport the player
        player.teleportTo(netsphereLevel, x, y, z, player.getYRot(), player.getXRot());
        
        source.sendSuccess(() -> Component.literal("Teleported to the Netsphere!"), true);
        return 1;
    }
}
