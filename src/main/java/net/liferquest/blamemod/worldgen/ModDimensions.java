package net.liferquest.blamemod.worldgen;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Defines dimension keys for the Netsphere custom dimension
 */
@SuppressWarnings("null")
public class ModDimensions {
    
    // Netsphere dimension keys
    public static final ResourceKey<Level> NETSPHERE_LEVEL = 
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
    
    public static final ResourceKey<LevelStem> NETSPHERE_STEM = 
            ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
    
    public static final ResourceKey<DimensionType> NETSPHERE_TYPE = 
            ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
}