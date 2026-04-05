package com.servermanagement.event;

import net.neoforged.fml.common.EventBusSubscriber;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.slimehead.SlimeHeadManager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CreativeTabHandler {
    
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        // Add slime head to Tools & Utilities tab
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ItemStack slimeHead = SlimeHeadManager.createSlimeHead();
            event.accept(slimeHead);
        }
    }
}
