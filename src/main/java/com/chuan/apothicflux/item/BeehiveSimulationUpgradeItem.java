package com.chuan.apothicflux.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Optional upgrade that lets a Flux Spawner accept Productive Bees spawn eggs.
 *
 * <p>The item itself is registered only when Productive Bees is present, so it
 * does not appear in packs without that dependency.</p>
 */
public class BeehiveSimulationUpgradeItem extends Item {

    public BeehiveSimulationUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.apothic_flux.beehive_simulation_upgrade"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
