package net.liferquest.blamemod.worldgen;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class BlameBiomes {
    /**
     * Creates a minimal Blame City Canyon biome
     * @return a biome with neutral gray colors, no spawns, and no features
     */
    public static Biome blameCityCanyon() {
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();
        
        BiomeSpecialEffects biomeSpecialEffects = new BiomeSpecialEffects.Builder()
                .fogColor(0x8B8B8B)      // Neutral gray fog
                .skyColor(0xA9A9A9)      // Light gray sky
                .waterColor(0x696969)    // Dark gray water
                .waterFogColor(0x696969) // Dark gray water fog
                .build();
        
        return new Biome.BiomeBuilder()
                .temperature(0.5f)
                .downfall(0.0f)
                .specialEffects(java.util.Objects.requireNonNull(biomeSpecialEffects))
                .mobSpawnSettings(java.util.Objects.requireNonNull(mobSpawnSettings))
                .build();
    }
}
