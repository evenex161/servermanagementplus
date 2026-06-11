package com.servermanagement.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class NbtHelper {

    public static CompoundTag saveItemStack(ItemStack stack, HolderLookup.Provider registryAccess) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        Tag tag = ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode ItemStack: " + msg));
        return tag instanceof CompoundTag ? (CompoundTag) tag : new CompoundTag();
    }

    public static ItemStack loadItemStack(CompoundTag tag, HolderLookup.Provider registryAccess) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        return ItemStack.OPTIONAL_CODEC.parse(ops, tag)
                .getOrThrow(msg -> new IllegalStateException("Failed to parse ItemStack: " + msg));
    }

    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        if (uuid != null) {
            tag.store(key, UUIDUtil.CODEC, uuid);
        }
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(null);
    }
}
