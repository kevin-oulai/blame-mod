package net.liferquest.blamemod.item;

import net.liferquest.blamemod.BlameMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BlameMod.MOD_ID);

//    public static final RegistryObject<Item> REVEAL_TOTEM = ITEMS.register("reveal_totem",
//            () -> new Item(new Item.Properties()));
    
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
