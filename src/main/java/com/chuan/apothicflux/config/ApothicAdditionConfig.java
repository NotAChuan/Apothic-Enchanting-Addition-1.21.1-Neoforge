package com.chuan.apothicflux.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ApothicAdditionConfig {
    private static final String CONFIG_PREFIX = "config.apothic_flux.";
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TIER_1_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_2_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_3_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_4_ENERGY_COST;

    public static final ModConfigSpec.IntValue FLUX_ENCHANTER_TICK_COST;
    public static final ModConfigSpec.IntValue FLUX_ENCHANTER_BASE_COST;
    public static final ModConfigSpec.ConfigValue<String> FLUX_ENCHANTER_REFRESH_ITEM;
    public static final ModConfigSpec.IntValue FLUX_ENCHANTER_REFRESH_COUNT;

    public static final ModConfigSpec.IntValue FLUX_ANVIL_BASE_COST;

    public static final ModConfigSpec.IntValue FLUX_SPAWNER_ENERGY_PER_EGG;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_EXP_BASE_COUNT;
    public static final ModConfigSpec.BooleanValue FLUX_SPAWNER_ENTITY_BLACKLIST_OPEN;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_MIN_DELAY_LIMIT;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_MAX_DELAY_LIMIT;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_SPAWN_COUNT_LIMIT;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_ECHOING_LIMIT;

    public static final ModConfigSpec.IntValue FLUX_INFUSER_CAPACITY;
    public static final ModConfigSpec.IntValue FLUX_INFUSER_ENERGY_PER_TICK;
    public static final ModConfigSpec.BooleanValue FLUX_INFUSER_ENERGY_ENABLED;
    public static final ModConfigSpec.IntValue FLUX_INFUSER_CRAFT_TIME;
    public static final ModConfigSpec.BooleanValue FLUX_INFUSER_AUTO_OUTPUT;

    static {
        BUILDER.comment("Flux Bookshelf settings / 通量书架设置")
                .translation(CONFIG_PREFIX + "category.bookshelf")
                .push("Flux Bookshelf");

        TIER_1_ENERGY_COST = BUILDER.comment(
                        "Energy cost per tick for Tier 1 Bookshelf.",
                        "一阶通量书架每 tick 的耗电量。")
                .translation(CONFIG_PREFIX + "bookshelf.tier_1_energy_cost")
                .defineInRange("tier1EnergyCost", 200, 0, Integer.MAX_VALUE);
        TIER_2_ENERGY_COST = BUILDER.comment(
                        "Energy cost per tick for Tier 2 Bookshelf.",
                        "二阶通量书架每 tick 的耗电量。")
                .translation(CONFIG_PREFIX + "bookshelf.tier_2_energy_cost")
                .defineInRange("tier2EnergyCost", 400, 0, Integer.MAX_VALUE);
        TIER_3_ENERGY_COST = BUILDER.comment(
                        "Energy cost per tick for Tier 3 Bookshelf.",
                        "三阶通量书架每 tick 的耗电量。")
                .translation(CONFIG_PREFIX + "bookshelf.tier_3_energy_cost")
                .defineInRange("tier3EnergyCost", 600, 0, Integer.MAX_VALUE);
        TIER_4_ENERGY_COST = BUILDER.comment(
                        "Energy cost per tick for Tier 4 Bookshelf.",
                        "四阶通量书架每 tick 的耗电量。")
                .translation(CONFIG_PREFIX + "bookshelf.tier_4_energy_cost")
                .defineInRange("tier4EnergyCost", 800, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        BUILDER.comment("Flux Enchanting Table settings / 通量附魔台设置")
                .translation(CONFIG_PREFIX + "category.flux_enchanting")
                .push("Flux Enchanting Table");

        FLUX_ENCHANTER_TICK_COST = BUILDER.comment(
                        "Flux Enchanting Table idle energy cost per tick.",
                        "通量附魔台每 tick (1/20 秒) 的待机耗电。")
                .translation(CONFIG_PREFIX + "flux_enchanting.tick_cost")
                .defineInRange("fluxEnchanterTickCost", 2000, 0, Integer.MAX_VALUE);

        FLUX_ENCHANTER_BASE_COST = BUILDER.comment(
                        "Base energy cost per enchantment level.",
                        "Example: If set to 1000, enchanting a level 50 enchant will cost 50,000 FE.",
                        "通量附魔台附魔时的基础耗电量（实际耗电 = 基础耗电 × 附魔等级）。")
                .translation(CONFIG_PREFIX + "flux_enchanting.base_cost")
                .defineInRange("fluxEnchanterBaseCost", 2000, 0, Integer.MAX_VALUE);

        FLUX_ENCHANTER_REFRESH_ITEM = BUILDER.comment(
                        "Item required to refresh enchantments (registry name).",
                        "刷新附魔时消耗的物品 ID。默认：apothic_flux:compressed_solidified_flux_experience (压缩固化通量经验)。")
                .translation(CONFIG_PREFIX + "flux_enchanting.refresh_item")
                .define("fluxEnchanterRefreshItem", "apothic_flux:compressed_solidified_flux_experience");

        FLUX_ENCHANTER_REFRESH_COUNT = BUILDER.comment(
                        "Amount of the refresh item required.",
                        "每次点击刷新附魔时需要消耗的上述物品数量。")
                .translation(CONFIG_PREFIX + "flux_enchanting.refresh_count")
                .defineInRange("fluxEnchanterRefreshCount", 8, 1, 64);

        BUILDER.pop();

        BUILDER.comment("Flux Anvil settings / 通量铁砧设置")
                .translation(CONFIG_PREFIX + "category.anvil")
                .push("Flux Anvil");

        FLUX_ANVIL_BASE_COST = BUILDER
                .comment("Base FE cost for the Flux Anvil per level.",
                        "Total Cost = Operation Level × Base Cost.",
                        "通量铁砧每级操作的基础 FE 耗电量（总耗电 = 操作等级 × 基础耗电）。")
                .translation(CONFIG_PREFIX + "anvil.base_cost")
                .defineInRange("fluxAnvilBaseCost", 4000, 1, Integer.MAX_VALUE);

        BUILDER.pop();

        BUILDER.comment("Flux Spawner settings / 通量刷怪笼设置")
                .translation(CONFIG_PREFIX + "category.spawner")
                .push("Flux Spawner");

        FLUX_SPAWNER_ENERGY_PER_EGG = BUILDER
                .comment("Base energy parameter for the Flux Spawner.",
                        "通量刷怪笼的基础耗电参数。",
                        "每 tick 耗电公式: 基础 × 刷怪蛋数量 + 基础 × (800 / 最大延迟) + 基础 × 刷怪数量 + 基础 × 回响等级 × 2。")
                .translation(CONFIG_PREFIX + "spawner.energy_per_egg")
                .defineInRange("fluxSpawnerEnergyPerEgg", 2000, 0, Integer.MAX_VALUE);

        FLUX_SPAWNER_EXP_BASE_COUNT = BUILDER
                .comment("Base count for Solidified Flux Experience drop.",
                        "固化通量经验掉落基础数。",
                        "掉落公式: Base × Eggs × (1 + Echoing) × Spawn Count。")
                .translation(CONFIG_PREFIX + "spawner.exp_base_count")
                .defineInRange("fluxSpawnerExpBaseCount", 4, 0, Integer.MAX_VALUE);

        FLUX_SPAWNER_ENTITY_BLACKLIST_OPEN = BUILDER
                .comment("Whether the Flux Spawner can use spawn eggs that are blacklisted from normal spawners.",
                        "Default: true. When enabled, the Flux Spawner can still process those blacklisted spawn eggs.",
                        "是否允许通量刷怪笼使用通常不可用于刷怪笼的刷怪蛋。",
                        "默认值：true。开启后，通量刷怪笼仍然可以处理这些被加入黑名单的刷怪蛋。")
                .translation(CONFIG_PREFIX + "spawner.entity_blacklist_open")
                .define("fluxSpawnerEntityBlacklistOpen", true);

        FLUX_SPAWNER_MIN_DELAY_LIMIT = BUILDER
                .comment("Lower bound used by Flux Spawner min delay upgrades and downgrades.",
                        "Only affects this mod's own modifier behavior.",
                        "通量刷怪笼 MIN_DELAY 升降级可到达的下限，仅影响本模组自己的升级逻辑。")
                .translation(CONFIG_PREFIX + "spawner.min_delay_limit")
                .defineInRange("fluxSpawnerMinDelayLimit", 20, 1, 200);

        FLUX_SPAWNER_MAX_DELAY_LIMIT = BUILDER
                .comment("Lower bound used by Flux Spawner max delay upgrades and downgrades.",
                        "Only affects this mod's own modifier behavior.",
                        "通量刷怪笼 MAX_DELAY 升降级可到达的下限，仅影响本模组自己的升级逻辑。")
                .translation(CONFIG_PREFIX + "spawner.max_delay_limit")
                .defineInRange("fluxSpawnerMaxDelayLimit", 20, 1, 800);

        FLUX_SPAWNER_SPAWN_COUNT_LIMIT = BUILDER
                .comment("Upper bound used by Flux Spawner spawn count upgrades.",
                        "Only affects this mod's own modifier behavior.",
                        "通量刷怪笼 SPAWN_COUNT 升级可到达的上限，仅影响本模组自己的升级逻辑。")
                .translation(CONFIG_PREFIX + "spawner.spawn_count_limit")
                .defineInRange("fluxSpawnerSpawnCountLimit", 16, 1, 64);

        FLUX_SPAWNER_ECHOING_LIMIT = BUILDER
                .comment("Upper bound used by Flux Spawner echoing upgrades.",
                        "Only affects this mod's own modifier behavior.",
                        "通量刷怪笼 ECHOING 升级可到达的上限，仅影响本模组自己的升级逻辑。")
                .translation(CONFIG_PREFIX + "spawner.echoing_limit")
                .defineInRange("fluxSpawnerEchoingLimit", 5, 0, 16);

        BUILDER.pop();

        BUILDER.comment("Automatic Flux Infuser settings / 自动通量灌注器设置")
                .translation(CONFIG_PREFIX + "category.flux_infuser")
                .push("Flux Infuser");

        FLUX_INFUSER_CAPACITY = BUILDER
                .comment("Internal energy buffer of the Automatic Flux Infuser.",
                        "自动通量灌注器的内部储电上限。")
                .translation(CONFIG_PREFIX + "flux_infuser.capacity")
                .defineInRange("fluxInfuserCapacity", 10000000, 0, Integer.MAX_VALUE);

        FLUX_INFUSER_ENERGY_PER_TICK = BUILDER
                .comment("Energy consumed per tick while the Automatic Flux Infuser is running.",
                        "自动通量灌注器运行中每 tick 消耗的电量。",
                        "注意：待机（无输入 / 未选择配方 / 输出已满 / 属性不足）不耗电。")
                .translation(CONFIG_PREFIX + "flux_infuser.energy_per_tick")
                .defineInRange("fluxInfuserEnergyPerTick", 1000, 0, Integer.MAX_VALUE);

        FLUX_INFUSER_ENERGY_ENABLED = BUILDER
                .comment("Whether the Automatic Flux Infuser consumes energy at all.",
                        "Default: true. When disabled the machine runs without consuming any energy.",
                        "自动通量灌注器是否耗电。",
                        "默认：true（耗电）。设为 false 后机器完全不耗电。")
                .translation(CONFIG_PREFIX + "flux_infuser.energy_enabled")
                .define("fluxInfuserEnergyEnabled", true);

        FLUX_INFUSER_CRAFT_TIME = BUILDER
                .comment("Ticks required for a single infusion.",
                        "Default: 60 (3 seconds). Minimum: 1 tick.",
                        "单次灌注所需的时间（tick）。",
                        "默认：60（3 秒）。最小：1 tick。")
                .translation(CONFIG_PREFIX + "flux_infuser.craft_time")
                .defineInRange("fluxInfuserCraftTime", 60, 1, 72000);

        FLUX_INFUSER_AUTO_OUTPUT = BUILDER
                .comment("Whether the infuser automatically pushes its output into the container below.",
                        "自动通量灌注器是否自动把产物输出到下方容器。")
                .translation(CONFIG_PREFIX + "flux_infuser.auto_output")
                .define("fluxInfuserAutoOutput", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
