package com.chuan.apothicflux.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class FluxSpawnerBlockItem extends BlockItem {
    public FluxSpawnerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.apothic_flux.flux_spawner.auto_output").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.apothic_flux.flux_spawner.input").withStyle(ChatFormatting.GREEN));
        }else {
            tooltip.add(Component.translatable("tooltip.apothic_flux.flux_spawner.click").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
