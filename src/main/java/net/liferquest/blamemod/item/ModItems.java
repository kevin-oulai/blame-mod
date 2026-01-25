package net.liferquest.blamemod.item;

import net.liferquest.blamemod.BlameMod;
import net.liferquest.blamemod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("null")
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(Registries.ITEM, BlameMod.MOD_ID);

    public static final RegistryObject<Item> REDSTONE_BED = ITEMS.register("redstone_bed",
        () -> new BlockItem(ModBlocks.REDSTONE_BED.get(), new Item.Properties())); // get() is non-null at registration time

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

