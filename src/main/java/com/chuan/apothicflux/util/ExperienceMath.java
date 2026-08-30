package com.chuan.apothicflux.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

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

    public static long xpNeededToGainLevels(Player player, int levels) {
        if (levels <= 0) {
            return 0L;
        }

        int currentLevel = Math.max(0, player.experienceLevel);
        int targetLevel = Math.max(currentLevel, currentLevel + levels);
        long targetXp = xpForLevel(targetLevel);
        long currentXp = Math.max(0, player.totalExperience);
        return Math.max(0L, targetXp - currentXp);
    }

    public static void setPlayerXp(Player player, int xp) {
        int safeXp = Math.max(0, xp);
        player.totalExperience = safeXp;
        player.experienceLevel = levelForXp(safeXp);
        long levelXp = xpForLevel(player.experienceLevel);
        int needed = Math.max(1, player.getXpNeededForNextLevel());
        player.experienceProgress = Mth.clamp((safeXp - levelXp) / (float) needed, 0.0F, 1.0F);
    }
}
