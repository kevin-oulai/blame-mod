package net.liferquest.blamemod.worldgen;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModBiomes {
    // DeferredRegister for managing biome registrations
    public static final DeferredRegister<Biome> BIOMES = 
        DeferredRegister.create(Registries.BIOME, BlameMod.MOD_ID);

    // Define the custom biome: Blame City Canyon - lazy initialized to avoid early registry access
    private static ResourceKey<Biome> blameCityCanyon;
    
    @SuppressWarnings("null")
    public static ResourceKey<Biome> BLAME_CITY_CANYON() {
        if (blameCityCanyon == null) {
            blameCityCanyon = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "blame_city_canyon"));
        }
        return blameCityCanyon;
    }

    /**
     * Registers all biomes to the event bus
     * @param bus the event bus to register to
     */
    public static void register(IEventBus bus) {
        BIOMES.register(bus);
    }
}
