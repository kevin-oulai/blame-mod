package net.liferquest.blamemod.item;

import net.liferquest.blamemod.BlameMod;
import net.liferquest.blamemod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlameMod.MOD_ID);

//    public static final RegistryObject<CreativeModeTab> SPECIAL_TOTEMS_TAB = CREATIVE_MODE_TABS.register(
//            "special_totems_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.CREATION_TOTEM.get()))
//                    .title(Component.translatable("creativetab.imaginarium.special_totems"))
//                    .displayItems((itemDisplayParameters, output) -> {
//                        output.accept(ModItems.CREATION_TOTEM.get());
//                        output.accept(ModItems.REVEAL_TOTEM.get());
//                        output.accept(ModItems.SCULPTURE_TOTEM.get());
//                        output.accept(ModItems.METAMORPHOSIS_TOTEM.get());
//                    })
//                    .build()
//    );

//    public static final RegistryObject<CreativeModeTab> TRAVEL_ITEMS_TAB = CREATIVE_MODE_TABS.register(
//            "travel_items_tab", () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.RED_PAINT_TUBE.get()))
//                    .withTabsBefore(SPECIAL_TOTEMS_TAB.getId())
//                    .title(Component.translatable("creativetab.imaginarium.travel_items"))
//                    .displayItems((itemDisplayParameters, output) -> {
//                        output.accept(ModItems.RED_PAINT_TUBE.get());
//                        output.accept(ModBlocks.CRACKED_PORTAL_BLOCK.get());
//                    })
//                    .build()
//    );

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
