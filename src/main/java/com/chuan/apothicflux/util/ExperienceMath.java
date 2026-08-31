package com.chuan.apothicflux.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

public final class ExperienceMath {
    private ExperienceMath() {
    }

    public static long xpForLevel(int level) {
        if (level <= 0) {
            return 0L;
        }
        if (level <= 16) {
            return (long) level * level + 6L * level;
        }
        if (level <= 31) {
            return (5L * level * level - 81L * level + 720L) / 2L;
        }
        return (9L * level * level - 325L * level + 4440L) / 2L;
    }

    public static int levelForXp(long xp) {
        if (xp <= 0L) {
            return 0;
        }

        if (xp <= 352L) {
            return (int) Math.floor(Math.sqrt(xp + 9.0D) - 3.0D);
        }

        if (xp <= 1507L) {
            return (int) Math.floor((81.0D + Math.sqrt(40.0D * xp - 7839.0D)) / 10.0D);
        }

        double level = Math.floor((325.0D + Math.sqrt(72.0D * xp - 54215.0D)) / 18.0D);
        return level >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) level;
    }

    public static long getRealExperience(Player player) {
        int level = player.experienceLevel;
        long baseXp = xpForLevel(level);
        int needed = Math.max(1, player.getXpNeededForNextLevel());
        long progressXp = (long) Math.floor(player.experienceProgress * needed);
        return baseXp + progressXp;
    }

    public static long xpNeededToGainLevels(Player player, int levels) {
        if (levels <= 0) {
            return 0L;
        }

        int currentLevel = Math.max(0, player.experienceLevel);
        int targetLevel = Math.max(currentLevel, currentLevel + levels);
        long targetXp = xpForLevel(targetLevel);
        long currentXp = getRealExperience(player);
        return Math.max(0L, targetXp - currentXp);
    }

    // 计算“降级 levels 级”需要从玩家身上扣除的经验。
    // 扣完后玩家会停在 (当前等级 - levels)，进度归零。
    public static long xpNeededToLoseLevels(Player player, int levels) {
        if (levels <= 0) {
            return 0L;
        }

        int currentLevel = Math.max(0, player.experienceLevel);
        int targetLevel = Math.max(0, currentLevel - levels);
        long targetXp = xpForLevel(targetLevel);
        long currentXp = getRealExperience(player);
        return Math.max(0L, currentXp - targetXp);
    }

//    public static void setPlayerXp(Player player, int xp) {
//        setPlayerXp(player, (long) xp);
//    }

    public static void setPlayerXp(Player player, long xp) {
        long safeXp = Math.max(0L, xp);
        player.experienceLevel = levelForXp(safeXp);
        long levelXp = xpForLevel(player.experienceLevel);
        int needed = Math.max(1, player.getXpNeededForNextLevel());
        double progress = (safeXp - levelXp) / (double) needed;
        player.experienceProgress = Mth.clamp((float) progress, 0.0F, 1.0F);
        player.totalExperience = (int) Math.min(safeXp, Integer.MAX_VALUE);

        // 强制客户端重同步经验条（对齐 EnderIO 依赖 giveExperiencePoints 触发 lastSentExp=-1 的行为）。
        // 当 totalExperience 已饱和为 int 最大值时，原版只比较 totalExperience != lastSentExp，
        // 等级变化不会被感知，导致经验条不更新；这里主动置位 lastSentExp 触发一次同步。
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setExperienceLevels(player.experienceLevel);
        }
    }
}
