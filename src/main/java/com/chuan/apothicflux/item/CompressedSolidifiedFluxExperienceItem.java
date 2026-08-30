package com.chuan.apothicflux.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CompressedSolidifiedFluxExperienceItem extends Item {

    public CompressedSolidifiedFluxExperienceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 逻辑必须只在服务端运行
        if (!level.isClientSide) {
            int consumeCount = player.isShiftKeyDown() ? itemstack.getCount() : 1;
            int expToGive = consumeCount * 45;

            // 1. 发放经验
            player.giveExperiencePoints(expToGive);

            // 2. 播放经验音效
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);

            // 3. 扣除对应数量的物品（创造模式免扣）
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(consumeCount);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
