package com.servermanagement.security;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.servermanagement.ServerManagementMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

/**
 * GSON TypeAdapter for ItemStack to avoid Java module access issues
 * with Optional fields in ItemStack internals.
 * 
 * Uses TypeAdapter (not JsonSerializer/JsonDeserializer) for reliable
 * type matching under Forge's classloading environment with Gson 2.10.
 */
public class ItemStackTypeAdapter extends TypeAdapter<ItemStack> {

    @Override
    public void write(JsonWriter out, ItemStack src) throws IOException {
        if (src == null || src.isEmpty()) {
            out.nullValue();
            return;
        }

        out.beginObject();

        // Store item registry name
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(src.getItem());
        out.name("item").value(itemId.toString());

        // Store count
        out.name("count").value(src.getCount());

        // Store NBT data if present (1.20.1 uses ItemStack.getTag() directly)
        CompoundTag tag = src.getTag();
        if (tag != null && !tag.isEmpty()) {
            out.name("nbt").value(tag.toString());
        }

        out.endObject();
    }

    @Override
    public ItemStack read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return ItemStack.EMPTY;
        }

        String itemId = null;
        int count = 1;
        String nbtString = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "item":
                    itemId = in.nextString();
                    break;
                case "count":
                    count = in.nextInt();
                    break;
                case "nbt":
                    nbtString = in.nextString();
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(resourceLocation);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item, count);

        // Apply NBT if present (1.20.1 sets directly on the ItemStack)
        if (nbtString != null) {
            try {
                CompoundTag tag = TagParser.parseTag(nbtString);
                stack.setTag(tag);
            } catch (Exception e) {
                ServerManagementMod.LOGGER.warn("Failed to parse ItemStack NBT: {}", e.getMessage());
            }
        }

        return stack;
    }
}
