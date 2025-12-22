package net.liferquest.blamemod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.liferquest.blamemod.worldgen.NetsphereChunkPostProcessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("null")
public class CanyonInfoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("canyon_info")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                int playerX = player.blockPosition().getX();
                int playerZ = player.blockPosition().getZ();
                long seed = player.serverLevel().getSeed();
                
                int canyonCenter = NetsphereChunkPostProcessor.computeCanyonCenterX(seed, playerZ);
                int distanceFromCenter = Math.abs(playerX - canyonCenter);
                boolean inCanyon = distanceFromCenter < 80;
                
                player.sendSystemMessage(Component.literal(
                    String.format("Canyon center at X=%d (your Z=%d)\nYou are %d blocks from center\n%s",
                        canyonCenter, playerZ, distanceFromCenter, 
                        inCanyon ? "§aInside canyon" : "§cOutside canyon")
                ));
                return 1;
            })
        );
    }
}
