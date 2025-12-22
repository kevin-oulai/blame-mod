package net.liferquest.blamemod;

import com.mojang.logging.LogUtils;
import net.liferquest.blamemod.block.ModBlocks;
import net.liferquest.blamemod.command.CanyonInfoCommand;
import net.liferquest.blamemod.command.NetsphereCommand;
import net.liferquest.blamemod.item.ModItems;
import net.liferquest.blamemod.worldgen.ModBiomes;
import net.liferquest.blamemod.worldgen.NetsphereChunkPostProcessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Gradle command: ./gradlew runClient

// The value here should match an entry in the META-INF/mods.toml file
@Mod(BlameMod.MOD_ID)
public class BlameMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "blamemod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlameMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(NetsphereChunkPostProcessor.class);

        // Register custom biomes
        ModBiomes.register(modEventBus);
        
        // Register custom blocks
        ModBlocks.register(modEventBus);
        
        // Register custom items
        ModItems.register(modEventBus);
        
        // Note: Dimensions (LevelStems) are registered via JSON data files in data/blamemod/dimension/
        // and data/blamemod/dimension_type/, not through DeferredRegister in 1.21+

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }



    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NetsphereCommand.register(event.getDispatcher());
        CanyonInfoCommand.register(event.getDispatcher());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
