package com.chuan.apothicflux.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动通量灌注器的「配方记录表」。
 * <p>
 * 按「输入物（物品 + 数据组件）」记录玩家为该输入物选中的灌注配方，
 * 因此一台机器可以同时记住多条不同输入物的配方（石头→沙子、圆石→煤炭 …）。
 * 上限 {@link #MAX_RECORDS} 条；已满时为新的输入物建立记录会被拒绝，
 * 并由 FluxInfuserMenu 给点击的玩家发提示。
 */
public final class FluxInfuserRecords {

    /** 记录表上限。超出后不再为新输入物建立记录。 */
    public static final int MAX_RECORDS = 64;

    private static final String KEY_LIST = "Records";
    private static final String KEY_ITEM = "Item";
    private static final String KEY_RECIPE = "Recipe";
    private static final String KEY_PRODUCED = "Produced";

    private FluxInfuserRecords() {
    }

    /**
     * 单条记录：某个输入物当前选中的灌注配方。
     * <p>
     * {@code key} 数量恒为 1，只用于「物品 + 数据组件」匹配。
     * {@code produced} 表示「本次选择之后是否已经产出过」，用于 GUI 发光边框：
     * 改选不同配方后置 false（发光），完成一次灌注后置 true（取消发光，且此后保持取消）。
     */
    public static final class Record {

        private final ItemStack key;
        private ResourceLocation recipeId;
        private boolean produced;

        public Record(ItemStack key, ResourceLocation recipeId, boolean produced) {
            this.key = key.copyWithCount(1);
            this.recipeId = recipeId;
            this.produced = produced;
        }

        public ItemStack key() {
            return key;
        }

        public ResourceLocation recipeId() {
            return recipeId;
        }

        /** true = 本次选择后尚未产出过，GUI 需要画发光边框。 */
        public boolean glow() {
            return !produced;
        }

        public boolean matches(ItemStack stack) {
            return ItemStack.isSameItemSameComponents(this.key, stack);
        }

        /**
         * 选择配方。
         *
         * @return true 表示状态发生变化（需要同步 + 重绘）。
         *         重选「同一个」配方不改动任何状态，因此不会重新发光。
         */
        public boolean select(ResourceLocation id) {
            if (id.equals(this.recipeId)) {
                return false;
            }
            this.recipeId = id;
            this.produced = false;
            return true;
        }

        /** 完成一次灌注：取消发光，且此后一直保持取消。 */
        public void markProduced() {
            this.produced = true;
        }
    }

    /** 找到与给定输入物匹配的记录；没有则返回 null。 */
    @Nullable
    public static Record find(List<Record> records, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Record record : records) {
            if (record.matches(stack)) {
                return record;
            }
        }
        return null;
    }

    public static ListTag save(List<Record> records, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Record record : records) {
            CompoundTag tag = new CompoundTag();
            tag.put(KEY_ITEM, stackToTag(registries, record.key()));
            tag.putString(KEY_RECIPE, record.recipeId().toString());
            tag.putBoolean(KEY_PRODUCED, !record.glow());
            list.add(tag);
        }
        return list;
    }

    public static List<Record> load(@Nullable ListTag list, HolderLookup.Provider registries) {
        List<Record> records = new ArrayList<>();
        if (list == null) {
            return records;
        }
        for (int i = 0; i < list.size() && records.size() < MAX_RECORDS; i++) {
            if (!(list.get(i) instanceof CompoundTag tag)) {
                continue;
            }
            if (!tag.contains(KEY_ITEM, Tag.TAG_COMPOUND) || !tag.contains(KEY_RECIPE, Tag.TAG_STRING)) {
                continue;
            }
            ItemStack key = tagToStack(registries, tag.getCompound(KEY_ITEM));
            if (key.isEmpty()) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_RECIPE));
            if (id == null) {
                continue;
            }
            records.add(new Record(key, id, tag.getBoolean(KEY_PRODUCED)));
        }
        return records;
    }

    public static String listKey() {
        return KEY_LIST;
    }

    /**
     * ItemStack → NBT 往返。这里刻意借用单槽 {@link ItemStackHandler}：
     * 该 API 在本项目其它机器 NBT 读写中已稳定使用，
     * 可避免 ItemStack.save / ItemStack.CODEC 在不同版本上的差异。
     */
    private static CompoundTag stackToTag(HolderLookup.Provider registries, ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack.copyWithCount(1));
        return handler.serializeNBT(registries);
    }

    private static ItemStack tagToStack(HolderLookup.Provider registries, CompoundTag tag) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.deserializeNBT(registries, tag);
        return handler.getStackInSlot(0);
    }
}
