package com.chuan.apothicenchantingaddition.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SolidifiedFluxExperienceItem extends Item {

    public SolidifiedFluxExperienceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 获取玩家手里的物品栈
        ItemStack itemstack = player.getItemInHand(hand);

        // 逻辑必须只在服务端运行，防止经验值虚假同步
        if (!level.isClientSide) {
            int expToGive = 0;
            int consumeCount = 0;

            if (player.isShiftKeyDown()) {
                //  Shift + 右键：梭哈！消耗玩家手里这一格的所有数量
                consumeCount = itemstack.getCount();
                int minExp = consumeCount * 2;
                int maxExp = consumeCount * 4;
                // 计算随机经验：nextInt(区间差 + 1) + 最小值
                expToGive = level.random.nextInt(maxExp - minExp + 1) + minExp;
            } else {
                //普通右键：稳扎稳打，只消耗 1 个
                consumeCount = 1;
                expToGive = level.random.nextInt(3) + 2; // 产生 2 到 4 之间的随机数
            }

            // 1. 给玩家增加经验
            player.giveExperiencePoints(expToGive);

            // 2. 播放清脆的吸收经验音效，音调稍微随机变化听起来更自然
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);

            // 3. 扣除物品（如果是创造模式则不扣除）
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(consumeCount);
            }
        }

        // 返回成功响应，带上甩手动画
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
