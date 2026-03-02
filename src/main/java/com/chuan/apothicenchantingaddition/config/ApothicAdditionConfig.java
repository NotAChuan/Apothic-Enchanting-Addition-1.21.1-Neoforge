package com.chuan.apothicenchantingaddition.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ApothicAdditionConfig {
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

    // 新增：通量刷怪笼配置
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_ENERGY_PER_EGG;
    public static final ModConfigSpec.IntValue FLUX_SPAWNER_EXP_BASE_COUNT;

    static {
        BUILDER.push("Energy Settings");

        TIER_1_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 1 Bookshelf")
                .defineInRange("tier1EnergyCost", 200, 0, Integer.MAX_VALUE);
        TIER_2_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 2 Bookshelf")
                .defineInRange("tier2EnergyCost", 400, 0, Integer.MAX_VALUE);
        TIER_3_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 3 Bookshelf")
                .defineInRange("tier3EnergyCost", 600, 0, Integer.MAX_VALUE);
        TIER_4_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 4 Bookshelf")
                .defineInRange("tier4EnergyCost", 800, 0, Integer.MAX_VALUE);

        BUILDER.push("Flux Enchanting Table Settings");

        FLUX_ENCHANTER_TICK_COST = BUILDER.comment(
                        "Energy cost per tick for the Flux Enchanting Table to stay active.",
                        "通量附魔台每 tick (1/20秒) 消耗的待机能量。")
                .defineInRange("fluxEnchanterTickCost", 2000, 0, Integer.MAX_VALUE);

        FLUX_ENCHANTER_BASE_COST = BUILDER.comment(
                        "Base energy cost per enchantment level.",
                        "Example: If set to 1000, enchanting a level 50 enchant will cost 50,000 FE.",
                        "通量附魔台附魔时的基础耗电量（实际耗电 = 该值 * 附魔等级）。")
                .defineInRange("fluxEnchanterBaseCost", 2000, 0, Integer.MAX_VALUE);

        FLUX_ENCHANTER_REFRESH_ITEM = BUILDER.comment(
                        "The item required to refresh the enchantments (Registry Name).",
                        "刷新附魔时消耗的物品 ID。默认：apothicenchantingaddition:compressed_solidified_flux_experience (压缩固化通量经验)。")
                .define("fluxEnchanterRefreshItem", "apothicenchantingaddition:compressed_solidified_flux_experience");

        FLUX_ENCHANTER_REFRESH_COUNT = BUILDER.comment(
                        "The amount of the refresh item required.",
                        "每次点击刷新附魔时，需要消耗上述物品的数量。")
                .defineInRange("fluxEnchanterRefreshCount", 8, 1, 64);

        BUILDER.push("Flux Anvil");

        FLUX_ANVIL_BASE_COST = BUILDER
                .comment("The base FE cost for the Flux Anvil per level.",
                        "Total Cost = Operation Level * Base Cost.",
                        "通量铁砧每级操作的基础 FE 耗电量（总耗电 = 操作等级 * 基础耗电）")
                .defineInRange("fluxAnvilBaseCost", 4000, 1, Integer.MAX_VALUE);

        BUILDER.pop(); // Pop Flux Anvil

        // 通量刷怪笼设置区域
        BUILDER.push("Flux Spawner");

        FLUX_SPAWNER_ENERGY_PER_EGG = BUILDER
                .comment("Base energy parameter for the Flux Spawner.",
                        "通量刷怪笼的基础耗电参数。",
                        "每 tick 耗电公式: 基础*刷怪蛋数量 + 基础*(800/最大延迟) + 基础*刷怪数量 + 基础*回响等级*2")
                .defineInRange("fluxSpawnerEnergyPerEgg", 2000, 0, Integer.MAX_VALUE);

        FLUX_SPAWNER_EXP_BASE_COUNT = BUILDER
                .comment("Base count for Solidified Flux Experience drop.",
                        "固化通量经验掉落基础数。",
                        "Formula / 掉落公式: Base * Eggs * (1 + Echoing)")
                .defineInRange("fluxSpawnerExpBaseCount", 4, 0, Integer.MAX_VALUE);


        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
