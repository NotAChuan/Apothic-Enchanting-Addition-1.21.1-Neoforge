package com.chuan.apothicflux.registry;

import com.chuan.apothicflux.block.*;
import com.chuan.apothicflux.block.entity.*;
import com.chuan.apothicflux.block.entity.FluxStatsBookshelfBlockEntity;
import com.chuan.apothicflux.item.CompressedSolidifiedFluxExperienceItem;
import com.chuan.apothicflux.item.FluxSpawnerBlockItem;
import com.chuan.apothicflux.item.SolidifiedFluxExperienceItem;
import com.chuan.apothicflux.menu.FluxEnchantingMenu;
import com.chuan.apothicflux.menu.FluxAnvilMenu;
import com.chuan.apothicflux.menu.FluxExpConverterMenu;
import com.chuan.apothicflux.menu.FluxSpawnerMenu;
import com.chuan.apothicflux.menu.FluxStatsBookshelfMenu;
import com.chuan.apothicflux.recipe.RitualCraftingRecipe;
import com.chuan.apothicflux.recipe.RitualDrawingRecipe;
import com.chuan.apothicflux.recipe.SpawnerRecipe;
import com.chuan.apothicflux.recipe.SpawnerRemoveRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {
    public static final String MOD_ID = "apothic_flux";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MOD_ID);

    public static final DeferredHolder<Block, FluxStatsBookshelfBlock> FLUX_STATS_BOOKSHELF_TIER_1 = registerBlock("flux_stats_bookshelf_tier_1", Tier.TIER_1);
    public static final DeferredHolder<Block, FluxStatsBookshelfBlock> FLUX_STATS_BOOKSHELF_TIER_2 = registerBlock("flux_stats_bookshelf_tier_2", Tier.TIER_2);
    public static final DeferredHolder<Block, FluxStatsBookshelfBlock> FLUX_STATS_BOOKSHELF_TIER_3 = registerBlock("flux_stats_bookshelf_tier_3", Tier.TIER_3);
    public static final DeferredHolder<Block, FluxStatsBookshelfBlock> FLUX_STATS_BOOKSHELF_TIER_4 = registerBlock("flux_stats_bookshelf_tier_4", Tier.TIER_4);

    public static final DeferredHolder<Block, RitualCoreBlock> RITUAL_CORE_BLOCK = BLOCKS.register("ritual_core",
            () -> new RitualCoreBlock(BlockBehaviour.Properties.of().noOcclusion().strength(0.2f)));

    public static final DeferredHolder<Block, FluxAnvilBlock> FLUX_ANVIL = BLOCKS.register("flux_anvil",
            () -> new FluxAnvilBlock(BlockBehaviour.Properties.of()
                    .strength(5.0f, 1200.0f)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()));

    public static final DeferredHolder<Block, FluxSpawnerBlock> FLUX_SPAWNER = BLOCKS.register("flux_spawner",
            () -> new FluxSpawnerBlock(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final DeferredHolder<Block, FluxExpConverterBlock> FLUX_EXP_CONVERTER = BLOCKS.register("exp_converter",
            () -> new FluxExpConverterBlock(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()));

    private static BaseFlowingFluid.Properties EXPERIENCE_FLUID_PROPERTIES;

    public static final DeferredHolder<FluidType, FluidType> EXPERIENCE_FLUID_TYPE = FLUID_TYPES.register("experience",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.apothic_flux.experience")
                    .canDrown(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, SoundEvents.EXPERIENCE_ORB_PICKUP)
                    .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, SoundEvents.EXPERIENCE_ORB_PICKUP)) {
            });

    public static final DeferredHolder<Block, LiquidBlock> EXPERIENCE_LIQUID_BLOCK = BLOCKS.register("experience",
            () -> new LiquidBlock(experienceFluid(),
                    BlockBehaviour.Properties.of()
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> EXPERIENCE_FLUID = FLUIDS.register("experience",
            () -> new BaseFlowingFluid.Source(EXPERIENCE_FLUID_PROPERTIES));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_EXPERIENCE_FLUID = FLUIDS.register("flowing_experience",
            () -> new BaseFlowingFluid.Flowing(EXPERIENCE_FLUID_PROPERTIES));

    public static final DeferredHolder<Item, BlockItem> FLUX_ANVIL_ITEM = ITEMS.register("flux_anvil",
            () -> new BlockItem(FLUX_ANVIL.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> FLUX_EXP_CONVERTER_ITEM = ITEMS.register("exp_converter",
            () -> new BlockItem(FLUX_EXP_CONVERTER.get(), new Item.Properties()));

    // 经验转化器顶部的球状物：只作为模型来源，不注册物品、不加入创造模式。
    public static final DeferredHolder<Block, Block> EXP_CONVERTER_TOP_BLOCK = BLOCKS.register("exp_converter_top",
            () -> new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak()));

    public static final DeferredHolder<Item, BucketItem> EXPERIENCE_BUCKET = ITEMS.register("experience_bucket",
            () -> new BucketItem(experienceFluid(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));

    static {
        EXPERIENCE_FLUID_PROPERTIES = new BaseFlowingFluid.Properties(
                EXPERIENCE_FLUID_TYPE::value,
                EXPERIENCE_FLUID::value,
                FLOWING_EXPERIENCE_FLUID::value
        ).bucket(() -> EXPERIENCE_BUCKET.get())
                .block(() -> EXPERIENCE_LIQUID_BLOCK.get())
                .slopeFindDistance(1)
                .levelDecreasePerBlock(1)
                .tickRate(10)
                .explosionResistance(100.0F);
    }

    public static final DeferredHolder<Item, Item> SOLIDIFIED_FLUX_EXPERIENCE = ITEMS.register("solidified_flux_experience",
            () -> new SolidifiedFluxExperienceItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> COMPRESSED_SOLIDIFIED_FLUX_EXPERIENCE = ITEMS.register("compressed_solidified_flux_experience",
            () -> new CompressedSolidifiedFluxExperienceItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, BlockItem> FLUX_SPAWNER_ITEM = ITEMS.register("flux_spawner",
            () -> new FluxSpawnerBlockItem(FLUX_SPAWNER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxStatsBookshelfBlockEntity>> STATS_BOOKSHELF_BE = BLOCK_ENTITIES.register("stats_bookshelf",
            () -> BlockEntityType.Builder.of(FluxStatsBookshelfBlockEntity::new,
                    FLUX_STATS_BOOKSHELF_TIER_1.get(),
                    FLUX_STATS_BOOKSHELF_TIER_2.get(),
                    FLUX_STATS_BOOKSHELF_TIER_3.get(),
                    FLUX_STATS_BOOKSHELF_TIER_4.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualBlockEntity>> RITUAL_BE = BLOCK_ENTITIES.register("ritual_core",
            () -> BlockEntityType.Builder.of(RitualBlockEntity::new, RITUAL_CORE_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxAnvilBlockEntity>> FLUX_ANVIL_BE = BLOCK_ENTITIES.register("flux_anvil",
            () -> BlockEntityType.Builder.of(FluxAnvilBlockEntity::new, FLUX_ANVIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxExpConverterBlockEntity>> FLUX_EXP_CONVERTER_BE = BLOCK_ENTITIES.register("exp_converter",
            () -> BlockEntityType.Builder.of(FluxExpConverterBlockEntity::new, FLUX_EXP_CONVERTER.get()).build(null));

    public static final DeferredHolder<Block, FluxEnchantingTableBlock> FLUX_ENCHANTING_TABLE = BLOCKS.register("flux_enchanting_table",
            () -> new FluxEnchantingTableBlock(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));

    public static final DeferredHolder<Item, BlockItem> FLUX_ENCHANTING_TABLE_ITEM = ITEMS.register("flux_enchanting_table",
            () -> new BlockItem(FLUX_ENCHANTING_TABLE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxEnchantingTableBlockEntity>> FLUX_ENCHANTING_TABLE_BE = BLOCK_ENTITIES.register("flux_enchanting_table",
            () -> BlockEntityType.Builder.of(FluxEnchantingTableBlockEntity::new, FLUX_ENCHANTING_TABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxSpawnerBlockEntity>> FLUX_SPAWNER_BE = BLOCK_ENTITIES.register("flux_spawner",
            () -> BlockEntityType.Builder.of(FluxSpawnerBlockEntity::new, FLUX_SPAWNER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<FluxEnchantingMenu>> FLUX_ENCHANTING_MENU = MENU_TYPES.register("flux_enchanting_menu",
            () -> IMenuTypeExtension.create(FluxEnchantingMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluxStatsBookshelfMenu>> STATS_BOOKSHELF_MENU = MENU_TYPES.register("stats_bookshelf_menu",
            () -> IMenuTypeExtension.create(FluxStatsBookshelfMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluxAnvilMenu>> FLUX_ANVIL_MENU = MENU_TYPES.register("flux_anvil_menu",
            () -> IMenuTypeExtension.create(FluxAnvilMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluxExpConverterMenu>> FLUX_EXP_CONVERTER_MENU = MENU_TYPES.register("exp_converter_menu",
            () -> IMenuTypeExtension.create(FluxExpConverterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluxSpawnerMenu>> FLUX_SPAWNER_MENU = MENU_TYPES.register("flux_spawner_menu",
            () -> IMenuTypeExtension.create(FluxSpawnerMenu::new));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualDrawingRecipe>> DRAWING_TYPE = RECIPE_TYPES.register("drawing", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return MOD_ID + ":drawing";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualDrawingRecipe>> DRAWING_SERIALIZER = RECIPE_SERIALIZERS.register("drawing",
            () -> serializer(RitualDrawingRecipe.CODEC, RitualDrawingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualCraftingRecipe>> RITUAL_TYPE = RECIPE_TYPES.register("ritual", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return MOD_ID + ":ritual";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualCraftingRecipe>> RITUAL_SERIALIZER = RECIPE_SERIALIZERS.register("ritual",
            () -> serializer(RitualCraftingRecipe.CODEC, RitualCraftingRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SpawnerRecipe>> SPAWNER_TYPE = RECIPE_TYPES.register("spawner", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return MOD_ID + ":spawner";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpawnerRecipe>> SPAWNER_SERIALIZER = RECIPE_SERIALIZERS.register("spawner",
            () -> serializer(SpawnerRecipe.CODEC, SpawnerRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SpawnerRemoveRecipe>> SPAWNER_REMOVE_TYPE = RECIPE_TYPES.register("spawner_remove", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return MOD_ID + ":spawner_remove";
        }
    });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SpawnerRemoveRecipe>> SPAWNER_REMOVE_SERIALIZER = RECIPE_SERIALIZERS.register("spawner_remove",
            () -> serializer(SpawnerRemoveRecipe.CODEC, SpawnerRemoveRecipe.STREAM_CODEC));

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }

    private static DeferredHolder<Block, FluxStatsBookshelfBlock> registerBlock(String name, Tier tier) {
        DeferredHolder<Block, FluxStatsBookshelfBlock> block = BLOCKS.register(name, () -> new FluxStatsBookshelfBlock(BlockBehaviour.Properties.of().strength(2.0f), tier));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENU_TYPES.register(eventBus);
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    private static BaseFlowingFluid.Source experienceFluid() {
        return EXPERIENCE_FLUID.get();
    }
}
