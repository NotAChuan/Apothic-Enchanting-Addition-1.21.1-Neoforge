package com.chuan.apothicenchantingaddition.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 缓存每种实体掉落的“概率模型上下文”：
 * 1) LootTable 解析结果
 * 2) 可复用的 dummy entity
 * 3) 每次 roll 产出 stack 数的滑动估计，用来给分帧队列动态分配预算
 */
public final class FluxSpawnerLootProbabilityModelCache {

    private static final Map<ServerLevel, Map<ModelKey, LootProbabilityModel>> CACHE = new WeakHashMap<>();

    private FluxSpawnerLootProbabilityModelCache() {
    }

    public static LootProbabilityModel get(ServerLevel level, EntityType<?> entityType) {
        ResourceKey<LootTable> lootTableKey = entityType.getDefaultLootTable();
        Map<ModelKey, LootProbabilityModel> levelCache = CACHE.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        return levelCache.computeIfAbsent(new ModelKey(entityType, lootTableKey), ignored -> new LootProbabilityModel(entityType, lootTableKey));
    }

    public static void clear(ServerLevel level) {
        Map<ModelKey, LootProbabilityModel> removed = CACHE.remove(level);
        if (removed == null) {
            return;
        }

        for (LootProbabilityModel model : removed.values()) {
            model.dispose();
        }
    }

    public static void clearAll() {
        for (Map<ModelKey, LootProbabilityModel> models : CACHE.values()) {
            for (LootProbabilityModel model : models.values()) {
                model.dispose();
            }
        }
        CACHE.clear();
    }

    private record ModelKey(EntityType<?> entityType, ResourceKey<LootTable> lootTableKey) {
    }

    public static final class LootProbabilityModel {
        private static final int DEFAULT_ESTIMATED_STACKS_PER_ROLL = 1;

        private final EntityType<?> entityType;
        private final ResourceKey<LootTable> lootTableKey;
        private @Nullable LootTable cachedLootTable;
        private @Nullable Entity cachedContextEntity;
        private int estimatedStacksPerRoll = DEFAULT_ESTIMATED_STACKS_PER_ROLL;

        private LootProbabilityModel(EntityType<?> entityType, ResourceKey<LootTable> lootTableKey) {
            this.entityType = entityType;
            this.lootTableKey = lootTableKey;
        }

        public List<ItemStack> sample(ServerLevel level, BlockPos origin, DamageSource damageSource, FakePlayer fakePlayer) {
            LootTable lootTable = resolveLootTable(level);
            Entity contextEntity = getOrCreateContextEntity(level);
            if (contextEntity == null) {
                return List.of();
            }

            contextEntity.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);

            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, contextEntity)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                    .create(LootContextParamSets.ENTITY);

            List<ItemStack> result = lootTable.getRandomItems(params);
            updateRollingStackEstimate(result.size());
            return result;
        }

        public int recommendedRollBudget(boolean hasTime) {
            int targetStacks = hasTime ? 24 : 10;
            return Math.max(1, targetStacks / Math.max(1, this.estimatedStacksPerRoll));
        }

        private LootTable resolveLootTable(ServerLevel level) {
            if (this.cachedLootTable == null) {
                this.cachedLootTable = level.getServer().reloadableRegistries().getLootTable(this.lootTableKey);
            }
            return this.cachedLootTable;
        }

        private @Nullable Entity getOrCreateContextEntity(ServerLevel level) {
            if (this.cachedContextEntity != null && !this.cachedContextEntity.isRemoved() && this.cachedContextEntity.level() == level) {
                return this.cachedContextEntity;
            }

            this.cachedContextEntity = this.entityType.create(level);
            return this.cachedContextEntity;
        }

        private void updateRollingStackEstimate(int generatedStacks) {
            int clamped = Math.max(1, generatedStacks);
            this.estimatedStacksPerRoll = Math.max(1, (this.estimatedStacksPerRoll * 3 + clamped) / 4);
        }

        private void dispose() {
            if (this.cachedContextEntity != null) {
                this.cachedContextEntity.discard();
                this.cachedContextEntity = null;
            }
            this.cachedLootTable = null;
        }
    }
}
