package com.chuan.apothicenchantingaddition.registry;

import com.chuan.apothicenchantingaddition.block.StatsBookshelfBlock;
import com.chuan.apothicenchantingaddition.block.Tier;
import com.chuan.apothicenchantingaddition.block.entity.StatsBookshelfBlockEntity;
import com.chuan.apothicenchantingaddition.menu.StatsBookshelfMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRegistry {
    public static final String MOD_ID = "apothicenchantingaddition"; // 确保和你的 mods.toml 一致

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);

    // 注册 4 个层级的方块
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_1 = registerBlock("stats_bookshelf_tier_1", Tier.TIER_1);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_2 = registerBlock("stats_bookshelf_tier_2", Tier.TIER_2);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_3 = registerBlock("stats_bookshelf_tier_3", Tier.TIER_3);
    public static final DeferredHolder<Block, StatsBookshelfBlock> STATS_BOOKSHELF_TIER_4 = registerBlock("stats_bookshelf_tier_4", Tier.TIER_4);

    // 注册方块实体 (所有层级共用一个 BE 类型)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatsBookshelfBlockEntity>> STATS_BOOKSHELF_BE = BLOCK_ENTITIES.register("stats_bookshelf",
            () -> BlockEntityType.Builder.of(StatsBookshelfBlockEntity::new,
                    STATS_BOOKSHELF_TIER_1.get(),
                    STATS_BOOKSHELF_TIER_2.get(),
                    STATS_BOOKSHELF_TIER_3.get(),
                    STATS_BOOKSHELF_TIER_4.get()
            ).build(null));

    // 注册菜单类型
    public static final DeferredHolder<MenuType<?>, MenuType<StatsBookshelfMenu>> STATS_BOOKSHELF_MENU = MENU_TYPES.register("stats_bookshelf_menu",
            () -> IMenuTypeExtension.create(StatsBookshelfMenu::new));


    // 辅助方法：同时注册方块和物品
    private static DeferredHolder<Block, StatsBookshelfBlock> registerBlock(String name, Tier tier) {
        DeferredHolder<Block, StatsBookshelfBlock> block = BLOCKS.register(name, () -> new StatsBookshelfBlock(BlockBehaviour.Properties.of().strength(2.0f), tier));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    // 初始化注册
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        MENU_TYPES.register(eventBus);

        // 注册到创造模式物品栏
        eventBus.addListener(ModRegistry::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(STATS_BOOKSHELF_TIER_1.get());
            event.accept(STATS_BOOKSHELF_TIER_2.get());
            event.accept(STATS_BOOKSHELF_TIER_3.get());
            event.accept(STATS_BOOKSHELF_TIER_4.get());
        }
    }
}
