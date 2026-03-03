package com.chuan.apothicenchantingaddition;

import com.chuan.apothicenchantingaddition.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ApothicEnchantingAddition.MOD_ID)
public class CommonEventHandler {

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos placePos = event.getPos().relative(event.getFace());

        if (!level.getBlockState(placePos.below()).isFaceSturdy(level, placePos.below(), Direction.UP)) return;

        level.getRecipeManager().getAllRecipesFor(ModRegistry.DRAWING_TYPE.get()).stream()
                .filter(r -> r.value().tool().test(stack))
                .findFirst()
                .ifPresent(recipe -> {
                    if (!level.isClientSide) {
                        level.setBlock(placePos, ModRegistry.RITUAL_CORE_BLOCK.get().defaultBlockState(), 3);

                        if (!event.getEntity().isCreative()) {
                            if (recipe.value().durabilityCost() > 0) {
                                stack.hurtAndBreak(recipe.value().durabilityCost(), event.getEntity(), LivingEntity.getSlotForHand(event.getHand()));
                            } else if (recipe.value().consumeItem()) {
                                stack.shrink(1);
                            }
                        }
                    }
                    event.setCanceled(true);
                });
    }
}
