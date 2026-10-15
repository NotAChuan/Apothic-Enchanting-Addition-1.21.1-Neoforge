package com.chuan.apothicflux.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.crafting.RecipeManager;

/** Server-wide persistent store for infuser memories. Block items carry only the UUID. */
public final class FluxInfuserSavedData extends SavedData {
    private static final String FILE = "apothic_flux_infuser_records";
    private static final String KEY_MACHINES = "Machines";
    private static final String KEY_ID = "Id";
    private static final String KEY_RECORDS = "Records";
    private final Map<UUID, CompoundTag> machines = new LinkedHashMap<>();

    public static FluxInfuserSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FluxInfuserSavedData::new, FluxInfuserSavedData::load), FILE);
    }

    private static FluxInfuserSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FluxInfuserSavedData data = new FluxInfuserSavedData();
        ListTag list = tag.getList(KEY_MACHINES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try { data.machines.put(UUID.fromString(entry.getString(KEY_ID)), entry.copy()); }
            catch (IllegalArgumentException ignored) { }
        }
        return data;
    }

    public CompoundTag get(UUID id) { CompoundTag tag = machines.get(id); return tag == null ? null : tag.copy(); }
    public void put(UUID id, CompoundTag state) { machines.put(id, state.copy()); setDirty(); }
    public void remove(UUID id) { if (machines.remove(id) != null) setDirty(); }

    public void pruneMissingRecipes(RecipeManager manager) {
        boolean changed = false;
        for (CompoundTag state : machines.values()) {
            ListTag records = state.getList(KEY_RECORDS, Tag.TAG_COMPOUND);
            for (int i = records.size() - 1; i >= 0; i--) {
                CompoundTag record = records.getCompound(i);
                var id = net.minecraft.resources.ResourceLocation.tryParse(record.getString("Recipe"));
                if (id == null || manager.byKey(id).isEmpty()) {
                    records.remove(i);
                    changed = true;
                }
            }
        }
        if (changed) setDirty();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        machines.forEach((id, state) -> { CompoundTag e = state.copy(); e.putString(KEY_ID, id.toString()); list.add(e); });
        tag.put(KEY_MACHINES, list);
        return tag;
    }
}
