package com.chuan.apothicenchantingaddition.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ApothicAdditionConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TIER_1_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_2_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_3_ENERGY_COST;
    public static final ModConfigSpec.IntValue TIER_4_ENERGY_COST;

//    public static final ModConfigSpec.IntValue MAX_ENERGY_CAPACITY;

    static {
        BUILDER.push("Energy Settings");

        TIER_1_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 1 Bookshelf")
                .defineInRange("tier1EnergyCost", 20, 0, Integer.MAX_VALUE);
        TIER_2_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 2 Bookshelf")
                .defineInRange("tier2EnergyCost", 40, 0, Integer.MAX_VALUE);
        TIER_3_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 3 Bookshelf")
                .defineInRange("tier3EnergyCost", 60, 0, Integer.MAX_VALUE);
        TIER_4_ENERGY_COST = BUILDER.comment("Energy cost per tick for Tier 4 Bookshelf")
                .defineInRange("tier4EnergyCost", 80, 0, Integer.MAX_VALUE);

//        MAX_ENERGY_CAPACITY = BUILDER.comment("Maximum FE capacity for the bookshelves")
//                .defineInRange("maxEnergyCapacity", 100000, 1000, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
