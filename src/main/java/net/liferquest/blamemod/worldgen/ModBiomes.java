package net.liferquest.blamemod.worldgen;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBiomes {
    // DeferredRegister for managing biome registrations
    public static final DeferredRegister<Biome> BIOMES = 
        DeferredRegister.create(Registries.BIOME, BlameMod.MOD_ID);

    // Define the custom biome: Blame City Canyon
    @SuppressWarnings("null")
    public static final ResourceKey<Biome> BLAME_CITY_CANYON = 
        ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "blame_city_canyon"));

    /**
     * Registers all biomes to the event bus
     * @param bus the event bus to register to
     */
    public static void register(IEventBus bus) {
        BIOMES.register(bus);
    }
}
