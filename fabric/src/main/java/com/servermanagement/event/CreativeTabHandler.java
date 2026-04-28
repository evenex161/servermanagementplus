package com.servermanagement.event;

import com.servermanagement.ServerManagementMod;
import com.servermanagement.config.ModConfig;
import com.servermanagement.features.slimehead.SlimeHeadManager;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
public class CreativeTabHandler {
    
    public static void buildContents(net.minecraft.world.item.CreativeModeTab tab, net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters params, net.minecraft.world.item.CreativeModeTab.Output output) {
        // Add slime head to Tools & Utilities tab
        // In Fabric, the tab is already filtered by the ItemGroupEvents registration
        ItemStack slimeHead = SlimeHeadManager.createSlimeHead();
        output.accept(slimeHead);
    }
}
