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
 * Uses lazy initialization to avoid early registry access during class loading
 */
@SuppressWarnings("null")
public class ModDimensions {
    
    // Netsphere dimension keys - lazy initialized to avoid early registry access
    private static ResourceKey<Level> netsphereLevel;
    private static ResourceKey<LevelStem> netsphereStem;
    private static ResourceKey<DimensionType> netsphereType;
    
    public static ResourceKey<Level> NETSPHERE_LEVEL() {
        if (netsphereLevel == null) {
            netsphereLevel = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
        }
        return netsphereLevel;
    }
    
    public static ResourceKey<LevelStem> NETSPHERE_STEM() {
        if (netsphereStem == null) {
            netsphereStem = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
        }
        return netsphereStem;
    }
    
    public static ResourceKey<DimensionType> NETSPHERE_TYPE() {
        if (netsphereType == null) {
            netsphereType = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(BlameMod.MOD_ID, "netsphere"));
        }
        return netsphereType;
    }
}