package com.chuan.apothicenchantingaddition.registry;

import com.chuan.apothicenchantingaddition.block.RitualCoreBlock;
import com.chuan.apothicenchantingaddition.block.StatsBookshelfBlock;
import com.chuan.apothicenchantingaddition.block.Tier;
import com.chuan.apothicenchantingaddition.block.entity.RitualBlockEntity;
import com.chuan.apothicenchantingaddition.block.entity.StatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.menu.StatsBookshelfMenu;
import com.chuan.apothicenchantingaddition.recipe.RitualCraftingRecipe;
import com.chuan.apothicenchantingaddition.recipe.RitualDrawingRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.chuan.apothicenchantingaddition.block.FluxEnchantingTableBlock;
import com.chuan.apothicenchantingaddition.block.entity.FluxEnchantingTableBlockEntity;
import com.chuan.apothicenchantingaddition.menu.FluxEnchantingMenu;

public class ModRegistry {
    public static final String MOD_ID = "apothicenchantingaddition";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    // 配方相关注册
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MOD_ID);

    // ================== 方块 ==================
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_1 = registerBlock("stats_bookshelf_tier_1", Tier.TIER_1);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_2 = registerBlock("stats_bookshelf_tier_2", Tier.TIER_2);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_3 = registerBlock("stats_bookshelf_tier_3", Tier.TIER_3);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_4 = registerBlock("stats_bookshelf_tier_4", Tier.TIER_4);

    public static final DeferredHolder<Block, RitualCoreBlock> RITUAL_CORE_BLOCK = BLOCKS.register("ritual_core",
            () -> new RitualCoreBlock(BlockBehaviour.Properties.of().noOcclusion().strength(0.2f)));

    // ================== 方块实体 ==================
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatsBookshelfBlockEntity>> STATS_BOOKSHELF_BE = BLOCK_ENTITIES.register("stats_bookshelf",
            () -> BlockEntityType.Builder.of(StatsBookshelfBlockEntity::new,
                    STATS_BOOKSHELF_TIER_1.get(),
                    STATS_BOOKSHELF_TIER_2.get(),
                    STATS_BOOKSHELF_TIER_3.get(),
                    STATS_BOOKSHELF_TIER_4.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualBlockEntity>> RITUAL_BE = BLOCK_ENTITIES.register("ritual_core",
            () -> BlockEntityType.Builder.of(RitualBlockEntity::new, RITUAL_CORE_BLOCK.get()).build(null));

    // 1. 注册通量附魔台方块与物品
    public static final DeferredHolder<Block, FluxEnchantingTableBlock> FLUX_ENCHANTING_TABLE = BLOCKS.register("flux_enchanting_table",
            () -> new FluxEnchantingTableBlock(BlockBehaviour.Properties.of().strength(5.0f).requiresCorrectToolForDrops()));

    // 注意：注册同名物品
    public static final DeferredHolder<Item, BlockItem> FLUX_ENCHANTING_TABLE_ITEM = ITEMS.register("flux_enchanting_table",
            () -> new BlockItem(FLUX_ENCHANTING_TABLE.get(), new Item.Properties()));

    // 2. 注册方块实体
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxEnchantingTableBlockEntity>> FLUX_ENCHANTING_TABLE_BE = BLOCK_ENTITIES.register("flux_enchanting_table",
            () -> BlockEntityType.Builder.of(FluxEnchantingTableBlockEntity::new, FLUX_ENCHANTING_TABLE.get()).build(null));

    // 3. 注册菜单类型 (Container)
    public static final DeferredHolder<MenuType<?>, MenuType<FluxEnchantingMenu>> FLUX_ENCHANTING_MENU = MENU_TYPES.register("flux_enchanting_menu",
            () -> IMenuTypeExtension.create(FluxEnchantingMenu::new));

    // ================== 菜单 ==================
    public static final DeferredHolder<MenuType<?>, MenuType<StatsBookshelfMenu>> STATS_BOOKSHELF_MENU = MENU_TYPES.register("stats_bookshelf_menu",
            () -> IMenuTypeExtension.create(StatsBookshelfMenu::new));

    // ================== 配方类型与序列化器 ==================

    // 1. 仪式绘制配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualDrawingRecipe>> DRAWING_TYPE = RECIPE_TYPES.register("drawing", () -> new RecipeType<>() {
        @Override
        public String toString() { return MOD_ID + ":drawing"; }
    });

    // 仪式绘制配方序列化器 (手动实现 RecipeSerializer)
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualDrawingRecipe>> DRAWING_SERIALIZER = RECIPE_SERIALIZERS.register("drawing",
            () -> new RecipeSerializer<RitualDrawingRecipe>() {
                @Override
                public MapCodec<RitualDrawingRecipe> codec() {
                    return RitualDrawingRecipe.CODEC;
                }
                @Override
                public StreamCodec<RegistryFriendlyByteBuf, RitualDrawingRecipe> streamCodec() {
                    return RitualDrawingRecipe.STREAM_CODEC;
                }
            });

    // 2. 聚灵汇法配方类型
    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualCraftingRecipe>> RITUAL_TYPE = RECIPE_TYPES.register("ritual", () -> new RecipeType<>() {
        @Override
        public String toString() { return MOD_ID + ":ritual"; }
    });

    // 聚灵汇法配方序列化器 (手动实现 RecipeSerializer)
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualCraftingRecipe>> RITUAL_SERIALIZER = RECIPE_SERIALIZERS.register("ritual",
            () -> new RecipeSerializer<RitualCraftingRecipe>() {
                @Override
                public MapCodec<RitualCraftingRecipe> codec() {
                    return RitualCraftingRecipe.CODEC;
                }
                @Override
                public StreamCodec<RegistryFriendlyByteBuf, RitualCraftingRecipe> streamCodec() {
                    // 这里现在能够正确匹配 RitualCraftingRecipe 类型
                    return RitualCraftingRecipe.STREAM_CODEC;
                }
            });


    private static DeferredHolder<Block, StatsBookshelfBlock> registerBlock(String name, Tier tier) {
        DeferredHolder<Block, StatsBookshelfBlock> block = BLOCKS.register(name, () -> new StatsBookshelfBlock(BlockBehaviour.Properties.of().strength(2.0f), tier));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENU_TYPES.register(eventBus);
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);

        eventBus.addListener(ModRegistry::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(STATS_BOOKSHELF_TIER_1.get());
            event.accept(STATS_BOOKSHELF_TIER_2.get());
            event.accept(STATS_BOOKSHELF_TIER_3.get());
            event.accept(STATS_BOOKSHELF_TIER_4.get());
            event.accept(FLUX_ENCHANTING_TABLE_ITEM.get());
        }
    }
}
