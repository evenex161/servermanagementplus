package com.servermanagement.event;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.slimehead.SlimeHeadManager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ServerManagementMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
