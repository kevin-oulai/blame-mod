package net.liferquest.blamemod.block;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(Registries.BLOCK, BlameMod.MOD_ID);

    public static final RegistryObject<Block> REDSTONE_BED = BLOCKS.register("redstone_bed",
        () -> new RedstoneBedBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .strength(0.2f)
            .noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

