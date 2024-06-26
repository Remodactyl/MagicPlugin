package com.elmakers.mine.bukkit.utility.platform.modern;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.utility.CompatibilityConstants;
import com.elmakers.mine.bukkit.utility.ReflectionUtils;
import com.elmakers.mine.bukkit.utility.platform.Platform;
import com.elmakers.mine.bukkit.utility.platform.base.NBTUtilsBase;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NBTUtils extends NBTUtilsBase {
    public NBTUtils(Platform platform) {
        super(platform);
    }

    @Override
    public Object getTag(ItemStack stack, String tag) {
        if (platform.getItemUtils().isEmpty(stack)) return null;
        Object tagObject = getTag(stack);
        if (tagObject == null || !(tagObject instanceof CompoundTag)) return null;
        return ((CompoundTag)tagObject).get(tag);
    }

    @Override
    public Object getTag(Object nbtBase, String tag) {
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return null;
        return ((CompoundTag)nbtBase).get(tag);
    }

    @Override
    public Object getTag(ItemStack itemStack) {
        Object tag = null;
        try {
            Object mcItemStack = getHandle(itemStack);
            if (mcItemStack == null) {
                if (itemStack.hasItemMeta()) {
                    mcItemStack = getHandle(itemStack);
                }
            }
            if (mcItemStack == null) return null;
            net.minecraft.world.item.ItemStack stack = (net.minecraft.world.item.ItemStack)mcItemStack;
            tag = stack.getTag();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return tag;
    }

    @Override
    public Set<String> getAllKeys(Object nbtBase) {
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return null;
        return ((CompoundTag)nbtBase).getAllKeys();
    }

    @Override
    public boolean contains(Object nbtBase, String tag) {
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return false;
        return ((CompoundTag)nbtBase).contains(tag);
    }

    @Override
    public Object createTag(Object nbtBase, String tag) {
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return null;

        CompoundTag compoundTag = (CompoundTag)nbtBase;
        CompoundTag meta = compoundTag.getCompound(tag);
        // Strangely getCompound always returns non-null, but the tag it returns
        // if not found in the parent is not connected to the parent.
        compoundTag.put(tag, meta);
        return meta;
    }

    @Override
    public Object createTag(ItemStack stack, String tag) {
        if (platform.getItemUtils().isEmpty(stack)) return null;
        Object outputObject = getTag(stack, tag);
        if (outputObject == null || !(outputObject instanceof CompoundTag)) {
            CompoundTag tagObject = getTag(stack);
            if (tagObject == null) {
                tagObject = new CompoundTag();
                ((net.minecraft.world.item.ItemStack)craft).setTag(tagObject);
            }
            outputObject = new CompoundTag();
            tagObject.put(tag, (CompoundTag)outputObject);
        }
        return outputObject;
    }

    @Override
    public byte[] getByteArray(Object tag, String key) {
        if (tag == null || !(tag instanceof CompoundTag)) return null;
        return ((CompoundTag)tag).getByteArray(key);
    }

    @Override
    public int[] getIntArray(Object tag, String key) {
        if (tag == null || !(tag instanceof CompoundTag)) return null;
        return ((CompoundTag)tag).getIntArray(key);
    }

    @Override
    public String getString(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getString(tag);
    }

    @Override
    public String getString(ItemStack stack, String tag) {
        if (platform.getItemUtils().isEmpty(stack)) return null;
        String meta = null;
        Object tagObject = getTag(stack);
        if (tagObject == null || !(tagObject instanceof CompoundTag)) return null;
        meta = ((CompoundTag)tagObject).getString(tag);
        return meta;
    }

    @Override
    public Byte getOptionalByte(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getByte(tag);
    }

    @Override
    public Integer getOptionalInt(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getInt(tag);
    }

    @Override
    public Short getOptionalShort(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getShort(tag);
    }

    @Override
    public Double getOptionalDouble(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getDouble(tag);
    }

    @Override
    public Boolean getOptionalBoolean(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return null;
        return ((CompoundTag)node).getBoolean(tag);
    }

    @Override
    public void setLong(Object node, String tag, long value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putLong(tag, value);
    }

    @Override
    public void setBoolean(Object node, String tag, boolean value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putBoolean(tag, value);
    }

    @Override
    public void setDouble(Object node, String tag, double value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putDouble(tag, value);
    }

    @Override
    public void setInt(Object node, String tag, int value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putInt(tag, value);
    }

    @Override
    public void setMetaShort(Object node, String tag, short value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putShort(tag, value);
    }

    @Override
    public void removeMeta(Object node, String tag) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).remove(tag);
    }

    @Override
    public void setTag(Object node, String tag, Object child) {
        if (node == null || !(node instanceof CompoundTag)) return;
        if (child == null) {
            ((CompoundTag)node).remove(tag);
        } else if (child instanceof Tag) {
            ((CompoundTag)node).put(tag, (Tag)child);
        }
    }

    @Override
    public boolean setTag(ItemStack stack, String tag, Object child) {
        if (platform.getItemUtils().isEmpty(stack)) return false;
        Object node = getTag(stack);
        if (node == null || !(node instanceof CompoundTag)) return false;
        if (child == null) {
            ((CompoundTag)node).remove(tag);
        } else {
            ((CompoundTag)node).put(tag, (Tag)child);
        }
        return true;
    }

    @Override
    public void setString(Object node, String tag, String value) {
        if (node == null || !(node instanceof CompoundTag)) return;
        ((CompoundTag)node).putString(tag, value);
    }

    @Override
    public void setString(ItemStack stack, String tag, String value) {
        if (platform.getItemUtils().isEmpty(stack)) return;
        Object tagObject = getTag(stack);
        if (tagObject == null || !(tagObject instanceof CompoundTag)) return;
        ((CompoundTag)tagObject).putString(tag, value);
    }

    @Override
    public void setIntArray(Object tag, String key, int[] value) {
        if (tag == null || !(tag instanceof CompoundTag)) return;
        ((CompoundTag)tag).put(key, new IntArrayTag(value));
    }

    @Override
    public void setByteArray(Object tag, String key, byte[] value) {
        if (tag == null || !(tag instanceof CompoundTag)) return;
        ((CompoundTag)tag).put(key, new ByteArrayTag(value));
    }

    @Override
    public void setEmptyList(Object tag, String key) {
        if (tag == null || !(tag instanceof CompoundTag)) return;
        ((CompoundTag)tag).put(key, new ListTag());
    }

    @Override
    public void addToList(Object listObject, Object node) {
        if (listObject == null || !(listObject instanceof ListTag) || !(node instanceof Tag)) return;
        ListTag list = (ListTag)listObject;
        list.add((Tag)node);
    }

    @Override
    public Object readTagFromStream(InputStream input) {
        CompoundTag tag = null;
        try {
            tag = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        } catch (Exception ex) {
            platform.getLogger().log(Level.WARNING, "Error reading from NBT input stream", ex);
        }
        return tag;
    }

    @Override
    public boolean writeTagToStream(Object tag, OutputStream output) {
        if (tag == null || !(tag instanceof CompoundTag)) return false;
        try {
            NbtIo.writeCompressed((CompoundTag)tag, output);
        } catch (Exception ex) {
            platform.getLogger().log(Level.WARNING, "Error writing NBT output stream", ex);
            return false;
        }
        return true;
    }

    @Override
    public Collection<Object> getTagList(Object tag, String key) {
        Collection<Object> list = new ArrayList<>();
        if (tag == null || !(tag instanceof CompoundTag)) {
            return list;
        }

        ListTag listTag = ((CompoundTag)tag).getList(key, CompatibilityConstants.NBT_TYPE_COMPOUND);

        if (listTag != null) {
            Logger logger = platform.getLogger();
            int size = listTag.size();
            for (int i = 0; i < size; i++) {
                // Doesn't seem like this is ever going to get resolved, mappings issue:
                // https://hub.spigotmc.org/jira/browse/SPIGOT-6550
                // Tag entry = listTag.get(i);
                Tag entry = (Tag)ReflectionUtils.getListItem(logger, listTag, i);
                list.add(entry);
            }
        }
        return list;
    }

    @Override
    public Object newCompoundTag() {
        return new CompoundTag();
    }

    @Override
    public ItemStack getItem(Object itemTag) {
        if (itemTag == null || !(itemTag instanceof CompoundTag)) return null;
        ItemStack item = null;
        try {
            net.minecraft.world.item.ItemStack nmsStack = net.minecraft.world.item.ItemStack.of((CompoundTag)itemTag);
            item = CraftItemStack.asCraftMirror(nmsStack);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return item;
    }
    protected StringTag getTagString(String value) {
        return StringTag.valueOf(value);
    }

    @Override
    public Object setStringList(Object nbtBase, String tag, Collection<String> values) {
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return null;
        CompoundTag compoundTag = (CompoundTag)nbtBase;
        ListTag listTag = new ListTag();

        for (String value : values) {
            StringTag nbtString = getTagString(value);
            listTag.add(nbtString);
        }

        compoundTag.put(tag, listTag);
        return listTag;
    }

    @Override
    public List<String> getStringList(Object nbtBase, String key) {
        List<String> list = new ArrayList<>();
        if (nbtBase == null || !(nbtBase instanceof CompoundTag)) return list;
        CompoundTag compoundTag = (CompoundTag)nbtBase;
        ListTag listTag = compoundTag.getList(key, CompatibilityConstants.NBT_TYPE_STRING);

        if (listTag != null) {
            Logger logger = platform.getLogger();
            int size = listTag.size();
            for (int i = 0; i < size; i++) {
                // Doesn't seem like this is ever going to get resolved, mappings issue:
                // https://hub.spigotmc.org/jira/browse/SPIGOT-6550
                // Tag entry = listTag.get(i);
                Tag entry = (Tag)ReflectionUtils.getListItem(logger, listTag, i);
                list.add(entry.getAsString());
            }
        }
        return list;
    }

    @Override
    public Object getHandle(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || !(stack instanceof CraftItemStack)) {
            return null;
        }
        return ReflectionUtils.getHandle(platform.getLogger(), stack, CraftItemStack.class);
    }
}
