package net.liferquest.blamemod.worldgen;

import com.mojang.datafixers.util.Pair;
import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Handles LevelStem registration for custom dimensions
 */
public class ModLevelStems {
    
    /**
     * Bootstrap the Netsphere dimension's LevelStem
     */
    public static void bootstrap(BootstrapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        
        // Create biome source with only blame_city_canyon
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
                new Climate.ParameterList<>(java.util.List.of(
                        Pair.of(Climate.parameters(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), 
                                biomes.getOrThrow(ModBiomes.BLAME_CITY_CANYON))
                ))
        );
        
        // Create chunk generator using overworld noise settings
        NoiseBasedChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
                biomeSource,
                noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD)
        );
        
        // Register the LevelStem
        context.register(
                ModDimensions.NETSPHERE_STEM,
                new LevelStem(
                        dimensionTypes.getOrThrow(ModDimensions.NETSPHERE_TYPE),
                        chunkGenerator
                )
        );
    }
}
